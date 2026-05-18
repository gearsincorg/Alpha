// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.driverstation.DefaultUserControls;
import org.wpilib.opmode.PeriodicOpMode;
import org.wpilib.opmode.Teleop;
import org.wpilib.smartdashboard.SmartDashboard;
import org.wpilib.system.Timer;

@Teleop
public class DefaultTeleMode extends PeriodicOpMode {
  private final Robot robot;
  private final DefaultUserControls userControls;
  private Timer sysTimer = new Timer();
  private Timer delayTimer = new Timer();
  private double lastCycleTime = 0;
  private double cycleTime = 0;
  private double cyclePeriod = 0;
  private double cycleActive = 0;

  public DefaultTeleMode(Robot robot, DefaultUserControls userControls) {
    this.robot = robot;
    this.userControls = userControls;
    sysTimer.start();
  }

  @Override
  public void periodic() {
    cycleTime = sysTimer.get();
    cyclePeriod = cycleTime - lastCycleTime;
    
    SmartDashboard.putBoolean("Phil", robot.motor0.isHubConnected());
    //robot.motor0.setThrottle(0);
    
    delay(0.2);

    robot.motor0.setThrottle(-userControls.getGamepad(0).getLeftY());
    cycleActive = sysTimer.get() - cycleTime;
    SmartDashboard.putNumber("Cycle Period", cyclePeriod * 1000);
    SmartDashboard.putNumber("Cycle Active", cycleActive * 1000);

    lastCycleTime = cycleTime;
  }

  public void delay(double seconds){
    delayTimer.restart();
    while (!delayTimer.hasElapsed(seconds)){
      //robot.motor0.setThrottle(-userControls.getGamepad(0).getLeftY());
    };
  }
}
