package gecv.alg.detect.corner.impl;

import gecv.alg.detect.corner.KltCornerDetector;
import gecv.alg.detect.corner.impl.SsdCorner_F32;
import gecv.struct.image.ImageFloat32;


/**
 * <p>
 * Implementation of {@link gecv.alg.detect.corner.KltCornerDetector} based off of {@link SsdCorner_F32}.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class KltCorner_F32 extends SsdCorner_F32 implements KltCornerDetector<ImageFloat32> {


	public KltCorner_F32(int imageWidth, int imageHeight, int windowRadius) {
		super(imageWidth, imageHeight, windowRadius);
	}

	@Override
	protected float computeIntensity() {
		// compute the smallest eigenvalue
		float left = (totalXX + totalYY) * 0.5f;
		float b = (totalXX - totalYY) * 0.5f;
		double right = Math.sqrt(b * b + totalXY * totalXY);

		// the smallest eigenvalue will be minus the right side
		return (float)(left - right);
	}
}