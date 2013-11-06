package boofcv.abst.tracker;

import boofcv.alg.tracker.tld.TldConfig;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.struct.image.ImageDataType;
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
	public TrackerObjectQuad<ImageUInt8> create(ImageType<ImageUInt8> imageTypee) {

		Class ct = ImageDataType.typeToClass(imageType.getDataType());
		TldConfig config = new TldConfig(false,ct);

		config.trackerFeatureRadius = 10;
		TldTracker tracker = new TldTracker(config);

		return new Tld_to_TrackerObjectQuad(tracker);
	}
}
