package boofcv.alg.sfm.robust;

import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.metric.ClosestPoint3D_F64;
import georegression.struct.line.LineParametric3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDistanceSe3SymmetricSq {

	TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();
	DistanceSe3SymmetricSq alg;
	
	public TestDistanceSe3SymmetricSq() {
		alg = new DistanceSe3SymmetricSq(triangulate,1,1,0,1,1,0);
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

	/**
	 * Manually compute the error using a calibration matrix and see if they match
	 */
	@Test
	public void testIntrinsicParameters() {
		// intrinsic camera calibration matrix
		DenseMatrix64F K = new DenseMatrix64F(3,3,true,100,0.01,200,0,150,200,0,0,1);
		DenseMatrix64F K2 = new DenseMatrix64F(3,3,true,105,0.021,180,0,155,210,0,0,1);
		DenseMatrix64F K_inv = new DenseMatrix64F(3,3);
		DenseMatrix64F K2_inv = new DenseMatrix64F(3,3);
		CommonOps.invert(K,K_inv);
		CommonOps.invert(K2,K2_inv);

		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getT().set(0.1,-0.1,0.01);

		Point3D_F64 X = new Point3D_F64(0.02,-0.05,3);

		AssociatedPair obs = new AssociatedPair();
		AssociatedPair obsP = new AssociatedPair();

		obs.keyLoc.x = X.x/X.z;
		obs.keyLoc.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.currLoc.x = X.x/X.z;
		obs.currLoc.y = X.y/X.z;

		// translate into pixels
		GeometryMath_F64.mult(K,obs.keyLoc,obsP.keyLoc);
		GeometryMath_F64.mult(K2,obs.currLoc,obsP.currLoc);

		// add some noise
		obsP.keyLoc.x += 0.25;
		obsP.keyLoc.y += 0.25;
		obsP.currLoc.x -= 0.25;
		obsP.currLoc.y -= 0.25;

		// convert noisy into normalized coordinates
		GeometryMath_F64.mult(K_inv,obsP.keyLoc,obsP.keyLoc);
		GeometryMath_F64.mult(K2_inv,obsP.currLoc,obsP.currLoc);

		// triangulate the point's position given noisy data
		LineParametric3D_F64 rayA = new LineParametric3D_F64();
		LineParametric3D_F64 rayB = new LineParametric3D_F64();
		rayA.slope.set(obsP.keyLoc.x,obsP.keyLoc.y,1);
		rayB.p.set(-0.1,0.1,-0.01);
		rayB.slope.set(obsP.currLoc.x,obsP.currLoc.y,1);

		ClosestPoint3D_F64.closestPoint(rayA,rayB,X);

		// compute predicted given noisy triangulation
		AssociatedPair ugh = new AssociatedPair();
		ugh.keyLoc.x = X.x/X.z;
		ugh.keyLoc.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr, X, X);

		ugh.currLoc.x = X.x/X.z;
		ugh.currLoc.y = X.y/X.z;

		// convert everything into pixels
		GeometryMath_F64.mult(K,ugh.keyLoc,ugh.keyLoc);
		GeometryMath_F64.mult(K2,ugh.currLoc,ugh.currLoc);
		GeometryMath_F64.mult(K,obsP.keyLoc,obsP.keyLoc);
		GeometryMath_F64.mult(K2,obsP.currLoc,obsP.currLoc);

		double dx1 = ugh.keyLoc.x - obsP.keyLoc.x;
		double dy1 = ugh.keyLoc.y - obsP.keyLoc.y;
		double dx2 = ugh.currLoc.x - obsP.currLoc.x;
		double dy2 = ugh.currLoc.y - obsP.currLoc.y;

		double error = dx1*dx1 + dy1*dy1 + dx2*dx2 + dy2*dy2;

		// convert noisy back into normalized coordinates
		GeometryMath_F64.mult(K_inv,obsP.keyLoc,obsP.keyLoc);
		GeometryMath_F64.mult(K2_inv,obsP.currLoc,obsP.currLoc);

		DistanceSe3SymmetricSq alg = new DistanceSe3SymmetricSq(triangulate,
				K.get(0,0),K.get(1,1),K.get(0,1),
				K2.get(0,0),K2.get(1,1),K2.get(0,1));
		alg.setModel(keyToCurr);
		assertEquals(error, alg.computeDistance(obsP), 1e-8);
	}

}
