package boofcv.abst.tracker;

import boofcv.alg.tracker.tld.TldTracker;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.shapes.RectangleCorner2D_F64;

/**
 * Wrapper around {@link boofcv.alg.tracker.tld.TldTracker} for {@link boofcv.abst.tracker.TrackerObjectRectangle}.
 *
 * @author Peter Abeles
 */
public class Tld_to_TrackerObjectRectangle<T extends ImageSingleBand, D extends ImageSingleBand>
		implements TrackerObjectRectangle<T>
{
	TldTracker<T,D> tracker;

	public Tld_to_TrackerObjectRectangle(TldTracker<T, D> tracker) {
		this.tracker = tracker;
	}

	@Override
	public boolean initialize(T image, int x0, int y0, int x1, int y1) {

		tracker.initialize(image,x0,y0,x1,y1);

		return true;
	}

	@Override
	public boolean process(T image, RectangleCorner2D_F64 location) {

		if( !tracker.track(image) )
			return false;

		location.set(tracker.getTargetRegion());

		return true;
	}
}
