// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.driverstation.DefaultUserControls;
import org.wpilib.driverstation.Gamepad;
import org.wpilib.epilogue.Logged;
import org.wpilib.hardware.expansionhub.ExpansionHubMotor;
import org.wpilib.opmode.PeriodicOpMode;
import org.wpilib.opmode.Teleop;
import org.wpilib.smartdashboard.SmartDashboard;
import org.wpilib.units.Units;

@Teleop
@Logged
public class PIDtesting extends PeriodicOpMode {

  static final double MAX_VELOCITY = 2500;
  static final double kS = 0.5;
  static final double kV = 0.004;
  static final double kP = 0.006;
  static final double kI = 0.1;  //  <<<   doesn't seem to have any effect
  static final double kD = 0.0;

  final Robot robot;
  Gamepad gamepad1;
  ExpansionHubMotor frontLeftDrive;
  double commandedVelocity = 0;
  double measuredVelocity  = 0;
  
  public PIDtesting(Robot robot, DefaultUserControls userControls) {
    this.robot = robot;

    // setup actuators
    frontLeftDrive = robot.motor0;
    frontLeftDrive.setDistancePerCount(1);
    frontLeftDrive.getVelocityConstants().setPID(kP, kI, kD).setFF(kS, kV, 0);
  }

  @Override
  public void start() {
    commandedVelocity = 0;
    measuredVelocity  = 0;
  }

  @Override
  public void periodic() {

    // Step target velocity up and down with gamepad Y and A
    if (robot.isEnabled()) {

      measuredVelocity = frontLeftDrive.getEncoderVelocity();

      SmartDashboard.putNumber("setpoint", commandedVelocity);
      SmartDashboard.putNumber("velocity", measuredVelocity);

      // commandedVelocity =  -gamepad1.getLeftY() * MAX_VELOCITY;
      if (gamepad1.getNorthFaceButtonPressed()) {
        commandedVelocity += 500;
      } else if (gamepad1.getSouthFaceButtonPressed()) {
        commandedVelocity -= 500;
      }

      frontLeftDrive.setVelocitySetpoint(commandedVelocity);
    } else {
      frontLeftDrive.setVoltage(Units.Volt.of(0));  
    }
  }
}
