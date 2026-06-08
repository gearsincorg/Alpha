// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.util.Units;

/**
 * The Constants class provides a convenient place for teams to hold robot-wide numerical or boolean
 * constants. This class should not be used for any other purpose. All constants should be declared
 * globally (i.e. public static). Do not put anything functional in this class.
 *
 * <p>It is advised to statically import this class (or one of its inner classes) wherever the
 * constants are needed, to reduce verbosity.
 */
public final class Constants {
  public static class OperatorConstants {
    public static final int kDriverControllerPort = 0;
  }

  /** Constants for the AprilTag vision pipeline (OpenCV + WPILib AprilTag). */
  public static class VisionConstants {
    /** USB camera device index passed to CameraServer.startAutomaticCapture(). */
    public static final int kCameraDevice = 0;

    /** Capture resolution. Must match the resolution your calibration was performed at. */
    public static final int kCameraWidth  = 640;
    public static final int kCameraHeight = 480;
    public static final int kCameraFps = 30;

    /** AprilTag family to detect (the 2025/2026 field uses 36h11). */
    public static final String kTagFamily = "tag36h11";

    /** Physical size of the printed tag's black square, in meters. */
    public static final double kTagSizeMeters = 0.098; // 4 in

    // --- Fallback camera intrinsics (Logitech C310 @ 640x480) ---
    // fx, fy = focal lengths in pixels; cx, cy = optical center in pixels.
    // Normally the intrinsics are looked up at runtime from calibrations/webcams.xml by the
    // camera's USB vid/pid and capture resolution; these are only used if that lookup misses.
    public static final double kFx = 822;
    public static final double kFy = 822;
    public static final double kCx = 330;
    public static final double kCy = 248;

    /**
     * Fixed transform from the robot origin to the camera lens. Used to express a detected tag's
     * pose in the robot frame (robotToTag = kRobotToCamera + cameraToTag).
     *
     * <p>WPILib robot frame: +X forward, +Y left, +Z up; rotation is (roll, pitch, yaw) in radians.
     * TODO(Phil): measure these for your robot's actual camera mount. 
     */
    public static final Transform3d kRobotToCamera =
        new Transform3d(
            Units.inchesToMeters(0.0), // X: forward of robot center
            Units.inchesToMeters(0.0), // Y: left of robot center
            Units.inchesToMeters(0.0), // Z: above the floor
            new Rotation3d(
                0.0, // roll
                0.0, // pitch (positive tilts the camera down)
                0.0)); // yaw (positive points the camera left)
  }
}
