package boofcv.abst.tracker;

import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.sfot.SparseFlowObjectTracker;
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
		SfotConfig config = new SfotConfig(ct);

		SparseFlowObjectTracker tracker = new SparseFlowObjectTracker(config);

		return new Sfot_to_TrackObjectQuad(tracker);
	}

}
