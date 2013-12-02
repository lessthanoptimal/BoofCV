package boofcv.abst.tracker;

import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestCirculant_to_TrackerObjectQuad extends TextureGrayTrackerObjectRectangleTests {

	public TestCirculant_to_TrackerObjectQuad() {
		tolStationary = 1;
	}

	@Override
	public TrackerObjectQuad<ImageUInt8> create(ImageType<ImageUInt8> imageType) {

		ConfigCirculantTracker config = new ConfigCirculantTracker();

		return FactoryTrackerObjectQuad.circulant(config, ImageUInt8.class);
	}

	@Test
	public void zooming_in() {
		// not supported
	}

	@Test
	public void zooming_out() {
		// not supported
	}
}
