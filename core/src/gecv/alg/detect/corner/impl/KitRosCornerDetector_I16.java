package gecv.alg.detect.corner.impl;

import gecv.alg.detect.corner.KitRosCornerDetector;
import gecv.alg.detect.corner.impl.SsdCorner_I16;
import gecv.struct.image.ImageInt16;

/**
 * <p>
 * Implementation of {@link gecv.alg.detect.corner.KitRosCornerDetector} based off of {@link SsdCorner_I16}.
 * </p>
 *
 * @author Peter Abeles
 */
public class KitRosCornerDetector_I16 extends SsdCorner_I16 implements KitRosCornerDetector<ImageInt16> {

	public KitRosCornerDetector_I16(int imageWidth, int imageHeight, int windowRadius) {
		super(imageWidth, imageHeight, windowRadius);
	}

	protected float computeIntensity() {
		// accessing the derivative images in this fashion is likely causes a large performance hit
		// in benchmark tests it does perform significantly slower than Harris and this is the most likely
		// culprit.
		double dX = derivX.get(x, y);
		double dY = derivY.get(x, y);

		double xx = dX * dX;
		double yy = dY * dY;

		double bottom = xx + yy;

		if (bottom == 0.0)
			return 0;

		double top = totalXX * yy - 2 * totalXY * dX * dY + totalYY * xx;
		return (float)(top / bottom);
	}
}
