// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.driverstation.DefaultUserControls;
import org.wpilib.driverstation.Gamepad;
import org.wpilib.epilogue.Logged;
import org.wpilib.hardware.expansionhub.ExpansionHubMotor;
import org.wpilib.hardware.hal.CANBusMap;
import org.wpilib.opmode.PeriodicOpMode;
import org.wpilib.opmode.Teleop;
import org.wpilib.smartdashboard.SmartDashboard;
import org.wpilib.system.Timer;
import org.wpilib.units.Units;
import com.revrobotics.spark.A301;


@Teleop
@Logged
public class CalibrateMotor extends PeriodicOpMode {

  static final double VOLTAGE_STEP = 0.001;

  final Robot robot;
  Gamepad gamepad1;
  final static A301 frontLeftDrive = new A301(CANBusMap.CAN_D4);
  
  double  commandedThrottle  = 0;
  double  commandedVelocity = 0;
  double  measuredVelocity  = 0;
  double  kS = 0;
  double  kV = 0;
  Timer   pause = new Timer();
  
  public CalibrateMotor(Robot robot, DefaultUserControls userControls) {
    this.robot = robot;
    gamepad1 = userControls.getGamepad(0);

    // setup actuators
  }

  @Override
  public void start() {
    commandedThrottle  = 0;
    measuredVelocity  = 0;
    kS = 0;
    kV = 0;
    frontLeftDrive.setThrottle(0);  
    pause.restart();
  }

  @Override
  public void periodic() {

    // Ramp up voltage to measure velocity response.
    if (robot.isEnabled() && (pause.get() > 1)) {

      measuredVelocity = frontLeftDrive.getEncoderVelocity().get(null);

      if ((kS == 0) && (measuredVelocity > 1)) {
        kS = commandedThrottle;
      }

      if (commandedThrottle >= 1) {
        kV = (10.0 - kS) / (measuredVelocity);
      } else {
        commandedThrottle += VOLTAGE_STEP;
      }

      SmartDashboard.putNumber("throttlee",  commandedThrottle);
      SmartDashboard.putNumber("velocity", measuredVelocity);
      SmartDashboard.putNumber("kS", kS);
      SmartDashboard.putNumber("kV", kV);

      frontLeftDrive.setThrottle(commandedThrottle);
      
    } else {
      frontLeftDrive.setThrottle(0);  
    }
  }
}
