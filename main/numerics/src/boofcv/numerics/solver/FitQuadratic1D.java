package boofcv.numerics.solver;

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolver;
import org.ejml.factory.LinearSolverFactory;

/**
 * Fits the coefficients for a quadratic polynomial to a set of even spaced data in an array.
 *
 * y = a*x<sup>2</sp> + b*x + c
 *
 * The coefficients (a,b,c) of the polynomial are found the solving a system of linear
 * equations that minimizes the least squares error.
 *
 * @author Peter Abeles
 */
public class FitQuadratic1D {

	LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.leastSquares(10,3);

	DenseMatrix64F A = new DenseMatrix64F(1,3);
	DenseMatrix64F x = new DenseMatrix64F(3,1);
	DenseMatrix64F y = new DenseMatrix64F(1,1);

	/**
	 * Computes polynomial coefficients for the given data.
	 *
	 * @param length Number of elements in data with relevant data.
	 * @param data Set of observation data.
	 * @return true if successful or false if it fails.
	 */
	public boolean process( int offset , int length , double ...data ) {
		if( data.length < 3 )
			throw new IllegalArgumentException("At least three points");

		A.reshape(data.length,3);
		y.reshape(data.length,1);

		int indexDst = 0;
		int indexSrc = offset;
		for( int i = 0; i < length; i++ ) {
			double d = data[indexSrc++];

			A.data[indexDst++] = i*i;
			A.data[indexDst++] = i;
			A.data[indexDst++] = 1;

			y.data[i] = d;
		}

		if( !solver.setA(A) )
			return false;

		solver.solve(y,x);

		return true;
	}

	/**
	 *
	 * @return The coefficients [a,b,c]
	 */
	public double[] getCoefficients() {
		return x.data;
	}
}
