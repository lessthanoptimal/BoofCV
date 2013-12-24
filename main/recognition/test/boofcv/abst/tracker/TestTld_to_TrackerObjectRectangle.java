package boofcv.abst.tracker;

import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class TestTld_to_TrackerObjectRectangle extends TextureGrayTrackerObjectRectangleTests {

	public TestTld_to_TrackerObjectRectangle() {
		tolScale = 0.15;
	}

	@Override
	public TrackerObjectQuad<ImageUInt8> create(ImageType<ImageUInt8> imageType) {
		return FactoryTrackerObjectQuad.tld(new ConfigTld(),ImageUInt8.class);
	}
}
