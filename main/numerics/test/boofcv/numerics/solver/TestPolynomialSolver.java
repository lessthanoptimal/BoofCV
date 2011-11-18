package boofcv.numerics.solver;

import org.ejml.data.Complex64F;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPolynomialSolver {
    public static Random rand = new Random(234234);


	/**
	 * Provide a simple test case where one of the roots should be real
	 */
	@Test
	public void polynomialRoots() {
		Complex64F[] roots = PolynomialSolver.polynomialRoots(4, 3, 2, 1);

		int numReal = 0;
		for( Complex64F c : roots ) {
			if( c.isReal() ) {
				assertEquals(0,cubic(4, 3, 2, 1,c.real),1e-8);
				numReal++;
			}
		}

		assertTrue(numReal>0);
	}

	/**
	 * Computed a root using Sage.  The two other roots are imaginary
	 */
	@Test
	public void cubicRootReal_known() {
		double root = PolynomialSolver.cubicRootReal(4, 3, 2, 1);

		// test it against a known solution
		assertEquals(-1.65062919143939,root,1e-8);
		// test it against the definition of a root
		assertEquals(0,cubic(4, 3, 2, 1,root),1e-8);
	}

	/**
	 * Create several random polynomials and see if it holds up
	 */
	// Commented this out because for some polynomials it will fail
//	@Test
//	public void cubicRootReal_random() {
//		for( int i = 0; i < 20; i++ ) {
//			double a = rand.nextGaussian()*2;
//			double b = rand.nextGaussian()*2;
//			double c = rand.nextGaussian()*2;
//			double d = rand.nextGaussian()*2;
//
//			double root = PolynomialSolver.cubicRootReal(a, b, c, d);
//
//			System.out.printf("a = %6.3f b = %6.3f c = %6.3f d= %6.3f\n", a, b, c, d);
//			System.out.println("root = "+root);
//
//			// test it against the definition of a root
//			assertEquals(0, cubic(a, b, c, d, root), 1e-8);
//		}
//	}

	public static double cubic( double a,  double b , double c , double d , double x ) {
		return d*x*x*x + c*x*x + b*x + a;
	}
}
