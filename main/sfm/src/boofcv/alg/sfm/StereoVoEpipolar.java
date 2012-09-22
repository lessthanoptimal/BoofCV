package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.AssociatedPair;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.feature.ComputePixelTo3D;
import boofcv.struct.image.ImageBase;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class StereoVoEpipolar<T extends ImageBase> {

	int MIN_TRACKS = 100;

	// tracks features in the image
	private KeyFramePointTracker<T,PointPoseTrack> tracker;
	// used to estimate a feature's 3D position from image range data
	private ComputePixelTo3D<T> pixelTo3D;

	// triangulate feature's 3D location
	private TriangulateTwoViewsCalibrated triangulate =
			FactoryTriangulate.twoGeometric();

	// estimate the camera motion up to a scale factor from two sets of point correspondences
	private ModelMatcher<Se3_F64, AssociatedPair> motionEstimator;

	Se3_F64 keyToWorld = new Se3_F64();
	Se3_F64 currToKey = new Se3_F64();

	int motionFailed;

	public StereoVoEpipolar(int MIN_TRACKS, ModelMatcher<Se3_F64, AssociatedPair> motionEstimator,
							ComputePixelTo3D<T> pixelTo3D,
							KeyFramePointTracker<T, PointPoseTrack> tracker,
							TriangulateTwoViewsCalibrated triangulate)
	{
		this.MIN_TRACKS = MIN_TRACKS;
		this.motionEstimator = motionEstimator;
		this.pixelTo3D = pixelTo3D;
		this.tracker = tracker;
		this.triangulate = triangulate;
	}

	public void reset() {
		tracker.reset();
		keyToWorld.reset();
		currToKey.reset();
		motionFailed = 0;
	}

	public boolean process( T leftImage , T rightImage ) {
		tracker.process(leftImage);

		boolean foundMotion = estimateMotion();

		if( !foundMotion ) {
			System.out.println("MOTION FAILED!");
			motionFailed++;
		}

		if( tracker.getActiveTracks().size() < MIN_TRACKS || !foundMotion ) {
			System.out.println("----------- CHANGE KEY FRAME ---------------");
			concatMotion();
			pixelTo3D.setImages(leftImage,rightImage);

			tracker.setKeyFrame();
			tracker.spawnTracks();

			List<PointPoseTrack> tracks = tracker.getPairs();
			List<PointPoseTrack> drop = new ArrayList<PointPoseTrack>();

			// estimate 3D coordinate using stereo vision
			for( PointPoseTrack p : tracks ) {
				Point2D_F64 pixel = p.getPixel().keyLoc;
				if( !pixelTo3D.process(pixel.x,pixel.y) ) {
					drop.add(p);
				} else {
					p.getLocation().set( pixelTo3D.getX() , pixelTo3D.getY(), pixelTo3D.getZ());
				}
			}

			// drop tracks which couldn't be triangulated
			for( PointPoseTrack p : drop ) {
				tracker.dropTrack(p);
			}

			return foundMotion;
		} else {
			return true;

		}
	}

	private boolean estimateMotion() {

		// estimate the motion up to a scale factor in translation
		if( !motionEstimator.process( (List)tracker.getPairs()) )
			return false;

		// TODO only update motion if there has been enough motion

		// TODO add non-linear refinement
		currToKey.set(motionEstimator.getModel());

		// estimate the scale factor using previously triangulated point locations
		int N = motionEstimator.getMatchSet().size();
		double ratio[] = new double[N];
		for( int i = 0; i < N; i++ ) {
			PointPoseTrack t = tracker.getPairs().get( motionEstimator.getInputIndex(i) );

			Point3D_F64 P = t.getLocation();
			double origZ = P.z;

			triangulate.triangulate(t.keyLoc,t.currLoc,currToKey,P);
			ratio[i] = P.z/origZ;
		}

		Arrays.sort(ratio);

		double scale = ratio[ ratio.length/2 ];

//		System.out.print("  Before T = "+currToKey.getT().norm());

		// correct the scale factors
		GeometryMath_F64.scale(currToKey.getT(),scale);

//		System.out.println("  After T = "+currToKey.getT().norm()+"  scale = "+scale);

		// TODO update point location if good geometry
//		for( int i = 0; i < N; i++ ) {
//			PointPoseTrack t = tracker.getPairs().get( motionEstimator.getInputIndex(i) );
//			GeometryMath_F64.scale(t.getLocation(),scale);
//		}

		return true;
	}

	private void concatMotion() {
		Se3_F64 temp = new Se3_F64();
		currToKey.concat(keyToWorld,temp);
		keyToWorld.set(temp);
		currToKey.reset();
	}

	public Se3_F64 getCurrToWorld() {
		Se3_F64 currToWorld = new Se3_F64();
		currToKey.concat(keyToWorld,currToWorld);
		return currToWorld;
	}

	public KeyFramePointTracker<T, PointPoseTrack> getTracker() {
		return tracker;
	}

	public ModelMatcher<Se3_F64, AssociatedPair> getMotionEstimator() {
		return motionEstimator;
	}
}
