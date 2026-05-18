// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.driverstation.DefaultUserControls;
import org.wpilib.driverstation.Gamepad;
import org.wpilib.hardware.expansionhub.ExpansionHubMotor;
import org.wpilib.hardware.expansionhub.ExpansionHubServo;
import org.wpilib.opmode.PeriodicOpMode;
import org.wpilib.opmode.Teleop;
import org.wpilib.units.Units;
import org.wpilib.units.VoltageUnit;
import org.wpilib.units.measure.Voltage;

@Teleop
public class DefaultTeleMode extends PeriodicOpMode {
  private final Robot robot;
  private Gamepad gamepad1;
  private ExpansionHubMotor frontLeftDrive;
  private ExpansionHubServo leftClaw;

  public DefaultTeleMode(Robot robot, DefaultUserControls userControls) {
    this.robot = robot;
    gamepad1 = userControls.getGamepad(0);
  }

  @Override
  public void start() {
    // setup actuators
    frontLeftDrive = robot.motor0;
    frontLeftDrive.setReversed(true);
    frontLeftDrive.setDistancePerCount(0.01);
    frontLeftDrive.getVelocityConstants().setPID(.1, 0, 0).setFF(0, 0, 0);


    leftClaw = robot.servo0;
    leftClaw.setPosition(0);
  }

  @Override
  public void periodic() {

    // frontLeftDrive.setThrottle(-gamepad1.getLeftY());   // works OK
    // frontLeftDrive.setVoltage(Units.Volt.of(-gamepad1.getLeftY() * 12.0));  // works OK
    frontLeftDrive.setVelocitySetpoint(-gamepad1.getLeftY() * 100);
    
    leftClaw.setPosition(gamepad1.getLeftTriggerAxis());
  }

}
