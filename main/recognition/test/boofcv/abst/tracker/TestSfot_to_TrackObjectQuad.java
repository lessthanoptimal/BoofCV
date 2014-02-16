package boofcv.abst.tracker;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.sfot.SparseFlowObjectTracker;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class TestSfot_to_TrackObjectQuad extends TextureGrayTrackerObjectRectangleTests {

	@Override
	public TrackerObjectQuad<ImageUInt8> create(ImageType<ImageUInt8> imageType) {

		Class ct = ImageDataType.typeToSingleClass(imageType.getDataType());
		Class dt = GImageDerivativeOps.getDerivativeType(ct);

		ImageGradient gradient = FactoryDerivative.sobel(ct, dt);
		SfotConfig config = new SfotConfig();

		SparseFlowObjectTracker tracker = new SparseFlowObjectTracker(config,ct,dt,gradient);

		return new Sfot_to_TrackObjectQuad(tracker,ct);
	}

}
