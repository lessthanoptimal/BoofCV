package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.KeyFrameTrack;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;

/**
 * Track information that contains extends {@link KeyFrameTrack} by providing
 * the feature's 3D location and if it is active or not
 * 
 * @author Peter Abeles
 */
public class PointPoseTrack extends KeyFrameTrack {
	// estimated 3D position
	Point3D_F64 location = new Point3D_F64();
	// only active tracks have their 3D position estimated
	boolean active = false;

	// feature location at the spawn point
	public Point2D_F64 spawnLoc = new Point2D_F64();

	long lastInlier;
	boolean original;

	@Override
	public void reset() {
		active = false;
	}

	public boolean isActive() {
		return active;
	}

	public Point3D_F64 getLocation() {
		return location;
	}

	public Point2D_F64 getSpawnLoc() {
		return spawnLoc;
	}
}
