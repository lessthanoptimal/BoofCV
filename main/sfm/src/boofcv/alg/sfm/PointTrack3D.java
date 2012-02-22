package boofcv.alg.sfm;

import boofcv.alg.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;

/**
 * @author Peter Abeles
 */
public class PointTrack3D {
	// estimated 3D position of the point
	Point3D_F64 location = new Point3D_F64();

	// feature observation at the key frame and most recent frame
	AssociatedPair pair = new AssociatedPair();
}
