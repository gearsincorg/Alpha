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

  final Robot robot;
  Gamepad gamepad1;
  ExpansionHubMotor frontLeftDrive;

  double  commandedVelocity = 0;
  double  measuredVelocity  = 0;
  final double  kS = 0.5;
  final double  kV = 0.004;
  
  public PIDtesting(Robot robot, DefaultUserControls userControls) {
    this.robot = robot;
    gamepad1 = userControls.getGamepad(0);

    // setup actuators
    frontLeftDrive = robot.motor0;
    frontLeftDrive.setDistancePerCount(1);
    frontLeftDrive.getVelocityConstants().setPID(.007, 0, 0).setFF(kS, kV, 0);
  }

  @Override
  public void start() {
    commandedVelocity = 0;
    measuredVelocity  = 0;
  }

  @Override
  public void periodic() {

    // Ramp up voltage to measure velocity response.
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

// stashed code
// frontLeftDrive.setThrottle(-gamepad1.getLeftY());   // works OK
// frontLeftDrive.setVelocitySetpoint(-gamepad1.getLeftY());
// frontLeftDrive.getVelocityConstants().setPID(0, 0, 0).setFF(0.5, 11.5 / 2500.0, 0);
