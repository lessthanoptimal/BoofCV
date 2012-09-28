package boofcv.alg.sfm.robust;

import boofcv.struct.geo.PointPositionPair;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDistanceTranGivenRotSq {

	DistanceTranGivenRotSq alg;

	public TestDistanceTranGivenRotSq() {
		alg = new DistanceTranGivenRotSq();
	}

	@Test
	public void testPerfect() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.05, -0.03, 0.02));
		keyToCurr.getT().set(0.1,-0.1,0.01);

		Point3D_F64 X = new Point3D_F64(0.1,-0.05,3);

		PointPositionPair obs = new PointPositionPair();
		obs.location = X.copy();

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.observed.x = X.x/X.z;
		obs.observed.y = X.y/X.z;

		alg.setRotation(keyToCurr.getR());
		alg.setModel(keyToCurr.getT());
		assertEquals(0, alg.computeDistance(obs), 1e-8);
	}

	@Test
	public void testNoisy() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.05, -0.03, 0.02));
		keyToCurr.getT().set(0.1,-0.1,0.01);

		Point3D_F64 X = new Point3D_F64(0.1,-0.05,3);

		PointPositionPair obs = new PointPositionPair();
		obs.location = X.copy();

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.observed.x = X.x/X.z+1;
		obs.observed.y = X.y/X.z+1;

		alg.setRotation(keyToCurr.getR());
		alg.setModel(keyToCurr.getT());
		assertTrue(alg.computeDistance(obs) > 1e-8);
	}
}
