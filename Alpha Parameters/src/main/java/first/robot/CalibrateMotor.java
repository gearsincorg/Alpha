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
public class CalibrateMotor extends PeriodicOpMode {

  static final double VOLTAGE_STEP = 0.01;

  final Robot robot;
  Gamepad gamepad1;
  ExpansionHubMotor frontLeftDrive;

  double  commandedVoltage  = 0;
  double  commandedVelocity = 0;
  double  measuredVelocity  = 0;
  double  kS = 0;
  double  kV = 0;
  
  public CalibrateMotor(Robot robot, DefaultUserControls userControls) {
    this.robot = robot;
    gamepad1 = userControls.getGamepad(0);

    // setup actuators
    frontLeftDrive = robot.motor2;
    frontLeftDrive.setDistancePerCount(1.0);
  }

  @Override
  public void start() {
    commandedVoltage  = 0;
    measuredVelocity  = 0;
    kS = 0;
    kV = 0;
  }

  @Override
  public void periodic() {

    // Ramp up voltage to measure velocity response.
    if (robot.isEnabled()) {

      measuredVelocity = frontLeftDrive.getEncoderVelocity();

      if ((kS == 0) && (measuredVelocity > 0)) {
        kS = commandedVoltage;
      }

      if (commandedVoltage >= 10) {
        kV = (10.0 - kS) / (measuredVelocity);
      } else {
        commandedVoltage += VOLTAGE_STEP;
      }

      SmartDashboard.putNumber("voltage",  commandedVoltage);
      SmartDashboard.putNumber("velocity", measuredVelocity);
      SmartDashboard.putNumber("kS", kS);
      SmartDashboard.putNumber("kV", kV);

      frontLeftDrive.setVoltage(Units.Volt.of(commandedVoltage));  
      
    } else {
      frontLeftDrive.setVoltage(Units.Volt.of(0));  
    }
  }
}

// stashed code
// frontLeftDrive.setThrottle(-gamepad1.getLeftY());   // works OK
// frontLeftDrive.setVelocitySetpoint(-gamepad1.getLeftY());
// frontLeftDrive.getVelocityConstants().setPID(0, 0, 0).setFF(0.5, 0.004);
