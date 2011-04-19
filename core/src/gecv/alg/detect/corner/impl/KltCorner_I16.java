package gecv.alg.detect.corner.impl;

import gecv.alg.detect.corner.KltCornerDetector;
import gecv.struct.image.ImageInt16;


/**
 * <p>
 * Implementation of {@link gecv.alg.detect.corner.KltCornerDetector} based off of {@link SsdCornerNaive_I16}.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class KltCorner_I16 extends SsdCorner_I16 implements KltCornerDetector<ImageInt16> {
	public KltCorner_I16(int imageWidth, int imageHeight, int windowRadius) {
		super(imageWidth, imageHeight, windowRadius);
	}

	@Override
	protected float computeIntensity() {
		// compute the smallest eigenvalue
		float left = (totalXX + totalYY) * 0.5f;
		float b = (totalXX - totalYY) * 0.5f;
		double right = Math.sqrt(b * b + (double)totalXY * totalXY);

		// the smallest eigenvalue will be minus the right side
		return (float)(left - right);
	}
}