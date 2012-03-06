package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.struct.image.ImageBase;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestMonocularSimpleVo {

	@Test
	public void updatePosition() {
		
	}
	
	/**
	 * Make sure the reference frame transformations are correctly being handled
	 */
	@Test
	public void triangulateNew() {
		MonocularSimpleVo alg = new MonocularSimpleVo(0,0,0,null,null,null,null,null,null);

		// create three motions, tranA is the location of the camera when the keyframe was made
		Se3_F64 tranAtoW = new Se3_F64();
		Se3_F64 tranBtoA = new Se3_F64();

		tranAtoW.getT().set(0.1,-0.3,1);
		RotationMatrixGenerator.eulerXYZ(0,0.05,0,tranAtoW.getR());
		tranBtoA.getT().set(1,0,0);

		Se3_F64 tranBtoW = tranBtoA.concat(tranAtoW,null);

		// create a point in space
		Point3D_F64 pointWorld = new Point3D_F64(0,0,5);

		// compute its location at each point in time
		Point3D_F64 pointA = new Point3D_F64();
		Point3D_F64 pointAB = new Point3D_F64();

		SePointOps_F64.transformReverse(tranAtoW, pointWorld, pointA);
		SePointOps_F64.transformReverse(tranBtoW, pointWorld, pointAB);
		
		// compute the observations
		PointPoseTrack track = new PointPoseTrack();
		track.keyLoc.set(pointA.x/pointA.z,pointA.y/pointA.z);
		track.currLoc.set(pointAB.x/pointAB.z,pointAB.y/pointAB.z);
		
		alg.keyToSpawn = tranAtoW;
		alg.keyToCurr = tranBtoW;

		// set up the tracker
		DummyTracker tracker = new DummyTracker();
		tracker.tracks.add(track);
		alg.tracker = tracker;
		
		// process and test
		alg.triangulateNew();
		
		assertEquals(pointWorld.x,track.location.x,1e-8);
		assertEquals(pointWorld.y,track.location.y,1e-8);
		assertEquals(pointWorld.z, track.location.z, 1e-8);
		assertTrue(track.active);
	}

	private class DummyTracker extends KeyFramePointTracker<ImageBase,PointPoseTrack> {

		List<PointPoseTrack> tracks = new ArrayList<PointPoseTrack>();

		@Override
		public List<PointPoseTrack> getPairs() {
			return tracks;
		}
	}
}
