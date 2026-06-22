// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.hardware.hal.CANBusMap;

import com.revrobotics.spark.A301;

/**
 * The Constants class provides a convenient place for teams to hold robot-wide numerical or boolean
 * constants. This class should not be used for any other purpose. All constants should be declared
 * globally (i.e. public static). Do not put anything functional in this class.
 *
 * <p>It is advised to statically import this class (or one of its inner classes) wherever the
 * constants are needed, to reduce verbosity.
 */
public final class Constants {
  public static class OperatorConstants {
    public static final int kDriverControllerPort = 0;
  }

  /** Constants for the A301 motion-smoothness test (subsystem + velocity command). */
  public static class A301Constants {
    // --- Hardware (adjust to match the actual wiring) ---
    /** CAN bus the first A301 is wired to. */
    public static final int kCanBusId1 = CANBusMap.CAN_D3;

    /** CAN bus the second A301 is wired to. */
    public static final int kCanBusId2 = CANBusMap.CAN_D4;

    /**
     * A301 device ID. Both motors use the factory default ({@link A301#defaultDeviceId}, 3); they
     * are distinguished by being on separate CAN buses.
     */
    public static final int kDeviceId = A301.defaultDeviceId;

    /**
     * Period of the fast integration loop, in seconds (200 Hz). {@code Robot} registers the
     * integrator with {@code addPeriodic} using this value, and the subsystem uses it as the
     * integration time step, so it is the single source of truth for the fast rate.
     */
    public static final double kIntegrationPeriodSeconds = 0.005;

    /** Velocity requested while a DPAD button is held, in rotations per second. */
    public static final double kTestVelocityRotationsPerSecond = 0.05;
  }
}
