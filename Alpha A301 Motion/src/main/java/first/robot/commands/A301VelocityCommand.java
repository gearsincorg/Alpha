// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.commands;

import first.robot.Constants.A301Constants;
import first.robot.subsystems.A301MotionSubsystem;
import java.util.function.BooleanSupplier;
import org.wpilib.command2.Command;

/**
 * Default command for {@link A301MotionSubsystem}: polls one or more button pairs at the 50 Hz
 * scheduler rate and requests a forward or reverse velocity for each pair's axis.
 *
 * <p>The two axes are driven independently from the same single command (the subsystem can only have
 * one default command), each from its own pair of buttons:
 *
 * <ul>
 *   <li>forward button held: request {@code +}{@link A301Constants#kTestVelocityRotationsPerSecond}.
 *   <li>reverse button held: request {@code -}{@link A301Constants#kTestVelocityRotationsPerSecond}.
 *   <li>neither (or both) held: stop that axis, which holds its current position.
 * </ul>
 *
 * <p>The same velocity constant is used for every axis. This never finishes; as the subsystem's
 * default command it runs whenever no other command requires the subsystem.
 */
public class A301VelocityCommand extends Command {
  /** Maps one axis to the forward/reverse buttons that drive it. */
  public record AxisBinding(int axis, BooleanSupplier forward, BooleanSupplier reverse) {}

  private final A301MotionSubsystem subsystem;
  private final AxisBinding[] bindings;

  /**
   * @param subsystem the A301 motion subsystem to drive
   * @param bindings one button pair per axis to control
   */
  public A301VelocityCommand(A301MotionSubsystem subsystem, AxisBinding... bindings) {
    this.subsystem = subsystem;
    this.bindings = bindings;
    addRequirements(subsystem);
  }

  @Override
  public void execute() {
    for (AxisBinding binding : bindings) {
      boolean forward = binding.forward().getAsBoolean();
      boolean reverse = binding.reverse().getAsBoolean();
      if (forward && !reverse) {
        subsystem.setVelocityRequest(binding.axis(), A301Constants.kTestVelocityRotationsPerSecond);
      } else if (reverse && !forward) {
        subsystem.setVelocityRequest(
            binding.axis(), -A301Constants.kTestVelocityRotationsPerSecond);
      } else {
        subsystem.stop(binding.axis());
      }
    }
  }

  @Override
  public void end(boolean interrupted) {
    for (AxisBinding binding : bindings) {
      subsystem.stop(binding.axis());
    }
  }

  @Override
  public boolean isFinished() {
    return false;
  }
}
