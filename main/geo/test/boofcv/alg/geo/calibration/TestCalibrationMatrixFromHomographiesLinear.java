package boofcv.alg.geo.calibration;

import boofcv.alg.calibration.CalibrationMatrixFromHomographiesLinear;
import boofcv.alg.geo.d3.epipolar.UtilEpipolar;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestCalibrationMatrixFromHomographiesLinear {

	Random rand = new Random(123);
	List<DenseMatrix64F> homographies;

	@Test
	public void withSkew() {

		DenseMatrix64F K = GenericCalibrationGrid.createStandardCalibration();

		// try different numbers of observations
		for( int N = 3; N <= 6; N++ ) {
			setup(K,N);

			CalibrationMatrixFromHomographiesLinear alg = new CalibrationMatrixFromHomographiesLinear(false);

			alg.process(homographies);

			DenseMatrix64F K_found = alg.getCalibrationMatrix();

			checkK(K,K_found);
		}
	}

	@Test
	public void withNoSkew() {

		// test a bunch
		for( int i = 0; i < 10; i++ ) {
			DenseMatrix64F K = GenericCalibrationGrid.createStandardCalibration();
			// force skew to zero
			K.set(0,1,0);

			setup(K,2);

			CalibrationMatrixFromHomographiesLinear alg = new CalibrationMatrixFromHomographiesLinear(true);

			alg.process(homographies);

			DenseMatrix64F K_found = alg.getCalibrationMatrix();

			checkK(K, K_found);
		}
	}

	/**
	 * compare two calibration matrices against each other taking in account the differences in tolerance
	 * for different elements
	 */
	private void checkK( DenseMatrix64F a , DenseMatrix64F b ) {
		assertEquals(a.get(0,0),b.get(0,0),0.3);
		assertEquals(a.get(1,1),b.get(1,1),0.3);
		assertEquals(a.get(0,1),b.get(0,1),0.08);
		assertEquals(a.get(0,2),b.get(0,2),10);
		assertEquals(a.get(1,2),b.get(1,2),10);
		assertEquals(a.get(2,2),b.get(2,2),1e-8);
	}

	/**
	 * Creates several random uncalibrated homographies
	 * @param K Calibration matrix
	 * @param N Number of homographies
	 */
	private void setup( DenseMatrix64F K , int N ) {
		List<Se3_F64> motions = new ArrayList<Se3_F64>();

		for( int i = 0; i < N; i++ ) {
			double x = rand.nextGaussian()*200;
			double y = rand.nextGaussian()*200;
			double z = rand.nextGaussian()*50-1000;

			double rotX = (rand.nextDouble()-0.5)*0.1;
			double rotY = (rand.nextDouble()-0.5)*0.1;
			double rotZ = (rand.nextDouble()-0.5)*0.1;

			motions.add( SpecialEuclideanOps_F64.setEulerXYZ(x,y,z,rotX,rotY,rotZ,null));
		}

		homographies = new ArrayList<DenseMatrix64F>();
		for( Se3_F64 se : motions ) {
			DenseMatrix64F H = UtilEpipolar.computeHomography(se.getR(),se.getT(),1000,new Vector3D_F64(0,0,1),K);

			homographies.add(H);
		}

	}


}
