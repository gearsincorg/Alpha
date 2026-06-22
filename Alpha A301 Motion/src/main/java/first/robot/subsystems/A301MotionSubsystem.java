// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.subsystems;

import com.revrobotics.spark.A301;
import first.robot.Constants.A301Constants;
import org.wpilib.command2.SubsystemBase;
import org.wpilib.driverstation.RobotState;
import org.wpilib.smartdashboard.SmartDashboard;

/**
 * Drives two REV Robotics A301 motors independently for motion-smoothness testing.
 *
 * <p>The control scheme is split across two rates:
 *
 * <ul>
 *   <li><b>50 Hz</b> (the command, via {@link #setVelocityRequest(int, double)} / {@link
 *       #stop(int)}): the operator requests a forward or reverse <em>velocity</em> for each axis.
 *   <li><b>200 Hz</b> ({@link #integrate()}, registered through {@code TimedRobot.addPeriodic}):
 *       each axis's velocity request is integrated into a position step that is streamed to that
 *       motor as a sequence of {@link A301#setPosition(double)} calls.
 * </ul>
 *
 * <p>The two axes are fully independent: {@link #kAxisD3} (motor on CAN D3) and {@link #kAxisD4}
 * (motor on CAN D4) each carry their own velocity request and integrated target, so they can be
 * driven separately by different buttons.
 *
 * <p>{@code A301.setPosition} commands the <em>relative</em> encoder in rotations, so on every
 * teleop enable {@link #seedFromAbsoluteEncoder()} reads each motor's absolute encoder and seeds
 * that motor's relative encoder and target to its true shaft angle. Each motor then advances by its
 * own velocity&times;dt step each tick, starting from wherever it physically is, so there is no jump
 * or fighting at enable. With no button held an axis's velocity request is zero, so its target
 * freezes and the motor actively holds position.
 *
 * <p>Threading: {@code addPeriodic} callbacks run on the main robot thread, the same thread as the
 * command scheduler, so the fields shared between the 50 Hz and 200 Hz paths need no
 * synchronization.
 */
public class A301MotionSubsystem extends SubsystemBase {
  /** Axis index of the motor on CAN D3 (driven by DPAD Up/Down). */
  public static final int kAxisD3 = 0;

  /** Axis index of the motor on CAN D4 (driven by North/South face buttons). */
  public static final int kAxisD4 = 1;

  /** One A301 motor plus its own velocity request and integrated position target. */
  private static final class A301Axis {
    final A301 motor;

    /** Operator-requested velocity, in rotations per second. Written at 50 Hz. */
    double velocityRequestRps = 0.0;

    /** Integrated position setpoint for this motor, in rotations. Advanced at 200 Hz. */
    double targetPositionRot = 0.0;

    A301Axis(int canBusId, int deviceId, boolean inverted) {
      motor = new A301(canBusId, deviceId);
      motor.setInverted(inverted);
    }

    /** Seeds the relative encoder and target from this motor's own absolute encoder. */
    void seed() {
      double absPositionRot = motor.getAbsoluteEncoderPosition().get(0.0);
      motor.setRelativeEncoderPosition(absPositionRot);
      targetPositionRot = absPositionRot;
      velocityRequestRps = 0.0;
    }

    /** Integrates this axis's own velocity request and commands the motor to the new target. */
    void step(double dtSeconds) {
      targetPositionRot += velocityRequestRps * dtSeconds;
      motor.setPosition(targetPositionRot);
    }
  }

  private final A301Axis[] axes = {
    new A301Axis(A301Constants.kCanBusId1, A301Constants.kDeviceId, false),
    new A301Axis(A301Constants.kCanBusId2, A301Constants.kDeviceId, true),
  };

  /** True once the targets have been seeded from the absolute encoders. */
  private boolean seeded = false;

  /** Creates a new A301MotionSubsystem. */
  public A301MotionSubsystem() {}

  /**
   * Requests a velocity for one axis. Called at the 50 Hz periodic rate by the command.
   *
   * @param axis which axis to drive ({@link #kAxisD3} or {@link #kAxisD4})
   * @param rotationsPerSecond requested velocity; positive is forward, negative is reverse
   */
  public void setVelocityRequest(int axis, double rotationsPerSecond) {
    axes[axis].velocityRequestRps = rotationsPerSecond;
  }

  /**
   * Stops one axis by zeroing its velocity request, which holds that axis's integrated target.
   *
   * @param axis which axis to stop ({@link #kAxisD3} or {@link #kAxisD4})
   */
  public void stop(int axis) {
    axes[axis].velocityRequestRps = 0.0;
  }

  /**
   * Seeds each motor's relative encoder and target from its own absolute encoder. Call once on every
   * teleop enable so {@link A301#setPosition(double)} starts from the true shaft angle instead of
   * wherever the relative encoder happened to be.
   */
  public void seedFromAbsoluteEncoder() {
    for (A301Axis axis : axes) {
      axis.seed();
    }
    seeded = true;
  }

  /**
   * Fast loop: integrates each axis's velocity request into a position step and pushes it to that
   * motor. Registered via {@code TimedRobot.addPeriodic} at {@link
   * A301Constants#kIntegrationPeriodSeconds}.
   *
   * <p>Does nothing until the targets have been seeded, and skips while the robot is disabled so the
   * setpoints cannot wind up away from the shafts when the motors are not being driven.
   */
  public void integrate() {
    if (!seeded || !RobotState.isEnabled()) {
      return;
    }
    for (A301Axis axis : axes) {
      axis.step(A301Constants.kIntegrationPeriodSeconds);
    }
  }

  @Override
  public void periodic() {
    for (int i = 0; i < axes.length; i++) {
      A301Axis axis = axes[i];
      String prefix = "A301/motor" + i + "/";
      SmartDashboard.putNumber(prefix + "velocityRequestRps", axis.velocityRequestRps);
      SmartDashboard.putNumber(prefix + "targetPositionRot", axis.targetPositionRot);
      SmartDashboard.putNumber(
          prefix + "absPositionRot", axis.motor.getAbsoluteEncoderPosition().get(0.0));
      SmartDashboard.putNumber(
          prefix + "relPositionRot", axis.motor.getRelativeEncoderPosition().get(0.0));
    }
  }
}
