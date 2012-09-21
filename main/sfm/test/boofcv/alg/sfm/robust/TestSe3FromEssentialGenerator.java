package boofcv.alg.sfm.robust;

import boofcv.abst.geo.EpipolarMatrixEstimator;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.AssociatedPair;
import boofcv.factory.geo.FactoryEpipolar;
import boofcv.factory.geo.FactoryTriangulate;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSe3FromEssentialGenerator {

	Random rand = new Random(34);

	@Test
	public void simpleTest() {
		// define motion
		Se3_F64 motion = new Se3_F64();
		motion.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.1, -0.05, -0.01));
		motion.getT().set(2,-0.1,0.1);

		// define observations
		List<AssociatedPair> obs = new ArrayList<AssociatedPair>();

		for( int i = 0; i < 8; i++ ) {
			Point3D_F64 p = new Point3D_F64(rand.nextGaussian()*0.1,rand.nextGaussian()*0.1,3+rand.nextGaussian()*0.1);

			AssociatedPair o = new AssociatedPair();

			o.keyLoc.x = p.x/p.z;
			o.keyLoc.y = p.y/p.z;

			Point3D_F64 pp = new Point3D_F64();
			SePointOps_F64.transform(motion,p,pp);

			o.currLoc.x = pp.x/pp.z;
			o.currLoc.y = pp.y/pp.z;

			obs.add(o);
		}

		// create alg
		EpipolarMatrixEstimator essentialAlg = FactoryEpipolar.computeFundamentalOne(8, true, 0);
		TriangulateTwoViewsCalibrated triangulate = FactoryTriangulate.twoGeometric();

		Se3FromEssentialGenerator alg = new Se3FromEssentialGenerator(essentialAlg,triangulate);

		Se3_F64 found = alg.createModelInstance();

		// recompute the motion
		assertTrue(alg.generate(obs, found));

		// account for scale difference
		double scale = found.getT().norm()/motion.getT().norm();

		assertTrue(MatrixFeatures.isIdentical(motion.getR(),found.getR(),1e-6));

		assertEquals(motion.getT().x*scale,found.getT().x,1e-8);
		assertEquals(motion.getT().y*scale,found.getT().y,1e-8);
		assertEquals(motion.getT().z*scale,found.getT().z,1e-8);
	}

}
