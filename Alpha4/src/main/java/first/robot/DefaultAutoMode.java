// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.opmode.Autonomous;
import org.wpilib.opmode.PeriodicOpMode;
import org.wpilib.smartdashboard.SmartDashboard;
import org.wpilib.system.Timer;

@Autonomous
public class DefaultAutoMode extends PeriodicOpMode {
  private final Robot robot;
  private final Timer timer = new Timer();
  private Timer sysTimer = new Timer();
  private double lastCycleTime = 0;
  private double cycleTime = 0;
  private double cyclePeriod = 0;
  private double cycleActive = 0;

  public DefaultAutoMode(Robot robot) {
    this.robot = robot;
    sysTimer.start();
  }

  @Override
  public void start() {
    timer.reset();
    timer.start();
  }

  @Override
  public void periodic() {
    cycleTime = sysTimer.get();
    cyclePeriod = cycleTime - lastCycleTime;


    if (timer.get() < 2.0) {
      robot.motor0.setThrottle(0.5);
      robot.motor1.setThrottle(0.5);
    } else if (timer.get() < 4.0) {
      robot.motor0.setThrottle(0.9);
      robot.motor1.setThrottle(0.9);
    } else {
      robot.motor0.setThrottle(0.0);
      robot.motor1.setThrottle(0.0);
    }

    cycleActive = sysTimer.get() - cycleTime;
    SmartDashboard.putNumber("Cycle Period", cyclePeriod * 1000);
    SmartDashboard.putNumber("Cycle Active", cycleActive * 1000000);

    lastCycleTime = cycleTime;

  }
}
