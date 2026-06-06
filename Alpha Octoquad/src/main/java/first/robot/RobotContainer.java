// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.command2.Command;
import org.wpilib.command2.Commands;
import org.wpilib.command2.RunCommand;
import org.wpilib.command2.button.CommandGamepad;
import org.wpilib.command2.button.Trigger;
import org.wpilib.math.util.MathUtil;
import first.robot.Constants.DriveConstants;
import first.robot.Constants.OperatorConstants;
import first.robot.subsystems.DriveSubsystem;
import first.robot.subsystems.OctoQuadSubsystem;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems and commands are defined here...
  private final DriveSubsystem m_driveSubsystem = new DriveSubsystem();
  private final OctoQuadSubsystem m_octoQuad = new OctoQuadSubsystem();

  private final CommandGamepad driverController =
      new CommandGamepad(OperatorConstants.kDriverControllerPort);

  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {
    // Push localizer config and start IMU calibration. Robot must be stationary at boot.
    m_octoQuad.initialize();
    m_driveSubsystem.resetHeading();

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
    // Kiwi drive: left stick = translate, right stick X = rotate.
    // Heading hold auto-corrects drift when the driver isn't commanding rotation.
    m_driveSubsystem.setDefaultCommand(
        new RunCommand(
            () -> m_driveSubsystem.driveWithHeadingHold(
                MathUtil.applyDeadband(driverController.getLeftX() / 4.0, DriveConstants.kDriveDeadband),
                MathUtil.applyDeadband(-driverController.getLeftY() / 4.0, DriveConstants.kDriveDeadband),
                MathUtil.applyDeadband(-driverController.getRightX() / 4.0, DriveConstants.kDriveDeadband)),
            m_driveSubsystem));

    // North face button: zero the IMU yaw (and re-sync the heading-hold PID to the new heading).
    // No subsystem requirement, so this fires without interrupting the default drive command.
    driverController.northFace().onTrue(Commands.runOnce(m_driveSubsystem::resetHeading)
                                .andThen(Commands.runOnce(m_octoQuad::resetHeading)));
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {
    // An example command will be run in autonomous
    return null; 
  }
}
