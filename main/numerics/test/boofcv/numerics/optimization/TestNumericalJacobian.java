package boofcv.numerics.optimization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestNumericalJacobian {

	/**
	 * Compare a numerical gradient to an analytically computed gradient from a non-linear system.
	 */
	@Test
	public void compareToKnown() {
		OptimizationFunction<double[],double[]> func = new TestLevenbergMarquardt.NonlinearResidual();
		OptimizationDerivative<double[]> derivTrue = new TestLevenbergMarquardt.NonlinearGradient();

		OptimizationDerivative<double[]> deriv = new NumericalJacobian<double[],double[]>(func);

		double[]model = new double[]{1,2};
		double[]state = new double[]{1.7};
		double[][]gradient = new double[1][2];
		double[][]expected = new double[1][2];

		deriv.setModel(model);
		deriv.computeDerivative(state,gradient);

		derivTrue.setModel(model);
		derivTrue.computeDerivative(state,expected);

		assertEquals(expected[0][0],gradient[0][0],1e-5);
		assertEquals(expected[0][1],gradient[0][1],1e-5);
	}
}
