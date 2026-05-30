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
public class A301Testing extends PeriodicOpMode {

  final static double MAX_VELOCITY = 200;

  final Robot robot;
  Gamepad gamepad1;
  double commandedVelocity = 0;
  double measuredVelocity  = 0;
  
  public A301Testing(Robot robot, DefaultUserControls userControls) {
    this.robot = robot;
    gamepad1 = userControls.getGamepad(0);
  }

  @Override
  public void start() {
    commandedVelocity = 0;
    measuredVelocity  = 0;
  }

  @Override
  public void periodic() {

    // Step target velocity up and down with gamepad Y and A
    if (robot.isEnabled()) {

      measuredVelocity = robot.leftDrive.getEncoderVelocity().get();

      SmartDashboard.putNumber("setpoint", commandedVelocity);
      SmartDashboard.putNumber("velocity", measuredVelocity);

      // step the requested velocity
      double axial   = -gamepad1.getLeftY();
      double lateral = -gamepad1.getRightX();
      
      // set the target velocity
      robot.leftDrive.setVelocity(-(axial - lateral) * MAX_VELOCITY);
      robot.rightDrive.setVelocity((axial + lateral) * MAX_VELOCITY);

    } else {
      robot.leftDrive.setThrottle(0);  
      robot.rightDrive.setThrottle(0);  
    }
  }
}
