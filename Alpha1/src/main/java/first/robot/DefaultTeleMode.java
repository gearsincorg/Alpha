// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.littletonrobotics.junction.AutoLogOutput;
import org.wpilib.driverstation.DefaultUserControls;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.hardware.discrete.DigitalInput;
import org.wpilib.hardware.discrete.DigitalOutput;
import org.wpilib.opmode.PeriodicOpMode;
import org.wpilib.opmode.Teleop;
import org.wpilib.smartdashboard.SmartDashboard;

@Teleop
public class DefaultTeleMode extends PeriodicOpMode {
  private final Robot robot;
  private final DefaultUserControls userControls;
  
  private DigitalOutput digOut0 = new DigitalOutput(0);
  private DigitalInput  digIn1  = new DigitalInput(1);
  private boolean digBit0 = false;
  private boolean digBit1 = false;

  private OpModeRobot base;

  //--------------------------------------------------------------------
  @AutoLogOutput double philDrive;

  public DefaultTeleMode(Robot robot, DefaultUserControls userControls) {
    this.robot = robot;
    this.userControls = userControls;
  }

  @Override
  public void start(){
    SmartDashboard.putString("Phil", "Start");
  }

  @Override
  public void disabledPeriodic() {
    SmartDashboard.putString("Phil", "Disabled Periodic");
  };

  @Override
  public void periodic() {
    digBit1 = digIn1.get();
    digOut0.set(digBit1);
  }

  
  @Override
  public void end(){
    SmartDashboard.putString("Phil", "End");
  }

  //==============================================

}
