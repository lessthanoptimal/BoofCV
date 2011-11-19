package boofcv.alg.geo.d3.epipolar;

import org.ejml.simple.SimpleMatrix;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFundamentalLinear7 extends CommonFundamentalChecks{

	@Test
	public void perfectFundamental() {
		checkEpipolarMatrix(7,true,new FundamentalLinear7(true));
	}

	@Test
	public void perfectEssential() {
		checkEpipolarMatrix(7,false,new FundamentalLinear7(false));
	}

	@Test
	public void enforceZeroDeterminant() {
		for( int i = 0; i < 20; i++ ) {
			SimpleMatrix F1 = SimpleMatrix.random(3, 3, 0.1, 2, rand);
			SimpleMatrix F2 = SimpleMatrix.random(3, 3, 0.1, 2, rand);

			double alpha = FundamentalLinear7.enforceZeroDeterminant(F1.getMatrix(),F2.getMatrix(),new double[4]);

			SimpleMatrix F = F1.scale(alpha).plus(F2.scale(1 - alpha));

			System.out.println("det = "+F.determinant()+"  F1 = "+F1.determinant());

			assertEquals(0, F.determinant(), 1e-8);
		}
	}

	@Test
	public void computeCoefficients() {
		SimpleMatrix F1 = SimpleMatrix.random(3, 3, 0.1, 2, rand);
		SimpleMatrix F2 = SimpleMatrix.random(3, 3, 0.1, 2, rand);

		double coefs[] = new double[4];

		FundamentalLinear7.computeCoefficients(F1.getMatrix(),F2.getMatrix(),coefs);

		double alpha = 0.4;

		double expected = F1.scale(alpha).plus(F2.scale(1 - alpha)).determinant();
		double found = coefs[0] + alpha*coefs[1] + alpha*alpha*coefs[2] + alpha*alpha*alpha*coefs[3];

		assertEquals(expected,found,1e-8);
	}
}
