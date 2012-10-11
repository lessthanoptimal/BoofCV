package boofcv.alg.geo;

import georegression.struct.point.Point2D_F64;

/**
 * Simple function for converting error in normalized image coordinates to pixels using
 * intrinsic camera parameters. Better to use tested code than cut and pasting.
 *
 * @author Peter Abeles
 */
public class NormalizedToPixelError {
	private double fx; // focal length x
	private double fy; // focal length y
	private double skew; // pixel skew

	public NormalizedToPixelError(double fx, double fy, double skew) {
		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
	}

	public double errorSq( Point2D_F64 a , Point2D_F64 b ) {
		double dy = (b.y - a.y);
		double dx = (b.x - a.x)*fx + dy*skew;
		dy *= fy;

		return dx*dx + dy*dy;
	}

	public double errorSq( double a_x , double a_y , double b_x , double b_y ) {
		double dy = (b_y - a_y);
		double dx = (b_x - a_x)*fx + dy*skew;
		dy *= fy;

		return dx*dx + dy*dy;
	}
}
