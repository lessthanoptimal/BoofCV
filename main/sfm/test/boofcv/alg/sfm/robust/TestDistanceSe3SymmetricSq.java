package boofcv.alg.sfm.robust;

import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.AssociatedPair;
import boofcv.factory.geo.FactoryTriangulate;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestDistanceSe3SymmetricSq {

	TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();
	DistanceSe3SymmetricSq alg;
	
	public TestDistanceSe3SymmetricSq() {
		alg = new DistanceSe3SymmetricSq(triangulate,1,1,0);
	}
	
	@Test
	public void testPerfect() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.05, -0.03, 0.02));
		keyToCurr.getT().set(0.1,-0.1,0.01);

		Point3D_F64 X = new Point3D_F64(0.1,-0.05,3);

		AssociatedPair obs = new AssociatedPair();

		obs.keyLoc.x = X.x/X.z;
		obs.keyLoc.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr,X,X);

		obs.currLoc.x = X.x/X.z;
		obs.currLoc.y = X.y/X.z;

		alg.setModel(keyToCurr);
		assertEquals(0, alg.computeDistance(obs), 1e-8);
	}

	@Test
	public void testNoisy() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.05, -0.03, 0.02));
		keyToCurr.getT().set(0.1,-0.1,0.01);

		Point3D_F64 X = new Point3D_F64(0.1,-0.05,3);

		AssociatedPair obs = new AssociatedPair();

		obs.keyLoc.x = X.x/X.z;
		obs.keyLoc.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.currLoc.x = X.x/X.z+1;
		obs.currLoc.y = X.y/X.z+1;

		alg.setModel(keyToCurr);
		assertTrue(alg.computeDistance(obs) > 1e-8);
	}

	@Test
	public void testBehindCamera() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.05, -0.03, 0.02));
		keyToCurr.getT().set(0.1,-0.1,0.01);

		Point3D_F64 X = new Point3D_F64(0.1,-0.05,-3);

		AssociatedPair obs = new AssociatedPair();

		obs.keyLoc.x = X.x/X.z;
		obs.keyLoc.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr,X,X);

		obs.currLoc.x = X.x/X.z;
		obs.currLoc.y = X.y/X.z;

		alg.setModel(keyToCurr);
		assertTrue(Double.MAX_VALUE == alg.computeDistance(obs));
	}

	@Test
	public void testIntrinsicParameters() {
		fail("Implement0");
	}

}
