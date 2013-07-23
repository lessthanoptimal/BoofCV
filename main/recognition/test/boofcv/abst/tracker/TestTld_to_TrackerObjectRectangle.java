package boofcv.abst.tracker;

import boofcv.alg.tracker.tld.TldConfig;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class TestTld_to_TrackerObjectRectangle extends GenericTrackerObjectRectangleTests {

	@Override
	public TrackerObjectRectangle<ImageUInt8> create(Class<ImageUInt8> imageType) {

		TldConfig config = new TldConfig(imageType);

		config.trackerFeatureRadius = 10;
		TldTracker tracker = new TldTracker(config);

		return new Tld_to_TrackerObjectRectangle(tracker);
	}
}
