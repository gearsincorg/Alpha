// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import first.robot.commands.A301VelocityCommand;
import first.robot.commands.A301VelocityCommand.AxisBinding;
import first.robot.subsystems.A301MotionSubsystem;
import org.wpilib.command2.Command;
import org.wpilib.command2.Commands;
import org.wpilib.command2.button.CommandGamepad;
import org.wpilib.command2.button.Trigger;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems and commands are defined here...

  private final CommandGamepad gamepad = new CommandGamepad(0);

  // Drives the A301 motor for motion-smoothness testing.
  private final A301MotionSubsystem a301Motion = new A301MotionSubsystem();

  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {
    // Configure the trigger bindings
    configureBindings();
  }

  /**
   * Use this method to define your trigger->command mappings. Triggers can be created via the
   * {@link Trigger#Trigger(java.util.function.BooleanSupplier)} constructor with an arbitrary
   * predicate, or via the named factories in {@link org.wpilib.command2.button.CommandGenericHID}'s
   * subclasses for {@link CommandGamepad Gamepad} gamepads or {@link
   * org.wpilib.command2.button.CommandJoystick Flight joysticks}.
   */
  private void configureBindings() {
    // Poll the two button pairs at the 50 Hz scheduler rate to request forward/reverse velocity
    // for each arm independently. When neither button of a pair is held that axis stops (holds
    // position).
    //   DPAD Up/Down  -> motor D3
    //   North/South   -> motor D4
    a301Motion.setDefaultCommand(
        new A301VelocityCommand(
            a301Motion,
            new AxisBinding(A301MotionSubsystem.kAxisD3, gamepad.dpadUp(), gamepad.dpadDown()),
            new AxisBinding(
                A301MotionSubsystem.kAxisD4, gamepad.northFace(), gamepad.southFace())));
  }

  /**
   * The A301 motion subsystem, exposed so {@link Robot} can drive its 200 Hz integration loop and
   * seed it from the absolute encoder on teleop enable.
   *
   * @return the A301 motion subsystem
   */
  public A301MotionSubsystem getA301Motion() {
    return a301Motion;
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    // No autonomous routine yet.
    return Commands.none();
  }
}
