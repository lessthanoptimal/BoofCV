package boofcv.alg.geo.d3.epipolar;

import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.misc.test.GeometryUnitTest;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDecomposeEssential {

	/**
	 * Check the decomposition against a known input.  See if the solutions have the expected
	 * properties and at least one matches the input.
	 */
	@Test
	public void checkAgainstKnown() {
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(0.1,-0.4,0.5,null);
		Vector3D_F64 T = new Vector3D_F64(2,1,-3);
		T.normalize();

		DenseMatrix64F T_hat = GeometryMath_F64.crossMatrix(T,null);

		DenseMatrix64F E = new DenseMatrix64F(3,3);
		CommonOps.mult(T_hat,R,E);

		DecomposeEssential alg = new DecomposeEssential();
		alg.decompose(E);

		List<Se3_F64> solutions = alg.getSolutions();

		assertEquals(4,solutions.size());

		checkUnique(solutions);

		checkHasOriginal(solutions,E);
	}

	/**
	 * Makes sure each solution returned is unique by transforming a point.
	 */
	private void checkUnique( List<Se3_F64> solutions ) {

		Point3D_F64 orig = new Point3D_F64(1,2,3);

		for( int i = 0; i < solutions.size(); i++ ) {
			Point3D_F64 found = SePointOps_F64.transform(solutions.get(i),orig,null);

			for( int j = i+1; j < solutions.size(); j++ ) {
				Point3D_F64 alt = SePointOps_F64.transform(solutions.get(j),orig,null);

				GeometryUnitTest.assertNotEquals(found,alt,1e-4);
			}
		}
	}

	/**
	 * See if an equivalent to the input matrix exists
	 */
	private void checkHasOriginal( List<Se3_F64> solutions , DenseMatrix64F origE ) {

		int numMatches = 0;
		for( Se3_F64 se : solutions ) {
			DenseMatrix64F T_hat = GeometryMath_F64.crossMatrix(se.getT(),null);

			DenseMatrix64F foundE = new DenseMatrix64F(3,3);
			CommonOps.mult(T_hat,se.getR(),foundE);

			if(MatrixFeatures.isIdentical(origE,foundE,1e-4))
				numMatches++;
		}

		assertEquals(2,numMatches);
	}
}
