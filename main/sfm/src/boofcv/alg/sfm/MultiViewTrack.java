package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.KeyFrameTrack;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class MultiViewTrack extends KeyFrameTrack {
	// estimated 3D position
	Point3D_F64 location = new Point3D_F64();

	// Initial 3D position, which might not be used
	Point3D_F64 candidate = new Point3D_F64();

	// list of all the views from which the point can be triangulated from
	List<View> views = new ArrayList<View>();

	// tick when spawned
	long whenSpawned;

	@Override
	public void reset() {
		views.clear();
	}

	public static class View
	{
		public Point2D_F64 o = new Point2D_F64();
		public Se3_F64 worldToView = new Se3_F64();
		public long whenViewed;
	}
}
