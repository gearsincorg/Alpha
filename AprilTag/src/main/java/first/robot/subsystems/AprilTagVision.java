// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.subsystems;

import first.robot.Constants.VisionConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.wpilib.command2.Command;
import org.wpilib.command2.Commands;
import org.wpilib.command2.SubsystemBase;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.networktables.NetworkTable;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.networktables.StructArrayPublisher;
import org.wpilib.smartdashboard.SmartDashboard;
import org.wpilib.vision.apriltag.AprilTagDetection;
import org.wpilib.vision.apriltag.AprilTagDetector;
import org.wpilib.vision.apriltag.AprilTagPoseEstimator;
import org.wpilib.vision.camera.CvSink;
import org.wpilib.vision.camera.CvSource;
import org.wpilib.vision.camera.UsbCamera;
import org.wpilib.vision.camera.UsbCameraInfo;
import org.wpilib.vision.stream.CameraServer;

/**
 * AprilTag detection and pose estimation built directly on OpenCV and the WPILib AprilTag library
 *
 * <p>Heavy image processing runs on its own daemon thread so it never blocks the 20 ms robot loop.
 * Each frame is grabbed from the USB camera, converted to grayscale, run through {@link
 * AprilTagDetector}, and each detection is fed to {@link AprilTagPoseEstimator} to recover the
 * camera-to-tag {@link Transform3d}. That is composed with the fixed robot-to-camera transform to
 * also express each tag in the robot frame. Results are published to NetworkTables and cached for
 * callers via {@link #getLatestObservations()}.
 *
 * <p>This project has no field map, so tags are reported relative to the camera and the robot only;
 * there is no field localization.
 */
public class AprilTagVision extends SubsystemBase {

  @Override
  public void periodic() {
    printDetections(); 
  }

  /**
   * A single tag seen in one frame.
   *
   * @param id the tag's id
   * @param cameraToTag transform from the camera to the tag
   * @param robotToTag transform from the robot origin to the tag (the tag's pose in the robot frame)
   * @param decisionMargin detector confidence (higher is better; reject low values)
   */
  public record TagObservation(
      int id, Transform3d cameraToTag, Transform3d robotToTag, double decisionMargin) {}

  // Drawing colors (OpenCV uses BGR order).
  private static final Scalar kOutlineColor = new Scalar(0, 255, 0);
  private static final Scalar kTextColor = new Scalar(255, 0, 255);

  private final Thread visionThread;

  // NetworkTables publishers (written from the vision thread).
  private final NetworkTable table = NetworkTableInstance.getDefault().getTable("AprilTagVision");
  private final StructArrayPublisher<Transform3d> cameraToTagPublisher =
      table.getStructArrayTopic("cameraToTag", Transform3d.struct).publish();
  private final StructArrayPublisher<Transform3d> robotToTagPublisher =
      table.getStructArrayTopic("robotToTag", Transform3d.struct).publish();

  // Latest results, swapped atomically by the vision thread and read by the main thread.
  private volatile List<TagObservation> latestObservations = List.of();
  private volatile double lastFrameTimestampSeconds;

  /** Creates the subsystem and starts the background vision thread. */
  public AprilTagVision() {
    visionThread = new Thread(this::visionLoop, "AprilTagVision");
    visionThread.setDaemon(true);
    visionThread.start();
  }

  /** @return the tags detected in the most recently processed frame (never null). */
  public List<TagObservation> getLatestObservations() {
    return latestObservations;
  }

  /**
   * A command that prints the current detections to the console once each time it is scheduled.
   * Handy for sanity-checking the pipeline on real hardware. Does not require the subsystem, so it
   * can run alongside anything else.
   *
   * @return the command
   */
  public Command logDetectionsCommand() {
    return Commands.runOnce(this::printDetections);
  }

  /**
   * Publishes the latest tag observations (id, robot-relative pose, confidence) to the
   * SmartDashboard as a single multiline string, one tag per line, under the key {@code
   * AprilTagVision/Detections}. This makes the detections visible on the driver station without
   * access to the console.
   */
  public void printDetections() {
    List<TagObservation> observations = latestObservations;
    if (observations.isEmpty()) {
      SmartDashboard.putString("AprilTagVision/Detections", "No tags in view.");
      return;
    }
    StringBuilder report = new StringBuilder();
    for (TagObservation obs : observations) {
      Transform3d t = obs.robotToTag();
      if (report.length() > 0) {
        report.append('\n');
      }
      report.append(
          String.format(
              "id=%d  robot-frame x=%.3f y=%.3f z=%.3f m  dist=%.3f m  margin=%.0f",
              obs.id(),
              t.getX(),
              t.getY(),
              t.getZ(),
              t.getTranslation().getNorm(),
              obs.decisionMargin()));
    }
    SmartDashboard.putString("AprilTagVision/Detections", report.toString());
  }

  /** @return CameraServer timestamp (seconds) of the last successfully processed frame. */
  public double getLastFrameTimestampSeconds() {
    return lastFrameTimestampSeconds;
  }

  /** The image-processing loop. Runs on its own thread until the JVM exits. */
  private void visionLoop() {
    UsbCamera camera = CameraServer.startAutomaticCapture(VisionConstants.kCameraDevice);
    camera.setResolution(VisionConstants.kCameraWidth, VisionConstants.kCameraHeight);
    camera.setFPS(VisionConstants.kCameraFps);

    CvSink sink = CameraServer.getVideo();
    CvSource outputStream =
        CameraServer.putVideo(
            "AprilTagDetections", VisionConstants.kCameraWidth, VisionConstants.kCameraHeight);

    AprilTagDetector detector = new AprilTagDetector();
    detector.addFamily(VisionConstants.kTagFamily);
    CameraCalibration calibration = resolveCalibration(camera);
    AprilTagPoseEstimator estimator =
        new AprilTagPoseEstimator(
            new AprilTagPoseEstimator.Config(
                VisionConstants.kTagSizeMeters,
                calibration.fx(),
                calibration.fy(),
                calibration.cx(),
                calibration.cy()));

    Mat frame = new Mat();
    Mat gray = new Mat();

    try {
      while (!Thread.interrupted()) {
        long frameTime = sink.grabFrame(frame);
        if (frameTime == 0) {
          // Timeout / camera error: report it on the output stream and keep going.
          outputStream.notifyError(sink.getError());
          continue;
        }

        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY);
        AprilTagDetection[] detections = detector.detect(gray);

        List<TagObservation> observations = new ArrayList<>(detections.length);
        for (AprilTagDetection detection : detections) {
          Transform3d cameraToTag = estimator.estimate(detection);
          // robot -> camera -> tag gives the tag's pose in the robot frame.
          Transform3d robotToTag = VisionConstants.kRobotToCamera.plus(cameraToTag);
          observations.add(
              new TagObservation(
                  detection.getId(), cameraToTag, robotToTag, detection.getDecisionMargin()));
          drawDetection(frame, detection);
        }

        publish(observations);
        latestObservations = observations;
        lastFrameTimestampSeconds = frameTime / 1e6; // CameraServer time is microseconds
        outputStream.putFrame(frame);
      }
    } finally {
      detector.close();
    }
  }

  /**
   * Picks the camera intrinsics to use, looking the running camera up in {@code webcams.xml} by its
   * USB vid/pid and the configured capture resolution. Falls back to the {@link VisionConstants}
   * defaults if the camera is unknown or the file can't be read, so the pipeline never hard-fails.
   *
   * @param camera the camera opened by CameraServer
   * @return the intrinsics to feed the pose estimator
   */
  private CameraCalibration resolveCalibration(UsbCamera camera) {
    CameraCalibration fallback =
        new CameraCalibration(
            VisionConstants.kCameraWidth,
            VisionConstants.kCameraHeight,
            VisionConstants.kFx,
            VisionConstants.kFy,
            VisionConstants.kCx,
            VisionConstants.kCy);

    UsbCameraInfo info = camera.getInfo();
    try {
      Optional<CameraCalibration> match =
          WebcamCalibrations.loadDefault()
              .find(
                  info.vendorId,
                  info.productId,
                  VisionConstants.kCameraWidth,
                  VisionConstants.kCameraHeight);
      if (match.isPresent()) {
        SmartDashboard.putString(
            "AprilTagVision/Calibration",
            String.format(
                "Calibration for vid=0x%04X pid=0x%04X @ %dx%d:"
                    + " fx=%.3f fy=%.3f cx=%.3f cy=%.3f",
                info.vendorId,
                info.productId,
                VisionConstants.kCameraWidth,
                VisionConstants.kCameraHeight,
                match.get().fx(),
                match.get().fy(),
                match.get().cx(),
                match.get().cy()));
        return match.get();
      }
      SmartDashboard.putString(
          "AprilTagVision/Calibration",
          String.format(
              "No calibration for vid=0x%04X pid=0x%04X @ %dx%d; using defaults.",
              info.vendorId,
              info.productId,
              VisionConstants.kCameraWidth,
              VisionConstants.kCameraHeight));
    } catch (IOException ex) {
      SmartDashboard.putString(
          "AprilTagVision/Calibration",
          "Could not load webcams.xml; using defaults: " + ex.getMessage());
    }
    return fallback;
  }

  /** Pushes the frame's observations to NetworkTables for dashboards / AdvantageScope. */
  private void publish(List<TagObservation> observations) {
    Transform3d[] cameraToTags = new Transform3d[observations.size()];
    Transform3d[] robotToTags = new Transform3d[observations.size()];
    long[] ids = new long[observations.size()];
    for (int i = 0; i < observations.size(); i++) {
      TagObservation obs = observations.get(i);
      cameraToTags[i] = obs.cameraToTag();
      robotToTags[i] = obs.robotToTag();
      ids[i] = obs.id();
    }
    cameraToTagPublisher.set(cameraToTags);
    robotToTagPublisher.set(robotToTags);
    table.getEntry("detectedIds").setIntegerArray(ids);
    table.getEntry("tagCount").setInteger(observations.size());
  }

  /** Outlines a detected tag and labels it with its id on the output frame. */
  private static void drawDetection(Mat frame, AprilTagDetection detection) {
    for (int i = 0; i < 4; i++) {
      int next = (i + 1) % 4;
      Point p1 = new Point(detection.getCornerX(i), detection.getCornerY(i));
      Point p2 = new Point(detection.getCornerX(next), detection.getCornerY(next));
      Imgproc.line(frame, p1, p2, kOutlineColor, 2);
    }
    Imgproc.putText(
        frame,
        Integer.toString(detection.getId()),
        new Point(detection.getCenterX(), detection.getCenterY()),
        Imgproc.FONT_HERSHEY_SIMPLEX,
        1.0,
        kTextColor,
        2);
  }
}
