// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.opmode.Autonomous;
import org.wpilib.opmode.PeriodicOpMode;
import org.wpilib.system.Timer;

@Autonomous
public class DefaultAutoMode extends PeriodicOpMode {
  private final Robot robot;
  private final Timer timer = new Timer();

  public DefaultAutoMode(Robot robot) {
    this.robot = robot;
  }

  @Override
  public void start() {
    timer.reset();
    timer.start();
  }

  @Override
  public void periodic() {
  }
}
