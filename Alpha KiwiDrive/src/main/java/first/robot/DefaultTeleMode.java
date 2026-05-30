// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.driverstation.DefaultUserControls;
import org.wpilib.driverstation.Gamepad;
import org.wpilib.hardware.expansionhub.ExpansionHubMotor;
import org.wpilib.opmode.PeriodicOpMode;
import org.wpilib.opmode.Teleop;

@Teleop
public class DefaultTeleMode extends PeriodicOpMode {
  private final Robot robot;
  private final Gamepad gamepad;
  private final double yFactor = Math.sqrt(3) / 2;

  public DefaultTeleMode(Robot robot, DefaultUserControls userControls) {
    this.robot = robot;
    gamepad = userControls.getGamepad(0);
  }

  @Override
  public void periodic() {
    double leftStickX = -gamepad.getLeftX();
    double leftStickY = gamepad.getLeftY();
    double rightStickX = gamepad.getRightX();

    double motorAPower = -leftStickX;
    double motorBPower = leftStickX / 2;
    double motorCPower = leftStickX / 2;

    leftStickY = leftStickY * yFactor;
    motorBPower += -(leftStickY);
    motorCPower += leftStickY;

    motorAPower += rightStickX;
    motorBPower += rightStickX;
    motorCPower += rightStickX;

    if (Math.abs(motorAPower) > 1 || Math.abs(motorBPower) > 1 || Math.abs(motorCPower) > 1) {
      double maxPower = findAbsoluteMax(motorAPower, motorBPower, motorCPower);
      motorAPower /= maxPower;
      motorBPower /= maxPower;
      motorCPower /= maxPower;
    }

    robot.motora.setThrottle(motorAPower);
    robot.motorb.setThrottle(motorBPower);
    robot.motorc.setThrottle(motorCPower);
  }
    
  double findAbsoluteMax(double a, double b, double c){
    double max;
    max = Math.max(Math.abs(a), Math.abs(b));
    max = Math.max(max, Math.abs(c));
    return max;
  }

}
