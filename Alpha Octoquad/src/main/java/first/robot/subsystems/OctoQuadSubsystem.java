package first.robot.subsystems;

import org.wpilib.command2.SubsystemBase;
import org.wpilib.math.geometry.Pose2d;
import org.wpilib.math.geometry.Rotation2d;
import org.wpilib.smartdashboard.SmartDashboard;

import first.robot.Constants.OctoQuadConstants;
import first.robot.subsystems.OctoQuad.LocalizerStatus;

/**
 * Scheduler-facing wrapper around the {@link OctoQuad} I2C driver. Owns the device lifecycle and
 * caches the most recent localization snapshot for the rest of the robot to consume.
 *
 * <p>Usage: call {@link #initialize()} once (e.g. from {@code RobotContainer}) to configure the
 * localizer and start IMU calibration. {@link #periodic()} then bulk-reads the latest data every
 * robot loop; consumers read it via {@link #getPose()}, {@link #getLatestData()}, and
 * {@link #isLocalizerReady()}.
 */
public class OctoQuadSubsystem extends SubsystemBase {

  // Data structure which will store the localizer data read from the OctoQuad
  OctoQuad.LocalizerDataBlock m_latest = new OctoQuad.LocalizerDataBlock();
  OctoQuadImpl  m_octoQuad = new OctoQuadImpl(OctoQuadConstants.kI2cPort);

  /**
   * Verify the device is present, push localizer configuration, then reset the localizer pose to
   * (0,0,0) and start IMU calibration. The pose is not valid until calibration finishes; poll
   * {@link #getStatus()} (or {@link #isLocalizerReady()}) for {@link LocalizerStatus#RUNNING}.
   *
   * @return true if the device responded with the expected chip ID and configuration was sent
   */
  public boolean initialize() {
    SmartDashboard.putString("OctoQuad", m_octoQuad.getFirmwareVersionString());

    // Configure a range of parameters for the absolute localizer
    // --> Read the quick start guide for an explanation of these!!
    // IMPORTANT: these parameter changes will not take effect until the localizer is reset!
    m_octoQuad.setSingleEncoderDirection(OctoQuadConstants.kLocalizerPortX, OctoQuadConstants.kLocalizerPortXDir);
    m_octoQuad.setSingleEncoderDirection(OctoQuadConstants.kLocalizerPortY, OctoQuadConstants.kLocalizerPortYDir);
    m_octoQuad.setLocalizerPortX(OctoQuadConstants.kLocalizerPortX);
    m_octoQuad.setLocalizerPortY(OctoQuadConstants.kLocalizerPortY);
    m_octoQuad.setLocalizerCountsPerMM_X(OctoQuadConstants.kTicksPerMM_X);
    m_octoQuad.setLocalizerCountsPerMM_Y(OctoQuadConstants.kTicksPerMM_X);
    m_octoQuad.setLocalizerTcpOffsetMM_X(OctoQuadConstants.kTicksPerMM_X);
    m_octoQuad.setLocalizerTcpOffsetMM_Y(OctoQuadConstants.kTcpOffsetMM_Y);
    m_octoQuad.setLocalizerImuHeadingScalar(OctoQuadConstants.kTcpOffsetMM_Y);
    m_octoQuad.setLocalizerVelocityIntervalMS(OctoQuadConstants.kVelocityIntervalMs);
    m_octoQuad.setI2cRecoveryMode(OctoQuad.I2cRecoveryMode.MODE_1_PERIPH_RST_ON_FRAME_ERR);

    // Resetting the localizer will apply the parameters configured above.
    // This function will NOT block until calibration of the IMU is complete -
    // for that you need to look at the status returned by getLocalizerStatus()
    
    // Reset pose to origin and (re)calibrate the IMU — robot MUST be stationary for this.
    m_octoQuad.resetLocalizerAndCalibrateIMU();
    return true;
  }

  /** Read the most recent localization data from the device into {@link #m_latest}. */
  @Override
  public void periodic() {

    // Read updated data from the OctoQuad into the 'localizer' data structure
    m_octoQuad.readLocalizerData(m_latest);
    publish();
  }

  /** @return the latest localizer pose (X/Y in meters, heading as a {@code Rotation2d}). */
  public Pose2d getPose() {
    return new Pose2d(m_latest.posX_mm, m_latest.posY_mm, new Rotation2d(m_latest.heading_rad));
  }

  /** @return the full latest snapshot, including validity and velocities. */
  public OctoQuad.LocalizerDataBlock getLatestData() {
    return m_latest;
  }

  /** @return the latest localizer status. */
  public LocalizerStatus getStatus() {
    return m_latest.localizerStatus;
  }

  /** @return true once IMU calibration is done and the latest read passed its CRC. */
  public boolean isLocalizerReady() {
    return m_latest.isDataValid();
  }

  /**
   * "Teleport" the localizer to a new pose, e.g. to seed odometry from a vision estimate.
   *
   * @param pose new pose; X/Y in meters, heading as a {@code Rotation2d}
   */
  public void setPose(Pose2d pose) {
    m_octoQuad.setLocalizerPose((int)(pose.getX()), (int)(pose.getY()), (float)pose.getRotation().getRadians());
  }

  /**
   * reset the IMU heading.
   *
   * @param pose new pose; X/Y in meters, heading as a {@code Rotation2d}
   */
  public void resetHeading() {
    m_octoQuad.setLocalizerHeading(0);
  }

  private void publish() {
    SmartDashboard.putString("OctoQuad/Status", m_latest.localizerStatus.toString());
    SmartDashboard.putBoolean("OctoQuad/Valid", m_latest.isDataValid());
    SmartDashboard.putNumber("OctoQuad/X", m_latest.posX_mm);
    SmartDashboard.putNumber("OctoQuad/Y", m_latest.posY_mm);
    SmartDashboard.putNumber("OctoQuad/HeadingDeg", Math.toDegrees(m_latest.heading_rad));
  }
}
