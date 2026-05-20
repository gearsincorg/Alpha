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

  // Set this value to be appropriate for your motor/gearing.
  static final double DISTANCE_PER_COUNT = 0.01; 

  static final double MAX_VELOCITY = 2600 / DISTANCE_PER_COUNT;  // Gobilda
  static final double kS = 0.5   ;
  static final double kV = 0.004 / DISTANCE_PER_COUNT;
  static final double kP = 0.002 / DISTANCE_PER_COUNT;
  static final double kI = 0.02  / DISTANCE_PER_COUNT;  
  static final double kD = 0.0   / DISTANCE_PER_COUNT;

  final Robot robot;
  Gamepad gamepad1;
  ExpansionHubMotor frontLeftDrive;
  double commandedVelocity = 0;
  double measuredVelocity  = 0;
  
  public PIDtesting(Robot robot, DefaultUserControls userControls) {
    this.robot = robot;
    gamepad1 = userControls.getGamepad(0);

    // setup actuators
    frontLeftDrive = robot.motor2;
    frontLeftDrive.setDistancePerCount(DISTANCE_PER_COUNT);
    frontLeftDrive.getVelocityConstants().setPID(kP, kI, kD).setFF(kS, kV, 0);
    frontLeftDrive.getPositionConstants().setS(kS);
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
      SmartDashboard.putNumber("output",   frontLeftDrive.getCurrent().baseUnitMagnitude());

      // step the requested velocity
      if (gamepad1.getNorthFaceButtonPressed()) {
        commandedVelocity += 500 * DISTANCE_PER_COUNT;
      } else if (gamepad1.getSouthFaceButtonPressed()) {
        commandedVelocity -= 500 * DISTANCE_PER_COUNT;
      }

      // set the target velocity
      frontLeftDrive.setVelocitySetpoint(commandedVelocity);
    } else {
      frontLeftDrive.setVoltage(Units.Volt.of(0));  
    }
  }
}
