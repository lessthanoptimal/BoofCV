package boofcv.alg.geo.calibration;

import boofcv.alg.calibration.Zhang98DecomposeHomography;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestZhang98DecomposeHomography {

	/**
	 * Test against a simple known case
	 */
	@Test
	public void knownCase() {
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(0.02, -0.05, 0.01, null);
		Vector3D_F64 T = new Vector3D_F64(100,50,-1000);
		DenseMatrix64F K = GenericCalibrationGrid.createStandardCalibration();
		DenseMatrix64F H = GenericCalibrationGrid.computeHomography(K,R,T);

		Zhang98DecomposeHomography alg = new Zhang98DecomposeHomography();
		alg.setCalibrationMatrix(K);
		Se3_F64 motion = alg.decompose(H);

		assertTrue(MatrixFeatures.isIdentical(R, motion.getR(), 1e-5));
		assertEquals(T.x,motion.getX(), 1e-5);
	}
}
