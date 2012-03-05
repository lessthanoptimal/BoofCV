package boofcv.geo;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.sfm.MonocularVisualOdometry;
import boofcv.struct.distort.PointTransform_F64;

/**
 * @author Peter Abeles
 */
public abstract class EvaluateMonoVisualOdometry {


	public abstract MonocularVisualOdometry<?> createAlg( ImagePointTracker<?> tracker ,
														  PointTransform_F64 pixelToNormalized );

	public void step() {

	}

	public void computeStatistics() {

	}

}
