// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;

import org.wpilib.hardware.bus.I2C;
import org.wpilib.hardware.hal.CANBusMap;

import first.robot.subsystems.OctoQuad;

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

  public static class DriveConstants {
    public static final int kRearMotorCanId       = CANBusMap.CAN_D0;
    public static final int kFrontRightMotorCanId = CANBusMap.CAN_D1;
    public static final int kFrontLeftMotorCanId  = CANBusMap.CAN_D2;

    public static final boolean kRearMotorInverted       = false;
    public static final boolean kFrontRightMotorInverted = false;
    public static final boolean kFrontLeftMotorInverted  = false;

    public static final double kDriveDeadband = 0.05;
    public static final double kDriveMaxRPM   = 400;

    // Drive frame of reference. When true, the driver's translation stick is field-relative:
    // pushing "forward" always moves the robot away from the field-forward direction regardless
    // of which way the robot is facing. Field-forward is defined by the IMU yaw zero at boot.
    // When false, translation is robot-relative (forward = the robot's current forward).
    public static final boolean USE_FIELD_CENTRIC = true;

    // Heading-hold (drift correction). When the driver is not commanding rotation,
    // a PID loop holds the robot's heading using the onboard IMU yaw.
    // P=0.5 verified stable on the real robot (A301 motor + 500 RPM gearbox).
    public static final double kHeadingP = 0.5;
    public static final double kHeadingI = 0.0;
    public static final double kHeadingD = 0.0;

    // Max rotation output (normalized [-1, 1]) the heading-hold loop may command.
    public static final double kHeadingMaxOutput = 0.5;

    // When the rotate stick is released, the held heading is latched immediately and the
    // PID actively arrests any residual spin. Because the robot keeps yawing while it
    // translates (the single front wheel scrubs like a sliding rear-drive car), the setpoint
    // is projected ahead by (current yaw rate * this lead time, in seconds) so the robot
    // decelerates *into* the held heading instead of overshooting it. Tune on the real robot:
    // increase if it still overshoots the release heading, decrease if it stops short.
    public static final double kHeadingLatchLeadTime = 0.10;
  }

  /**
   * OctoQuad FTC Edition MK2 (DigitalChickenLabs) — 8-channel encoder/PWM interface with an
   * onboard IMU and an absolute localizer that fuses two dead-wheel odometry pods with the IMU.
   *
   * <p>Register map, scalars, and protocol verified against the official firmware-v3 driver
   * (DigitalChickenLabs/OctoQuad, {@code code_examples/FTC/OctoQuadFWv3.java}).
   */
  public static class OctoQuadConstants {
    // The MK2 ships at I2C address 0x30 (7-bit). Pick the bus the device is wired to.
    public static final I2C.Port kI2cPort = I2C.Port.PORT_0;
    public static final int kI2cAddress = 0x30;

    // Which encoder ports the X and Y dead-wheel odometry pods are plugged into (0-7).
    public static final int kLocalizerPortX = 0;
    public static final int kLocalizerPortY = 1;

    // Which direction does each encoder run
    public static final OctoQuad.EncoderDirection kLocalizerPortXDir = OctoQuad.EncoderDirection.REVERSE;
    public static final OctoQuad.EncoderDirection kLocalizerPortYDir = OctoQuad.EncoderDirection.FORWARD;

    // Encoder counts per millimeter of travel for each pod. MEASURE these on the real robot
    // (push it a known distance, divide counts by mm) — do NOT compute them theoretically.
    public static final float kTicksPerMM_X = 13.26f;
    public static final float kTicksPerMM_Y = 13.26f;

    // Offset (mm) from the localizer's true tracking-center point to the robot point you want
    // pose reported about (usually the geometric center). See setTcpOffset docs in the driver.
    public static final float kTcpOffsetMM_X = -25.0f;
    public static final float kTcpOffsetMM_Y =  42.0f;

    // Correction factor applied to IMU heading (1.0 = none). Tune with the heading-scalar routine.
    public static final float kImuHeadingScalar = 1.00833f;

    // Translational-velocity averaging window (1-255 ms). Longer = smoother but more latent.
    public static final int kVelocityIntervalMs = 25;
  }
}
