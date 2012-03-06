package boofcv.geo.simulation.mono;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.sfm.MonocularVisualOdometry;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.struct.distort.PointTransform_F64;

/**
 * @author Peter Abeles
 */
public class EvaluateSimpleMono extends EvaluateMonoVisualOdometry {
	


	@Override
	public MonocularVisualOdometry<?> createAlg(ImagePointTracker<?> tracker, 
												PointTransform_F64 pixelToNormalized) 
	{
		int minTracks = targetTracks/3;

		return FactoryVisualOdometry.monoSimple(minTracks,3,1e-3,tracker,pixelToNormalized);
	}

	public static void main( String args[] ) {
		EvaluateSimpleMono target = new EvaluateSimpleMono();
		StandardMonoScenarios scenarios = new StandardMonoScenarios(target);
		
		scenarios.forwardScenario(0);
		
		System.out.println("Done");
	}
}
