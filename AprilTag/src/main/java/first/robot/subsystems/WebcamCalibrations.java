// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.subsystems;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Loads camera intrinsics from the {@code webcams.xml} resource and looks them up by USB vendor id,
 * product id, and capture resolution.
 *
 * <p>The file is an FTC-style calibration file: each {@code <Camera>} carries a numeric {@code vid}
 * and {@code pid}, and each {@code <Calibration>} gives the {@code size}, {@code focalLength}, and
 * {@code principalPoint} for one resolution. Values are parsed forgivingly (an optional trailing
 * {@code f} and either spaces or commas between the two numbers are both accepted).
 *
 * <p>If no exact resolution match exists for a camera, a calibration of the same aspect ratio is
 * scaled to fit (see {@link CameraCalibration#scaledTo}).
 */
public final class WebcamCalibrations {
  /** Default classpath location of the calibration file. */
  public static final String kDefaultResource = "/first/robot/calibrations/webcams.xml";

  /** One entry as read from the file, tagged with the camera it belongs to. */
  private record Entry(int vid, int pid, CameraCalibration calibration) {}

  private final List<Entry> entries;

  private WebcamCalibrations(List<Entry> entries) {
    this.entries = entries;
  }

  /**
   * Loads calibrations from the given classpath resource.
   *
   * @param resourcePath absolute classpath path (e.g. {@link #kDefaultResource})
   * @return the loaded calibrations
   * @throws IOException if the resource is missing or cannot be parsed
   */
  public static WebcamCalibrations loadFromResource(String resourcePath) throws IOException {
    try (InputStream in = WebcamCalibrations.class.getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new IOException("Calibration resource not found on classpath: " + resourcePath);
      }
      return new WebcamCalibrations(parse(in));
    }
  }

  /** Loads calibrations from {@link #kDefaultResource}. */
  public static WebcamCalibrations loadDefault() throws IOException {
    return loadFromResource(kDefaultResource);
  }

  /**
   * Finds the best calibration for a camera at a capture resolution.
   *
   * @param vid USB vendor id (as reported by {@code UsbCameraInfo.vendorId})
   * @param pid USB product id (as reported by {@code UsbCameraInfo.productId})
   * @param width capture width in pixels
   * @param height capture height in pixels
   * @return an exact match if present, otherwise a same-aspect-ratio calibration scaled to the
   *     requested size, or empty if the camera has no usable calibration
   */
  public Optional<CameraCalibration> find(int vid, int pid, int width, int height) {
    // Exact (vid, pid, size) match wins.
    for (Entry e : entries) {
      if (e.vid() == vid
          && e.pid() == pid
          && e.calibration().width() == width
          && e.calibration().height() == height) {
        return Optional.of(e.calibration());
      }
    }

    // Otherwise scale a same-aspect-ratio calibration for this camera, preferring the closest size.
    double targetAspect = (double) width / height;
    CameraCalibration best = null;
    long bestPixelDelta = Long.MAX_VALUE;
    for (Entry e : entries) {
      if (e.vid() != vid || e.pid() != pid) {
        continue;
      }
      CameraCalibration c = e.calibration();
      if (Math.abs((double) c.width() / c.height() - targetAspect) > 1e-3) {
        continue;
      }
      long delta = Math.abs((long) c.width() * c.height() - (long) width * height);
      if (delta < bestPixelDelta) {
        bestPixelDelta = delta;
        best = c;
      }
    }
    return Optional.ofNullable(best).map(c -> c.scaledTo(width, height));
  }

  private static List<Entry> parse(InputStream in)
      throws IOException {
    Document doc;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setIgnoringComments(true);
      doc = factory.newDocumentBuilder().parse(in);
    } catch (ParserConfigurationException | SAXException ex) {
      throw new IOException("Failed to parse webcam calibrations", ex);
    }

    List<Entry> result = new ArrayList<>();
    NodeList cameras = doc.getElementsByTagName("Camera");
    for (int i = 0; i < cameras.getLength(); i++) {
      Element camera = (Element) cameras.item(i);
      int vid = Integer.decode(camera.getAttribute("vid").trim());
      int pid = Integer.decode(camera.getAttribute("pid").trim());

      NodeList children = camera.getChildNodes();
      for (int j = 0; j < children.getLength(); j++) {
        Node child = children.item(j);
        if (child.getNodeType() != Node.ELEMENT_NODE || !"Calibration".equals(child.getNodeName())) {
          continue;
        }
        Element cal = (Element) child;
        int[] size = parseIntPair(cal.getAttribute("size"), "size");
        double[] focal = parseDoublePair(cal.getAttribute("focalLength"), "focalLength");
        double[] principal = parseDoublePair(cal.getAttribute("principalPoint"), "principalPoint");
        result.add(
            new Entry(
                vid,
                pid,
                new CameraCalibration(
                    size[0], size[1], focal[0], focal[1], principal[0], principal[1])));
      }
    }
    return result;
  }

  /** Splits a "a b" / "a, b" attribute (with optional trailing 'f' on each value) into two tokens. */
  private static String[] tokens(String raw, String attr) {
    String[] parts = raw.trim().replace("f", "").split("[,\\s]+");
    if (parts.length != 2) {
      throw new IllegalArgumentException(
          "Expected two values for '" + attr + "' but got: \"" + raw + "\"");
    }
    return parts;
  }

  private static int[] parseIntPair(String raw, String attr) {
    String[] t = tokens(raw, attr);
    return new int[] {Integer.parseInt(t[0]), Integer.parseInt(t[1])};
  }

  private static double[] parseDoublePair(String raw, String attr) {
    String[] t = tokens(raw, attr);
    return new double[] {Double.parseDouble(t[0]), Double.parseDouble(t[1])};
  }
}
