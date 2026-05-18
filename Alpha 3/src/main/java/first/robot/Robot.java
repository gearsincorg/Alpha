package first.robot;

import org.wpilib.drive.MecanumDrive;
import org.wpilib.driverstation.DefaultUserControls;
import org.wpilib.framework.TimedRobot;
import org.wpilib.hardware.expansionhub.ExpansionHubMotor;
import org.wpilib.hardware.imu.OnboardIMU;
import org.wpilib.smartdashboard.SmartDashboard;

public class Robot extends TimedRobot {
  public final ExpansionHubMotor rearRight = new ExpansionHubMotor(0, 0);
  public final ExpansionHubMotor rearLeft = new ExpansionHubMotor(0, 1);
  public final ExpansionHubMotor frontRight = new ExpansionHubMotor(0, 2);
  public final ExpansionHubMotor frontLeft = new ExpansionHubMotor(0, 3);
  private static final OnboardIMU.MountOrientation kIMUMountOrientation =
      OnboardIMU.MountOrientation.LANDSCAPE;

  private final MecanumDrive robotDrive;
  private final OnboardIMU imu = new OnboardIMU(kIMUMountOrientation);
  private final DefaultUserControls controls = new DefaultUserControls();

  public Robot() {
    frontLeft.setReversed(true);
    rearLeft.setReversed(true);

    imu.resetYaw();

    robotDrive =
        new MecanumDrive(
            frontLeft::setThrottle,
            rearLeft::setThrottle,
            frontRight::setThrottle,
            rearRight::setThrottle);

  }

  @Override
  public void teleopPeriodic() {
    robotDrive.driveCartesian(
        -controls.getGamepad(0).getLeftY(), 
        controls.getGamepad(0).getLeftX(), 
        controls.getGamepad(0).getRightX(), 
        imu.getRotation2d());
    SmartDashboard.putNumber("Phil",-controls.getGamepad(0).getLeftY() );
  }
}