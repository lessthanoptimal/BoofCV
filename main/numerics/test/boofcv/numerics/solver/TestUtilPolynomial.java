package boofcv.numerics.solver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestUtilPolynomial {

	@Test
	public void quadraticVertex() {
		double a = -0.5;
		double b = 2.0;

		double x = -b/(2*a);

		double found = UtilPolynomial.quadraticVertex(a,b);

		assertEquals(x,found,1e-8);
	}
}
