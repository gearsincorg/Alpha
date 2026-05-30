// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.subsystems;

import org.wpilib.command2.SubsystemBase;
import org.wpilib.hardware.imu.OnboardIMU;
import org.wpilib.hardware.imu.OnboardIMU.MountOrientation;
import org.wpilib.smartdashboard.SmartDashboard;

public class IMUsubsystem extends SubsystemBase {

  OnboardIMU obIMU = new OnboardIMU(MountOrientation.FLAT);

  /** Creates a new IMU. */
  public IMUsubsystem() {
    obIMU.resetYaw();
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
    SmartDashboard.putNumber("pitch", Math.toDegrees(obIMU.getAngleY()));
    SmartDashboard.putNumber("roll",  Math.toDegrees(obIMU.getAngleX()));
    SmartDashboard.putNumber("yaw",   Math.toDegrees(obIMU.getAngleZ()));
    SmartDashboard.putNumber("yaw2",  Math.toDegrees(obIMU.getYawRadians()));
  }
}
