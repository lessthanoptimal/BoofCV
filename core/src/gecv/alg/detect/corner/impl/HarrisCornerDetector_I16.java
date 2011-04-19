package gecv.alg.detect.corner.impl;

import gecv.alg.detect.corner.HarrisCornerDetector;
import gecv.alg.detect.corner.impl.SsdCorner_I16;
import gecv.struct.image.ImageInt16;

/**
 * <p>
 * Implementation of {@link gecv.alg.detect.corner.HarrisCornerDetector} based off of {@link SsdCorner_I16}.
 * </p>
 *
 * @author Peter Abeles
 */
public class HarrisCornerDetector_I16 extends SsdCorner_I16 implements HarrisCornerDetector<ImageInt16> {

	float kappa = 0.04f;

	public HarrisCornerDetector_I16(int imageWidth, int imageHeight, int windowRadius) {
		super(imageWidth, imageHeight, windowRadius);
	}

	@Override
	protected float computeIntensity() {
		// det(A) + kappa*trace(A)
		return totalXX * totalYY - totalXY * totalXY + kappa * (totalXX + totalYY);
	}

	@Override
	public float getKappa() {
		return kappa;
	}
}
