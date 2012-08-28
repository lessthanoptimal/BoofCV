package boofcv.numerics.solver;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPolynomialOps {

	@Test
	public void quadraticVertex() {
		double a = -0.5;
		double b = 2.0;

		double x = -b/(2*a);

		double found = PolynomialOps.quadraticVertex(a, b);

		assertEquals(x,found,1e-8);
	}

	@Test
	public void findRealRoot() {
		Polynomial p = Polynomial.wrap(-1.322309e+02 , 3.713984e+02 , -5.007874e+02 , 3.744386e+02 ,-1.714667e+02  , 4.865014e+01 ,-1.059870e+01  ,  1.642273e+00 ,-2.304341e-01,2.112391e-03,-2.273737e-13);

		double root = PolynomialOps.findRealRoot(p,1000);
		double error = p.evaluate(root);
		System.out.println("Found root = "+root+"  with value "+error);
	}

	@Test
	public void derivative() {
		Polynomial p = Polynomial.wrap(2,3,4,5);
		p.size = 2;

		Polynomial d = new Polynomial(10);

		PolynomialOps.derivative(p,d);
		assertTrue(d.isIdentical(Polynomial.wrap(3), 1e-8));

		p.size = 3;
		PolynomialOps.derivative(p,d);
		assertTrue(d.isIdentical(Polynomial.wrap(3,8),1e-8));
	}


	@Test
	public void refineRoot() {

	}


	@Test
	public void divide() {
		// numerator and denominator, intentionally add trailing zeros to skew things up
		Polynomial n = Polynomial.wrap(1,2,3,0);
		Polynomial d = Polynomial.wrap(-3,1,0);

		// quotient and remainder
		Polynomial q = new Polynomial(10);
		Polynomial r = new Polynomial(10);

		// expected solutions
		Polynomial expectedQ = Polynomial.wrap(11,3,0);
		Polynomial expectedR = Polynomial.wrap(34);

		PolynomialOps.divide(n,d,q,r);

		assertEquals(3,q.size);
		assertEquals(1,r.size);

		assertTrue(expectedQ.isIdentical(q,1e-8));
		assertTrue(expectedR.isIdentical(r,1e-8));
	}
}
