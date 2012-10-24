package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.PointPosePair;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Visual odometry where a ranging device is assumed for pixels in the primary view.  Typical
 * inputs would include a stereo or depth camera.
 *
 * @author Peter Abeles
 */

// TODO Use the mode instead of median to resolve scale ambiguity

public class PixelDepthVoEpipolar<T extends ImageBase> {
	// TODO Make relative to the last update or remove?
	double MIN_PIXEL_CHANGE = 100;

	double TOL_TRIANGULATE = 2*Math.PI/180.0;

	int MIN_TRACKS = 100;

	// tracks features in the image
	private KeyFramePointTracker<T,PointPoseTrack> tracker;
	// used to estimate a feature's 3D position from image range data
	private ImagePixelTo3D pixelTo3D;

	// triangulate feature's 3D location
	private TriangulateTwoViewsCalibrated triangulate =
			FactoryTriangulate.twoGeometric();

	// estimate the camera motion up to a scale factor from two sets of point correspondences
	private ModelMatcher<Se3_F64, PointPosePair> motionEstimator;

	ComputeObservationAcuteAngle computeObsAngle = new ComputeObservationAcuteAngle();

	Se3_F64 keyToWorld = new Se3_F64();
	Se3_F64 currToKey = new Se3_F64();
	Se3_F64 currToWorld = new Se3_F64();

	int numTracksUsed;

	boolean hasSignificantChange;

	int motionFailed;

	boolean motionEstimated;

	public PixelDepthVoEpipolar(int MIN_TRACKS, ModelMatcher<Se3_F64, PointPosePair> motionEstimator,
								ImagePixelTo3D pixelTo3D,
								KeyFramePointTracker<T, PointPoseTrack> tracker,
								TriangulateTwoViewsCalibrated triangulate)
	{
		this.MIN_TRACKS = MIN_TRACKS;
		this.motionEstimator = motionEstimator;
		this.pixelTo3D = pixelTo3D;
		this.tracker = tracker;
		this.triangulate = triangulate;
	}

	public void setCalibration( StereoParameters config ) {

	}

	public void reset() {
		tracker.reset();
		keyToWorld.reset();
		currToKey.reset();
		motionFailed = 0;
	}

	// TODO indicate FULL_MOTION, ANGLE_ONLY,NO_MOTION,FAULT
	public boolean process( T leftImage ) {
		tracker.process(leftImage);

		boolean foundMotion = estimateMotion();

		if( !foundMotion ) {
//			System.out.println("MOTION FAILED!");
			motionFailed++;
		}

//		System.out.println(" numTracksUsed = "+numTracksUsed);
		if( numTracksUsed < MIN_TRACKS ) {
			pixelTo3D.initialize();

			System.out.println("----------- CHANGE KEY FRAME ---------------");
			concatMotion();

			tracker.setKeyFrame();
			tracker.spawnTracks();

			List<PointPoseTrack> tracks = tracker.getPairs();
			List<PointPoseTrack> drop = new ArrayList<PointPoseTrack>();

			// estimate 3D coordinate using stereo vision
			for( PointPoseTrack p : tracks ) {
				Point2D_F64 pixel = p.getPixel().keyLoc;
				// discard point if it can't triangulate
				if( !pixelTo3D.process(pixel.x,pixel.y) || pixelTo3D.getW() == 0 ) {
					drop.add(p);
				} else {
					double w = pixelTo3D.getW();
					p.getLocation().set( pixelTo3D.getX()/w , pixelTo3D.getY()/w, pixelTo3D.getZ()/w);

//					System.out.println("Stereo z = "+p.getLocation().getZ());
//					if( p.getLocation().z < 100 )
//						System.out.println("   * ");
				}
			}

			// drop tracks which couldn't be triangulated
			for( PointPoseTrack p : drop ) {
				tracker.dropTrack(p);
			}

			hasSignificantChange = false;
			motionEstimated = false;
			return foundMotion;
		} else {
			return foundMotion;
		}
	}

	private boolean estimateMotion() {

		motionEstimated = false;

		if( tracker.getPairs().size() <= 0 )
			return false;

		// only estimate camera motion if there is a chance of the solution not being noise
		if( !hasSignificantChange  ) {
			if( checkSignificantMotion() )
				hasSignificantChange = true;
			else {
				numTracksUsed = tracker.getPairs().size();
				return false;
			}
		}

		List<PointPosePair> obs = new ArrayList<PointPosePair>();

		for( PointPoseTrack t : tracker.getPairs() ) {
			PointPosePair p = new PointPosePair();

			p.location = t.getLocation();
			p.observed = t.currLoc;

			obs.add(p);
		}

		// estimate the motion up to a scale factor in translation
		if( !motionEstimator.process( obs ) )
			return false;

		// TODO add non-linear refinement

		motionEstimator.getModel().invert(currToKey);

		// update feature locations using triangulation
//		computeObsAngle.setFromAtoB(currToKey);
//		for( PointPoseTrack t : tracker.getPairs() ) {
//			if( computeObsAngle.computeAcuteAngle(t.currLoc,t.keyLoc) >= TOL_TRIANGULATE ) {
//				triangulate.triangulate(t.currLoc,t.keyLoc,currToKey,t.location);
//			}
//		}

		numTracksUsed = motionEstimator.getMatchSet().size();
		motionEstimated = true;

		return true;
	}

	private boolean checkSignificantMotion() {
		List<PointPoseTrack> tracks = tracker.getPairs();

		int numOver = 0;

		for( int i = 0; i < tracks.size(); i++ ) {
			AssociatedPair p = tracks.get(i).getPixel();

			if( p.keyLoc.distance2(p.currLoc) > MIN_PIXEL_CHANGE )
				numOver++;
		}
		return numOver >= tracks.size()/2;
	}

	private void concatMotion() {
		Se3_F64 temp = new Se3_F64();
		currToKey.concat(keyToWorld,temp);
		keyToWorld.set(temp);
		currToKey.reset();
	}

	public Se3_F64 getCurrToWorld() {
		currToKey.concat(keyToWorld,currToWorld);
		return currToWorld;
	}

	public KeyFramePointTracker<T, PointPoseTrack> getTracker() {
		return tracker;
	}

	public ModelMatcher<Se3_F64, PointPosePair> getMotionEstimator() {
		return motionEstimator;
	}

	public boolean isMotionEstimated() {
		return motionEstimated;
	}
}
