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

  public DefaultAutoMode(Robot robot) {
    this.robot = robot;
  }


  @Override
  public void disabledPeriodic() {
    SmartDashboard.putString("Phil", "AUTO Disabled Periodic");
  };


  @Override
  public void start() {
    SmartDashboard.putString("Phil", "AUTO Start");
    timer.reset();
    timer.start();
  }

  @Override
  public void periodic() {
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
    SmartDashboard.putString("Phil", "AUTO Periodic");
  }

  @Override
  public void end(){
    SmartDashboard.putString("Phil", "AUTO End");
  }

}
