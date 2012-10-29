package boofcv.alg.sfm;

import georegression.struct.point.Point2D_F64;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class TrackGaussianCheck implements TrackDistributionCheck
{
	double threshold;

	double origLarge;
	double origSmall;

	double sigmaLarge;
	double sigmaSmall;

	public TrackGaussianCheck(double threshold) {
		this.threshold = threshold;
	}

	@Override
	public void configure(int imageWidth, int imageHeight) {

	}

	@Override
	public void setInitialLocation(List<Point2D_F64> tracks) {
		computeDistribution(tracks);

		origLarge = sigmaLarge;
		origSmall = sigmaSmall;
	}

	private void computeDistribution(List<Point2D_F64> tracks) {
		double meanX = 0;
		double meanY = 0;

		int N = tracks.size();

		for( Point2D_F64 t : tracks ) {
			meanX += t.getX();
			meanY += t.getY();
		}
		meanX /= N;
		meanY /= N;

		double sxx = 0;
		double sxy = 0;
		double syy = 0;

		for( Point2D_F64 t : tracks ) {
			double dx = t.x - meanX;
			double dy = t.y - meanY;

			sxx += dx*dx;
			sxy += dx*dy;
			syy += dy*dy;
		}

		sxx = Math.sqrt(sxx/N);
		sxy = Math.sqrt(sxy/N);
		syy = Math.sqrt(syy/N);

		double t = sxx+syy;
		double right = Math.sqrt(t*t/4.0 - syy);
		sigmaLarge = t/2.0 + right;
		sigmaSmall = t/2.0 - right;
	}

	@Override
	public boolean checkDistribution(List<Point2D_F64> tracks) {
		computeDistribution(tracks);

		System.out.println("  distribution ratio = "+(origLarge-sigmaLarge)/sigmaLarge);

		return (origLarge-sigmaLarge)/sigmaLarge > threshold ||
				(origSmall-sigmaSmall)/sigmaSmall > threshold;
	}
}
