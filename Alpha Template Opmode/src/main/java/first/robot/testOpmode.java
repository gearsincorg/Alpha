// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.driverstation.DefaultUserControls;
import org.wpilib.driverstation.Gamepad;
import org.wpilib.epilogue.Logged;
import org.wpilib.hardware.hal.CANBusMap;
import org.wpilib.opmode.PeriodicOpMode;
import org.wpilib.opmode.Teleop;
import com.revrobotics.spark.A301;

@Teleop
@Logged
public class testOpmode extends PeriodicOpMode {

  final static A301 a301Motor = new A301(CANBusMap.CAN_D0);
  final Robot robot;
  Gamepad gamepad1;
  double commandedVelocity = 0;
  double measuredVelocity  = 0;
  
  public testOpmode(Robot robot, DefaultUserControls userControls) {
    this.robot = robot;
    gamepad1 = userControls.getGamepad(0);
  }

  @Override
  public void start() {
  }

  @Override
  public void periodic() {

    // Run motor with joystick
    if (robot.isEnabled()) {
      a301Motor.setThrottle(-gamepad1.getLeftY());  
    } else {
      a301Motor.setThrottle(0);  
    }
  }
}
