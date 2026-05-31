// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.subsystems;

import org.wpilib.command2.SubsystemBase;
import org.wpilib.hardware.imu.OnboardIMU;
import org.wpilib.hardware.imu.OnboardIMU.MountOrientation;
import org.wpilib.smartdashboard.SmartDashboard;

public class IMUsubsystem extends SubsystemBase {

  OnboardIMU obIMUF = new OnboardIMU(MountOrientation.FLAT);
  OnboardIMU obIMUP = new OnboardIMU(MountOrientation.PORTRAIT);
  OnboardIMU obIMUL = new OnboardIMU(MountOrientation.LANDSCAPE);

  /** Creates a new IMU. */
  public IMUsubsystem() {
    obIMUF.resetYaw();
    obIMUL.resetYaw();
    obIMUP.resetYaw();
  }


  @Override
  public void periodic() {
    // This method will be called once per scheduler run
    SmartDashboard.putNumber("F Angle X",  Math.toDegrees(obIMUF.getAngleX()));
    SmartDashboard.putNumber("F Angle Y", Math.toDegrees(obIMUF.getAngleY()));
    SmartDashboard.putNumber("F Angle Z",   Math.toDegrees(obIMUF.getAngleZ()));
    SmartDashboard.putNumber("F Yaw",  Math.toDegrees(obIMUF.getYawRadians()));

    SmartDashboard.putNumber("L Angle X",  Math.toDegrees(obIMUL.getAngleX()));
    SmartDashboard.putNumber("L Angle Y", Math.toDegrees(obIMUL.getAngleY()));
    SmartDashboard.putNumber("L Angle Z",   Math.toDegrees(obIMUL.getAngleZ()));
    SmartDashboard.putNumber("L Yaw",  Math.toDegrees(obIMUL.getYawRadians()));

    SmartDashboard.putNumber("P Angle X",  Math.toDegrees(obIMUP.getAngleX()));
    SmartDashboard.putNumber("P Angle Y", Math.toDegrees(obIMUP.getAngleY()));
    SmartDashboard.putNumber("P Angle Z",   Math.toDegrees(obIMUP.getAngleZ()));
    SmartDashboard.putNumber("P Yaw",  Math.toDegrees(obIMUP.getYawRadians()));
  }
}
