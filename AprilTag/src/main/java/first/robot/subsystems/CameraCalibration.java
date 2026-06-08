// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.subsystems;

/**
 * Camera intrinsics for one camera at one capture resolution.
 *
 * @param width capture width in pixels these intrinsics were calibrated at
 * @param height capture height in pixels these intrinsics were calibrated at
 * @param fx focal length in pixels (x)
 * @param fy focal length in pixels (y)
 * @param cx principal point x in pixels
 * @param cy principal point y in pixels
 */
public record CameraCalibration(int width, int height, double fx, double fy, double cx, double cy) {
  /**
   * Returns a copy of these intrinsics scaled to a different resolution of the same aspect ratio.
   * Intrinsics scale linearly with image size, so this is valid only when the target shares this
   * calibration's aspect ratio (the caller is responsible for checking that).
   *
   * @param newWidth target width in pixels
   * @param newHeight target height in pixels
   * @return the scaled intrinsics
   */
  public CameraCalibration scaledTo(int newWidth, int newHeight) {
    double sx = (double) newWidth / width;
    double sy = (double) newHeight / height;
    return new CameraCalibration(newWidth, newHeight, fx * sx, fy * sy, cx * sx, cy * sy);
  }
}
