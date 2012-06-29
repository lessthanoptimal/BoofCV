package boofcv.numerics.solver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestFitQuadratic1D {

	@Test
	public void basic() {
		double a = 2.5;
		double b = -1.0;
		double c = 0.5;

		double values[] = new double[10];

		for( int i = 0; i < values.length; i++ ) {
			values[i] = a*i*i + b*i + c;
		}

		FitQuadratic1D alg = new FitQuadratic1D();

		assertTrue(alg.process(0,values.length,values));

		double coef[] = alg.getCoefficients();

		assertEquals(a,coef[0],1e-8);
		assertEquals(b,coef[1],1e-8);
		assertEquals(c,coef[2],1e-8);
	}
}
