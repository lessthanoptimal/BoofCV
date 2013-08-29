package boofcv.abst.tracker;

import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.sfot.SparseFlowObjectTracker;
import boofcv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class TestSfot_to_TrackObjectQuad extends GenericTrackerObjectRectangleTests {

	@Override
	public TrackerObjectQuad<ImageUInt8> create(Class<ImageUInt8> imageType) {

		SfotConfig config = new SfotConfig(imageType);

		SparseFlowObjectTracker tracker = new SparseFlowObjectTracker(config);

		return new Sfot_to_TrackObjectQuad(tracker);
	}

}
