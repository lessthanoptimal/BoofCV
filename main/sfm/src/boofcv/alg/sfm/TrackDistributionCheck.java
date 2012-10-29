package boofcv.alg.sfm;

import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * @author Peter Abeles
 */
public interface TrackDistributionCheck {

	void configure( int imageWidth , int imageHeight );

	public void setInitialLocation(List<Point2D_F64> tracks);

	public boolean checkDistribution( List<Point2D_F64> tracks );
}
