// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.driverstation.DefaultUserControls;
import org.wpilib.driverstation.UserControlsInstance;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.hardware.hal.CANBusMap;

import com.revrobotics.spark.A301;

/**
 * This is a demo program showing the use of the Expansion Hub motors and servos. The motors and
 * servos are driven using the controllers in the telop opmode, and timed in the auto op mode.
 */
@UserControlsInstance(DefaultUserControls.class)
public class Robot extends OpModeRobot {

  final static A301 leftDrive = new A301(CANBusMap.CAN_D0);
  final static A301 rightDrive = new A301(CANBusMap.CAN_D1);
  
  /** Called once at the beginning of the robot program. */
  public Robot() {
    leftDrive.setInverted(true);
  }
}
