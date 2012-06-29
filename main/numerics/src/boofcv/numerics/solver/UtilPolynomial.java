package boofcv.numerics.solver;

/**
 * @author Peter Abeles
 */
public class UtilPolynomial {

	/**
	 * Given the coefficients compute the vertex (minimum/maximum) of the quadratic.
	 *
	 * y = a*x<sup>2</sp> + b*x + c
	 *
	 * @param a quadratic coefficient.
	 * @param b quadratic coefficient.
	 * @return The quadratic's vertex.
	 */
	public static double quadraticVertex( double a, double b ) {
		return -b/(2.0*a);
	}
}
