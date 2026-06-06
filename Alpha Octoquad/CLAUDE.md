# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FRC (FIRST Robotics Competition) robot software for Team 2818, built on WPILib 2027 alpha 5 with Java 25. Uses the WPILib Command-based programming framework.

## Build & Deploy Commands

```bash
# Build
gradlew build

# Run tests
gradlew test

# Simulate on desktop (no robot hardware needed)
gradlew simulateJava

# Deploy to robot controller
gradlew deploy

# Build fat JAR only
gradlew shadowJar

# Clean
gradlew clean
```

The project deploys to a "systemcore" robot controller target. Static resources go in `src/main/deploy/` and are copied to `/home/systemcore/deploy/` on the robot.

## Architecture

**Entry point:** `first.Main` → `first.robot.Robot` → `first.robot.RobotContainer`

**Command-based pattern — key concepts:**
- `Robot` (extends `TimedRobot`) owns the main lifecycle. Its `robotPeriodic()` ticks the `CommandScheduler` every 20ms. Robot-mode methods (`autonomousInit()`, `teleopInit()`, etc.) should stay thin — schedule/cancel commands, don't put logic here.
- `RobotContainer` is the composition root. Declare all subsystems and operator inputs here; wire triggers to commands in `configureBindings()`.
- **Subsystems** extend `SubsystemBase`. Only one command can require a given subsystem at a time — the scheduler enforces this. Add robot state to subsystems; expose it via command-factory methods.
- **Commands** extend `Command` and declare their subsystem requirements. Implement `initialize()`, `execute()`, `end(boolean interrupted)`, and `isFinished()`.
- **Triggers** connect state (gamepad buttons, subsystem conditions) to commands via `.onTrue()` / `.whileTrue()`.
- `first.robot.Constants` is the canonical home for all tuning values and hardware port numbers.

**Autonomous:** `first.robot.commands.Autos` is a factory class for auto routines. `Robot.autonomousInit()` schedules the selected routine.

## Key Details

- Main class for GradleRIO: `first.Main`
- CommandsV2 vendor library (conflicts with V3 — don't add both)
- Tests use JUnit 5 Jupiter with WPILib simulation support; native libs are on `build/jni/release`
- VS Code launch configs: "WPILib Desktop Debug" (simulation) and "WPILib roboRIO Debug" (real robot)
- All current subsystem/command/auto classes are template stubs — replace with real robot logic
