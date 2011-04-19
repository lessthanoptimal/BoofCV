package gecv.alg.detect.corner;

import gecv.struct.image.ImageBase;

/**
 * @author Peter Abeles
 */
public interface HarrisCornerDetector<T extends ImageBase> extends GradientCornerDetector<T> {

	public float getKappa();
}
