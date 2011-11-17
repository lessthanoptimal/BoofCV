package boofcv.alg.geo.d3.epipolar;

import boofcv.alg.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
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
public class TestHomographyLinear4 {

	Random rand = new Random(234234);

	// create a reasonable calibration matrix
	DenseMatrix64F K = new DenseMatrix64F(3,3,true,60,0.01,-200,0,80,-150,0,0,1);

	@Test
	public void perfectCalibrated() {
		// test the minimum number of points
		checkEpipolarMatrix(4,false,new HomographyLinear4(false));
		// test with extra points
		checkEpipolarMatrix(10,false,new HomographyLinear4(false));
	}

	@Test
	public void perfectPixels() {
		checkEpipolarMatrix(4,true,new HomographyLinear4(true));
		checkEpipolarMatrix(10,true,new HomographyLinear4(true));
	}

	/**
	 * Create a set of points perfectly on a plane and provide perfect observations of them
	 *
	 * @param N Number of observed points.
	 * @param isPixels Pixel or calibrated coordinates
	 * @param alg Algorithm being evaluated
	 */
	private void checkEpipolarMatrix( int N , boolean isPixels , HomographyLinear4 alg ) {
		// define the camera's motion
		Se3_F64 motion = new Se3_F64();
		motion.getR().set(RotationMatrixGenerator.eulerArbitrary(0, 1, 2, 0.05, -0.03, 0.02));
		motion.getT().set(0.1,-0.1,0.01);

		// randomly generate points in space
		List<Point3D_F64> pts = createRandomPlane(N);

		// transform points into second camera's reference frame
		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		for(Point3D_F64 p1 : pts ) {
			Point3D_F64 p2 = SePointOps_F64.transform(motion, p1, null);

			AssociatedPair pair = new AssociatedPair();
			pair.keyLoc.set(p1.x/p1.z,p1.y/p1.z);
			pair.currLoc.set(p2.x/p2.z,p2.y/p2.z);
			pairs.add(pair);

			if( isPixels ) {
				GeometryMath_F64.mult(K, pair.keyLoc, pair.keyLoc);
				GeometryMath_F64.mult(K, pair.currLoc,pair.currLoc);
			}
		}

		// compute essential
		assertTrue(alg.process(pairs));

		// validate by testing essential properties
		DenseMatrix64F H = alg.getHomography();

		// sanity check, F is not zero
		assertTrue(NormOps.normF(H) > 0.001 );

		// see if it follows the epipolar constraint
		for( AssociatedPair p : pairs ) {
			Point2D_F64 a = GeometryMath_F64.mult(H,p.keyLoc,new Point2D_F64());

			double diff = a.distance(p.currLoc);
			assertEquals(0,diff,1e-8);
		}
	}

	/**
	 * Creates a set of random points along the (X,Y) plane
	 */
	private List<Point3D_F64> createRandomPlane( int N )
	{
		List<Point3D_F64> ret = new ArrayList<Point3D_F64>();

  		for( int i = 0; i < N; i++ ) {
			  double x = (rand.nextDouble()-0.5)*2;
			  double y = (rand.nextDouble()-0.5)*2;

			  ret.add( new Point3D_F64(x,y,3));
		  }

		return ret;
	}
}
