package boofcv.alg.geo.d3.epipolar;

import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.GeoTestingOps;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.NormOps;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public class TestFundamentalLinear8 {

	Random rand = new Random(234234);

	// create a reasonable calibration matrix
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,60,0.01,-200,0,80,-150,0,0,1);

	@Test
	public void perfectFundamental() {
		checkEpipolarMatrix(8,true,new FundamentalLinear8(true));
		checkEpipolarMatrix(15,true,new FundamentalLinear8(true));
	}

	@Test
	public void perfectEssential() {
		checkEpipolarMatrix(8,false,new FundamentalLinear8(false));
		checkEpipolarMatrix(15,false,new FundamentalLinear8(false));
	}

	private void checkEpipolarMatrix( int N , boolean isFundamental , FundamentalLinear8 alg ) {
		// define the camera's motion
		Se3_F64 motion = new Se3_F64();
		motion.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.05, -0.03, 0.02));
		motion.getT().set(0.1,-0.1,0.01);

		// randomly generate points in space
		List<Point3D_F64> pts = GeoTestingOps.randomPoints_F32(-1, 1, -1, 1, 2, 3, N, rand);

		// transform points into second camera's reference frame
		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		for(Point3D_F64 p1 : pts ) {
			Point3D_F64 p2 = SePointOps_F64.transform(motion, p1, null);

			AssociatedPair pair = new AssociatedPair();
			pair.keyLoc.set(p1.x/p1.z,p1.y/p1.z);
			pair.currLoc.set(p2.x/p2.z,p2.y/p2.z);
			pairs.add(pair);

			if( isFundamental ) {
				GeometryMath_F64.mult(K, pair.keyLoc, pair.keyLoc);
				GeometryMath_F64.mult(K,pair.currLoc,pair.currLoc);
			}
		}

		// compute essential
		assertTrue(alg.process(pairs));

		// validate by testing essential properties
		DenseMatrix64F F = alg.getF();

		// sanity check, F is not zero
		assertTrue(NormOps.normF(F) > 0.1 );

		// see if it follows the epipolar constraint
		for( AssociatedPair p : pairs ) {
			double val = GeometryMath_F64.innerProd(p.currLoc, F, p.keyLoc);
			assertEquals(0,val,1e-8);
		}
	}
}
