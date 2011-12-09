package boofcv.alg.geo.calibration;

import boofcv.alg.calibration.CalibrationGridConfig;
import boofcv.alg.calibration.CalibrationPlanarGridZhang98;
import boofcv.alg.calibration.ParametersZhang98;
import georegression.misc.test.GeometryUnitTest;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestCalibrationPlanarGridZhang98 {

	Random rand = new Random(234);

	@Test
	public void fullTest() {
		CalibrationGridConfig config = GenericCalibrationGrid.createStandardConfig();
		List<Point2D_F64> grid = config.computeGridPoints();
		ParametersZhang98 initial = GenericCalibrationGrid.createStandardParam(true,2,3,rand);

		List<List<Point2D_F64>> observations = GenericCalibrationGrid.createObservations(initial,grid);

		CalibrationPlanarGridZhang98 alg = new CalibrationPlanarGridZhang98(config,true,2);

		assertTrue(alg.process(observations));

		fail("add test here");
//		checkIntrinsicOnly(initial, found,0.01,0.1);
	}

	/**
	 * See how well it computes an initial guess at the parameters given perfect inputs
	 */
	@Test
	public void initialParam() {
		CalibrationGridConfig config = GenericCalibrationGrid.createStandardConfig();
		List<Point2D_F64> grid = config.computeGridPoints();
		ParametersZhang98 initial = GenericCalibrationGrid.createStandardParam(true,2,3,rand);

		List<List<Point2D_F64>> observations = GenericCalibrationGrid.createObservations(initial,grid);

		Helper alg = new Helper(config,true,2);

		ParametersZhang98 found = alg.initialParam(observations);

		checkIntrinsicOnly(initial, found,0.01,0.1);
	}

	/**
	 * Test nonlinear optimization with perfect inputs
	 */
	@Test
	public void optimizedParam_perfect() {

		CalibrationGridConfig config = GenericCalibrationGrid.createStandardConfig();
		List<Point2D_F64> grid = config.computeGridPoints();
		ParametersZhang98 initial = GenericCalibrationGrid.createStandardParam(true,2,3,rand);
		ParametersZhang98 found = new ParametersZhang98(2,3);

		List<List<Point2D_F64>> observations = GenericCalibrationGrid.createObservations(initial,grid);

		assertTrue(CalibrationPlanarGridZhang98.optimizedParam(observations, grid, initial, found));

		checkEquals(initial, found);
	}

	/**
	 * Test nonlinear optimization with a bit of noise
	 */
	@Test
	public void optimizedParam_noisy() {

		CalibrationGridConfig config = GenericCalibrationGrid.createStandardConfig();
		List<Point2D_F64> grid = config.computeGridPoints();
		ParametersZhang98 initial = GenericCalibrationGrid.createStandardParam(true,2,3,rand);
		ParametersZhang98 expected = initial.copy();
		ParametersZhang98 found = new ParametersZhang98(2,3);

		List<List<Point2D_F64>> observations = GenericCalibrationGrid.createObservations(initial,grid);

		// add a tinny bit of noise
		initial.a += rand.nextDouble()*0.01*Math.abs(initial.a);
		initial.b += rand.nextDouble()*0.01*Math.abs(initial.b);
		initial.c += rand.nextDouble()*0.01*Math.abs(initial.c);
		initial.x0 += rand.nextDouble()*0.01*Math.abs(initial.x0);
		initial.y0 += rand.nextDouble()*0.01*Math.abs(initial.y0);

		assertTrue(CalibrationPlanarGridZhang98.optimizedParam(observations,grid,initial,found));

		checkEquals(expected, found);
	}

	private void checkIntrinsicOnly(ParametersZhang98 initial,
									ParametersZhang98 found , double tolK , double tolD ) {
		assertEquals(initial.a,found.a,Math.abs(initial.a)*tolK);
		assertEquals(initial.b,found.b,Math.abs(initial.b)*tolK);
		assertEquals(initial.c,found.c,Math.abs(initial.c)*tolK);
		assertEquals(initial.x0,found.x0,Math.abs(initial.x0)*tolK);
		assertEquals(initial.y0, found.y0, Math.abs(initial.y0) * tolK);

		for( int i = 0; i < initial.distortion.length; i++ ) {
			assertEquals(initial.distortion[i],found.distortion[i],tolD);
		}
	}

	public static void checkEquals( ParametersZhang98 expected , ParametersZhang98 found )
	{
		double tol=1e-6;

		assertEquals(expected.a,found.a,tol);
		assertEquals(expected.b,found.b,tol);
		assertEquals(expected.c,found.c,tol);
		assertEquals(expected.x0,found.x0,tol);
		assertEquals(expected.y0,found.y0,tol);

		for( int i = 0; i < expected.distortion.length; i++ ) {
			assertEquals(expected.distortion[i],found.distortion[i],tol);
		}

		for( int i = 0; i < 2; i++ ) {
			ParametersZhang98.View pp = expected.views[i];
			ParametersZhang98.View ff = found.views[i];

			GeometryUnitTest.assertEquals(pp.T, ff.T, tol);
			GeometryUnitTest.assertEquals(pp.rotation.unitAxisRotation,ff.rotation.unitAxisRotation,tol);
			assertEquals(pp.rotation.theta,ff.rotation.theta,tol);
		}
	}

	private static class Helper extends CalibrationPlanarGridZhang98
	{

		public Helper(CalibrationGridConfig config, boolean assumeZeroSkew, int numSkewParam) {
			super(config, assumeZeroSkew, numSkewParam);
		}

		public ParametersZhang98 initialParam( List<List<Point2D_F64>> observations ) {
			return super.initialParam(observations);
		}
	}
}
