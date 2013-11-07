package boofcv.abst.tracker;

import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

/**
 * @author Peter Abeles
 */
public class TestMsl_to_TrackerObjectQuad extends ColorTrackerObjectRectangleTests {

	public TestMsl_to_TrackerObjectQuad() {
		super(false);
		tolTranslateSmall = 0.05;
		// it adjusts the size for even regions which can cause an offset by one
		tolStationary = 1.001;
	}

	@Override
	public TrackerObjectQuad<MultiSpectral<ImageUInt8>> create(ImageType<MultiSpectral<ImageUInt8>> imageType) {
		return FactoryTrackerObjectQuad.meanShiftLikelihood(30, 6, 255, MeanShiftLikelihoodType.HISTOGRAM, imageType);
	}

	@Override
	public void translation_large() {
		// Not designed to handle
	}

	@Override
	public void zooming_in() {
		// Not designed to handle
	}

	@Override
	public void zooming_out() {
		// Not designed to handle
	}

}
