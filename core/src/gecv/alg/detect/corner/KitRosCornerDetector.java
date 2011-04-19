package gecv.alg.detect.corner;

import gecv.struct.image.ImageBase;

/**
 * <p>
 * Implementation of the Kitchen and Rosenfeld corner detector as described in [1].  Unlike the KLT or Harris corner
 * detectors this corner detector is designed to detect corners on the actual corner.  This operator is mathematically
 * identical to calculating the horizontal curvature of the intensity image.  A reasonable indication of the corner's
 * strength is obtained by multiplying the curvature by the local intensity gradient.
 * </p>
 * <p/>
 * <p>
 * [1] Page 393 of E.R. Davies, "Machine Vision Theory Algorithms Practicalities," 3rd ed. 2005
 * </p>
 *
 * @author Peter Abeles
 */
public interface KitRosCornerDetector<T extends ImageBase> extends GradientCornerDetector<T> {
}
