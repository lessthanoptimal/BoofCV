package boofcv.struct.sfm;

import boofcv.struct.geo.Point2D3D;

/**
 * Adds track maintenance information for {@link Point2D3D}.
 * 
 * @author Peter Abeles
 */
public class Point2D3DTrack extends Point2D3D {

	// the tick in which it was last an inlier
	public long lastInlier;

}
