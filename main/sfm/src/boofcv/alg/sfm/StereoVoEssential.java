package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.AssociatedPair;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.feature.ComputePixelTo3D;
import boofcv.struct.image.ImageBase;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class StereoVoEssential <T extends ImageBase> {

	int MIN_TRACKS = 100;

	// tracks features in the image
	private KeyFramePointTracker<T,PointPoseTrack> tracker;
	// used to estimate a feature's 3D position from image range data
	private ComputePixelTo3D<T> pixelTo3D;

	// list of tracks that have been triangulated, for debugging only
	private List<PointPoseTrack> triangulated = new ArrayList<PointPoseTrack>();

	// triangulate feature's 3D location
	private TriangulateTwoViewsCalibrated triangulate =
			FactoryTriangulate.twoGeometric();

	// estimate the camera motion up to a scale factor from two sets of point correspondences
	private ModelMatcher<Se3_F64, AssociatedPair> motionEstimator;

	boolean forcedReset = true;

	Se3_F64 keyToWorld = new Se3_F64();
	Se3_F64 currToKey = new Se3_F64();

//	public boolean process( T leftImage , T rightImage ) {
//		tracker.process(leftImage);
//
//
//
//		if( tracker.getActiveTracks().size() < MIN_TRACKS ) {
//
//		}
//
//
//	}
//
//	private boolean estimateMotion() {
//
//		// estimate the motion up to a scale factor in translation
//		if( !motionEstimator.process( (List<AssociatedPair>)tracker.getPairs()) )
//			return false;
//
//		currToKey.set(motionEstimator.getModel());
//
//		// estimate the scale factor using previously triangulated point locations
//		List<>
//
//		return true;
//	}
}
