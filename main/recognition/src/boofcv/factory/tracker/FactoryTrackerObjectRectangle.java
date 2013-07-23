package boofcv.factory.tracker;

import boofcv.abst.tracker.Tld_to_TrackerObjectRectangle;
import boofcv.abst.tracker.TrackerObjectRectangle;
import boofcv.alg.tracker.tld.TldConfig;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.struct.image.ImageSingleBand;

/**
 * @author Peter Abeles
 */
public class FactoryTrackerObjectRectangle {

	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	TrackerObjectRectangle<T> createTLD( TldConfig<T,D> config ) {
		TldTracker<T,D> tracker = new TldTracker<T,D>(config);

		return new Tld_to_TrackerObjectRectangle<T,D>(tracker);
	}
}
