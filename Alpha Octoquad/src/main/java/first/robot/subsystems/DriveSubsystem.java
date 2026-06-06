package first.robot.subsystems;

import com.revrobotics.spark.A301;
import org.wpilib.command2.SubsystemBase;
import org.wpilib.hardware.imu.OnboardIMU;
import org.wpilib.hardware.imu.OnboardIMU.MountOrientation;
import org.wpilib.math.controller.PIDController;
import org.wpilib.smartdashboard.SmartDashboard;

import first.robot.Constants;
import first.robot.Constants.DriveConstants;

/**
 * Three-wheel (kiwi) holonomic drive.
 *
 * Wheel layout (viewed from above):
 *
 *   [Front Left] 150°     [Front Right] 30°
 *
 *              [Rear] 270°
 *
 * Kinematics: v_wheel = -vx * sin(θ) + vy * cos(θ) + ω
 */

public class DriveSubsystem extends SubsystemBase {
  private final A301 m_rearMotor       = new A301(DriveConstants.kRearMotorCanId);
  private final A301 m_frontRightMotor = new A301(DriveConstants.kFrontRightMotorCanId);
  private final A301 m_frontLeftMotor  = new A301(DriveConstants.kFrontLeftMotorCanId);

  private static final double kRearAngleRad       = 3.0 * Math.PI / 2; // 270°
  private static final double kFrontRightAngleRad = Math.PI / 6;       // 30°
  private static final double kFrontLeftAngleRad  = 5.0 * Math.PI / 6; // 150°

  // Onboard IMU and heading-hold controller for automatic drift correction.
  private final OnboardIMU m_imu = new OnboardIMU(MountOrientation.FLAT);
  private final PIDController m_headingController =
      new PIDController(DriveConstants.kHeadingP, DriveConstants.kHeadingI, DriveConstants.kHeadingD);

  // True once a hold heading has been latched (driver released rotate AND robot stopped turning).
  private boolean m_headingLocked = false;
  private double m_headingSetpoint = 0.0;

  public DriveSubsystem() {
    m_rearMotor.setInverted(DriveConstants.kRearMotorInverted);
    m_frontRightMotor.setInverted(DriveConstants.kFrontRightMotorInverted);
    m_frontLeftMotor.setInverted(DriveConstants.kFrontLeftMotorInverted);

    // Heading is circular: let the controller take the shortest path across ±π.
    m_headingController.enableContinuousInput(-Math.PI, Math.PI);
  }

  /**
   * Drive using kiwi (three-wheel holonomic) kinematics.
   *
   * @param xSpeed Lateral speed [-1, 1], positive = right
   * @param ySpeed Forward speed [-1, 1], positive = forward
   * @param rot    Rotation speed [-1, 1], positive = counterclockwise
   */
  public void drive(double xSpeed, double ySpeed, double rot) {
    double[] speeds = {
      wheelSpeed(xSpeed, ySpeed, rot, kRearAngleRad),
      wheelSpeed(xSpeed, ySpeed, rot, kFrontRightAngleRad),
      wheelSpeed(xSpeed, ySpeed, rot, kFrontLeftAngleRad),
    };

    desaturate(speeds);

    m_rearMotor.setVelocity(speeds[0] * Constants.DriveConstants.kDriveMaxRPM);
    m_frontRightMotor.setVelocity(speeds[1] * Constants.DriveConstants.kDriveMaxRPM);
    m_frontLeftMotor.setVelocity(speeds[2] * Constants.DriveConstants.kDriveMaxRPM);

    SmartDashboard.putNumber("DriveA", speeds[0]);
    SmartDashboard.putNumber("DriveB", speeds[1]);
    SmartDashboard.putNumber("DriveC", speeds[2]);
  }

  /**
   * Drive with automatic heading hold (drift correction).
   *
   * <p>While the driver commands rotation, {@code rotInput} passes straight through. When the
   * driver releases the rotate stick, the loop latches a hold heading immediately and runs a PID
   * loop on IMU yaw to actively arrest any residual spin and hold the heading until the driver
   * rotates again. The latched setpoint is projected ahead by the current yaw rate
   * ({@link DriveConstants#kHeadingLatchLeadTime}) so the robot decelerates into the held heading
   * rather than overshooting it.
   *
   * @param xSpeed   Lateral speed [-1, 1], positive = right
   * @param ySpeed   Forward speed [-1, 1], positive = forward
   * @param rotInput Driver rotation command [-1, 1] (deadbanded), positive = counterclockwise
   */
  public void driveWithHeadingHold(double xSpeed, double ySpeed, double rotInput) {
    double rot;
    if (rotInput != 0.0) {
      // Driver is actively rotating — pass through and drop any held heading.
      m_headingLocked = false;
      rot = rotInput;
    } else {
      if (!m_headingLocked) {
        // Stick just released. Latch a hold heading immediately and let the PID actively
        // kill the residual spin — do NOT wait for the robot to coast to a stop, because
        // while it keeps translating the front wheel scrubs and it never stops yawing on
        // its own. Project the setpoint ahead by the current yaw rate so the robot
        // decelerates into the held heading instead of overshooting it.
        m_headingSetpoint =
            m_imu.getYawRadians() + m_imu.getGyroRateZ() * DriveConstants.kHeadingLatchLeadTime;
        m_headingController.reset();
        m_headingLocked = true;
      }
      // Heading latched — actively correct back to the setpoint.
      rot = headingCorrection();
    }

    if (DriveConstants.USE_FIELD_CENTRIC) {
      // Rotate the driver's field-relative translation command into the robot frame by -yaw.
      // Frame: x = right, y = forward, yaw is CCW-positive, so (x, y, z-up) is right-handed.
      double yaw = m_imu.getYawRadians();
      double cos = Math.cos(yaw);
      double sin = Math.sin(yaw);
      double fieldX = xSpeed;
      double fieldY = ySpeed;
      xSpeed =  fieldX * cos + fieldY * sin;
      ySpeed = -fieldX * sin + fieldY * cos;
    }

    drive(xSpeed, ySpeed, rot);
  }

  /**
   * Zero the IMU yaw and re-sync the heading-hold controller so it holds the new (zero) heading
   * rather than chasing the now-stale pre-reset setpoint. Since the heading-hold setpoint and the
   * latched yaw both lived in the old IMU frame, resetting the IMU without this would make the PID
   * snap the robot back to the old reference. Setting the setpoint to 0 and clearing the
   * controller's integral/derivative state keeps the robot pointed where it is.
   *
   * <p>This also redefines field-forward (for {@code USE_FIELD_CENTRIC} drive) to the robot's
   * current heading.
   */
  public void resetHeading() {
    m_imu.resetYaw();
    m_headingSetpoint = 0.0;
    m_headingController.reset();
  }

  /** PID rotation command (clamped) to drive IMU yaw toward the latched heading setpoint. */
  private double headingCorrection() {
    double output = m_headingController.calculate(m_imu.getYawRadians(), m_headingSetpoint);
    return Math.clamp(output, -DriveConstants.kHeadingMaxOutput, DriveConstants.kHeadingMaxOutput);
  }

  public void stopMotors() {
    m_rearMotor.setThrottle(0);
    m_frontRightMotor.setThrottle(0);
    m_frontLeftMotor.setThrottle(0);
  }

  @Override
  public void periodic() {
    SmartDashboard.putBoolean("HeadingLocked", m_headingLocked);
    SmartDashboard.putNumber("HeadingSetpointDeg", Math.toDegrees(m_headingSetpoint));
    SmartDashboard.putNumber("HeadingYawDeg", Math.toDegrees(m_imu.getYawRadians()));
    SmartDashboard.putNumber("HeadingYawRate", Math.toDegrees(m_imu.getGyroRateZ()));
  }

  private static double wheelSpeed(double vx, double vy, double omega, double angleRad) {
    return -vx * Math.sin(angleRad) + vy * Math.cos(angleRad) + omega;
  }

  private static void desaturate(double[] speeds) {
    double max = 0;
    for (double s : speeds) max = Math.max(max, Math.abs(s));
    if (max > 1.0) for (int i = 0; i < speeds.length; i++) speeds[i] /= max;
  }
}
