package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.RefinePerspectiveNPoint;
import boofcv.abst.geo.TriangulateNViewsCalibrated;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.PointPositionPair;
import boofcv.alg.sfm.robust.ModelMatcherTranGivenRot;
import boofcv.factory.geo.FactoryEpipolar;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Estimates camera motion up to a scale factor using a single calibrated camera.  The algorithm
 * is heavily inspired from [1]. but modified to handle a more narrow field of view.  Rotational
 * and translational components of motion are estimated separately, hence the class name.  A history
 * of observations is stored for each feature allowing more accurate triangulation.  The rotational
 * component is always computed and compounded, even if the translational component
 * cannot be computed.  The translational component is only computed when there are features available
 * which have been triangulated.  Motion is only estimated between key frames.
 * </p>
 *
 * <p>
 * High level algorithm summary, see code for details:
 * <ol>
 * <li> Select a key frame of X percent of image features have moved more than Y pixels.</li>
 * <li> Using RANSAC, estimate the camera motion between the two most recent key frames.</li>
 * <li> If first key frame, triangulate feature locations.</li>
 * <li> Compound rotational component of motion </li>
 * <li> Estimate translational component by assuming rotation is known and using previously
 *      computed landmark locations.</li>
 * <li> Update landmark locations of there the acute angle is sufficiently large to accurately
 *      triangulate </li>
 * </ol>
 * </p>
 *
 * <p>
 * [1] Tardif, J.-P., Pavlidis, Y., and Daniilidis, K. "Monocular visual odometry in urban
 * environments using an omnidirectional camera," IROS 2008
 * </p>
 *
 * @author Peter Abeles
 */
// TODO It appears that triangulation and pose estimation slowly degrade and every recover
//      This can be seen in how the translation inlier count drops, despite there being a lot
//      of triangulated points.  Might need to add a reset to prevent total failure
// TODO There are magic numbers spread throughout the code.  Remove those once debugging was finished
public class MonocularSeparatedMotion<T extends ImageBase> {

	// refines the full pose estimate
	private RefinePerspectiveNPoint refinePose = FactoryEpipolar.refinePnP(1e-20,300);

	// tracks point features
	private KeyFramePointTracker<T,MultiViewTrack> tracker;
	// robustly estimates the motion between two key frames
	private ModelMatcher<Se3_F64,MultiViewTrack> epipolarMotion;
	// triangulation used when there are only two observations
	private TriangulateTwoViewsCalibrated triangulate2 = FactoryTriangulate.twoGeometric();
	// triangulation used when there are N observations
	private TriangulateNViewsCalibrated triangulateN = FactoryTriangulate.nDLT();

	// robustly estimates translation given the rotation
	private ModelMatcherTranGivenRot estimateTran;
	
	// what is considered a large reprojection error
	private double largeReprojection;

	// threshold used to select key frames
	private double pixelMotionThreshold;
	// threshold used to select which features can be triangulated.  acute angle in radians
	private double triangulateAngle;

	// transform from the world frame to 'start'
	private Se3_F64 worldToStart = new Se3_F64();
	// transform from 'start' to the latest keyframe
	private Se3_F64 startToKey = new Se3_F64();

	// was only the rotational component updated in the past cycle
	private boolean rotationOnlyUpdate;

	// number of no orientation updates in a row
	private int numNoOrientation;

	// has the location ever been estimated
	private boolean hasLocation = false;
	// has the scale factor been lost?
	private boolean lostScale = false;

	// number of ticks
	private long tick = 0;
	
	public MonocularSeparatedMotion(ImagePointTracker<T> tracker,
									PointTransform_F64 pixelToNormalized,
									ModelMatcher<Se3_F64, AssociatedPair> epipolarMotion,
									ModelMatcherTranGivenRot estimateTran,
									double largeReprojection,
									double pixelMotionThreshold, double triangulateAngle)
	{
		this.tracker = new KeyFramePointTracker<T,MultiViewTrack>(tracker,pixelToNormalized,MultiViewTrack.class);;
		this.epipolarMotion = (ModelMatcher)epipolarMotion;
		this.estimateTran = estimateTran;
		this.largeReprojection = largeReprojection;
		this.pixelMotionThreshold = pixelMotionThreshold;
		this.triangulateAngle = triangulateAngle;
	}

	public void reset() {
		worldToStart.reset();
		startToKey.reset();
		numNoOrientation = 0;
		tick = 0;
		hasLocation = false;
		tracker.reset();
		spawnTracks();
	}
	
	public boolean process( T image )
	{
		tracker.process(image);

		// see if sufficient change in pixel location
		if( !sufficientMotion() ) {
			if( tracker.getActiveTracks().size() == 0 ) {
				reset();
			}
			return false;
		}

		//  Estimate motion between keyframe and the current frame
		epipolarMotion.process(tracker.getPairs());

		estimateMotion();
		
//		List<MultiViewTrack>  tracks = tracker.getPairs();
//		for( int i = 0; i < tracks.size(); )  {
//			MultiViewTrack t = tracks.get(i);
//
//			if( t.views.size <= 1 && tick - t.whenSpawned >= 8 ) {
////				System.out.println("Dropping useless track");
//				tracker.dropTrack(t);
//			} else {
//				i++;
//			}
//		}
		
		if( hasLocation ) {
			if( rotationOnlyUpdate ) {
				if( numNoOrientation++ > 2 ) {
					System.out.println(" REALLY has lost scale.");
					hasLocation = false;
					Se3_F64 temp = new Se3_F64();
					worldToStart.concat(startToKey,temp);
					worldToStart.set(temp);
					startToKey.reset();
					tracker.reset();
					spawnTracks();
				}
				
			} else {
				spawnTracks();
			}
		}

		tracker.setKeyFrame();
		debugCrap();
		tick++;
		
		return true;
	}

	public void debugCrap() {
		int numTracksWithObs = 0;
		int numTriangulated = 0;
		for( MultiViewTrack t : tracker.getPairs() ) {
			if( t.views.size() > 0 )
				numTracksWithObs++;
			if( t.views.size() >= 2 )
				numTriangulated++;
		}

		System.out.printf("\n   total: %4d  triangulated: %4d   observed %4d : Full "+(!rotationOnlyUpdate),
				tracker.getPairs().size(), numTriangulated, numTracksWithObs);
		if( lostScale )
			System.out.println("  LOST SCALE");
		else
			System.out.println();
	}

	/**
	 * Creates and adds a new observations of the tracked feature
	 */
	protected MultiViewTrack.View popView( MultiViewTrack t ) {
		MultiViewTrack.View v = new MultiViewTrack.View();
		v.whenViewed = tick;
		t.views.add(v);
		return v;
	}

	/**
	 * Requests that the tracker spawns new tracks and adds an observation
	 */
	public void spawnTracks() {
		List<MultiViewTrack> spawned = tracker.spawnTracks();

//		if( !rotationOnlyUpdate ) {
			for( MultiViewTrack t : spawned ) {
				t.whenSpawned = tick;
				MultiViewTrack.View v = popView(t);
				v.o.set(t.currLoc);
				v.worldToView.set(startToKey);
			}
//		}
		System.out.println("  Spawned Tracks "+spawned.size());
	}

	/**
	 * Estimates the cameras motion.  The motion is estimated between the current frame
	 * and the previous key frame. The rotational component is compounded to the current
	 * motion estimate and translation estimated separately.
	 */
	public void estimateMotion() {
		// extract rotation from Essential matrix
		List<MultiViewTrack> inliers = epipolarMotion.getMatchSet();
		Se3_F64 keyToCurr = epipolarMotion.getModel().copy();

		DenseMatrix64F rotationToCurr = new DenseMatrix64F(3,3);
		CommonOps.mult(keyToCurr.getR(), startToKey.getR(),rotationToCurr);

		estimateTran.setRotation(rotationToCurr);
		
		// compute translation from features with valid locations
		List<PointPositionPair> list = new ArrayList<PointPositionPair>();
		
		for( MultiViewTrack t : inliers ) {
			if( t.views.size() > 1 ) {
				list.add( new PointPositionPair(t.currLoc,t.location));
			}
		}

		// todo if rotation only for a while then assume scale lost
		lostScale = list.size() <= 5;

		if( hasLocation && lostScale ) {  // todo handle first frame better
			// no triangulated tracks to update with so scale has been lost
			System.out.println("LOST SCALE!!!");
			handleLostScale();
			startToKey.getR().set(rotationToCurr);
		} else {
			// estimate and see if it succeeded or not
			if( estimateTran.process(list) ) {
				System.out.println("Updating Position: inlier "+estimateTran.getMatchSet().size()+"  total "+list.size());

				rotationOnlyUpdate = false;

				startToKey.getR().set(rotationToCurr);
				startToKey.getT().set(estimateTran.getModel());

				// non-linear refinement of rotation and translation
				refinePose.process(startToKey,estimateTran.getMatchSet());
				startToKey.getT().set(refinePose.getRefinement().getT());
//				startToKey.set(refinePose.getRefinement());
				
			} else {
				if( hasLocation ) {
					System.out.println("Updating Rotation Only");
					rotationOnlyUpdate = true;
					keyToCurr.getT().set(0,0,0);
				}
				Se3_F64 temp = new Se3_F64();
				startToKey.concat(keyToCurr,temp);
				startToKey.set(temp);
			}
		}

		if( hasLocation ) {
			// must have the full pose estimate to update the structure
			if( !rotationOnlyUpdate )
				updateStructure();
		} else {
			updateStructure_No_Location();
		}
	}

	/**
	 * The scale has been lost, Concat the current motion estimate and make the current frame the start frame.
	 * Discard previous views of the feature and start over again.
	 */
	private void handleLostScale() {
		hasLocation = false;
		Se3_F64 temp = new Se3_F64();
		worldToStart.concat(startToKey,temp);
		worldToStart.set(temp);
		startToKey.reset();
		for( MultiViewTrack t : tracker.getPairs() ) {
			t.views.clear();
			MultiViewTrack.View v = popView(t);
			v.o.set(t.keyLoc);
			v.worldToView.set(startToKey);
		}
	}

	
	public void updateStructure() {
		List<MultiViewTrack> inliers = epipolarMotion.getMatchSet();
		
		// tracks which are good to estimate position from
		List<MultiViewTrack> positionTracks = findGoodTriangulate(inliers, startToKey);

		for( MultiViewTrack t : positionTracks ) {
			// add the current view
			MultiViewTrack.View v = popView(t);
			v.o.set(t.currLoc);
			v.worldToView.set(startToKey);
			
			triangulatePoint(t);
		}
	}

	// find tracks which can be triangulated well from these two observations
	Se3_F64 inv = new Se3_F64();
	Se3_F64 V1ToV2 = new Se3_F64();
	
	private void triangulatePoint( MultiViewTrack t) 
	{
		// triangulate point
		if( t.views.size() == 2 ) {
			MultiViewTrack.View v0 = t.views.get(0);
			MultiViewTrack.View v1 = t.views.get(t.views.size()-1);

			// transform from first to second view
			v0.worldToView.invert(inv);
			v1.worldToView.concat(inv, V1ToV2);

			triangulate2.triangulate(v0.o,v1.o, V1ToV2,t.location);

			// convert from being in v0's perspective
			SePointOps_F64.transformReverse(v0.worldToView, t.location, t.location);
		} else {
			List<Point2D_F64> obs = new ArrayList<Point2D_F64>();
			List<Se3_F64> where = new ArrayList<Se3_F64>();
			
			for( int i = 0; i < t.views.size(); i++ ) {
				obs.add( t.views.get(i).o );
				where.add( t.views.get(i).worldToView );
			}
			
			triangulateN.triangulate(obs,where,t.location);

//				System.out.println("  num views "+t.views.size);
		}
	}

	/**
	 * Attempts to estimate the cameras translational motion for the first time.  Gets a list of
	 * features which are geometrically good enough to triangulate and triangulates their location.
	 * The scale factor is then normalized using the downrange of the farthest feature.
	 */
	public void updateStructure_No_Location() {
		List<MultiViewTrack> inliers = epipolarMotion.getMatchSet();
		
		// tracks which are good to estimate position from
		List<MultiViewTrack> positionTracks = findGoodTriangulate(inliers, startToKey);

		// make sure there are at least 2 tracks good enough to triangulate to accept
		// the initial position
		if( positionTracks.size() >= 2 ) {
			// add the second view to these tracks
			double maxZ = 0;
			for( MultiViewTrack t : positionTracks ) {
				t.location.set(t.candidate);
				MultiViewTrack.View v = popView(t);
				v.o.set(t.currLoc);
				v.worldToView.set(startToKey);

				if( maxZ < t.location.z )
					maxZ = t.location.z;
			}

			// rescale the points and translation based on the max distance, an attempt to keep
			// everything within a reasonable scale factor
			for( MultiViewTrack t : positionTracks ) {
				t.location.x /= maxZ;
				t.location.y /= maxZ;
				t.location.z /= maxZ;
			}
			startToKey.T.x /= maxZ;
			startToKey.T.y /= maxZ;
			startToKey.T.z /= maxZ;

			// add a first observation to all tracks without observations now that the location has been established
			for( MultiViewTrack t : tracker.getPairs() ) { // todo is this needed?
				if( t.views.size() != 0 )
					continue;
				MultiViewTrack.View v = popView(t);
				v.o.set(t.currLoc);
				v.worldToView.set(startToKey);
			}

			rotationOnlyUpdate = false;
			hasLocation = true;
		} else {
			// discard translation info
			rotationOnlyUpdate = true;
			startToKey.getT().set(0,0,0);
		}
	}

	/**
	 * Creates a list of features which can be triangulated well.  A feature is declared as being good to
	 * triangulate if the acute angle between its triangulated position and the two camera centers is
	 * larger than a threshold.    The current camera center and the camera center of the most recently saved
	 * observation are used.
	 */
	private List<MultiViewTrack> findGoodTriangulate(List<MultiViewTrack> candidates , Se3_F64 worldToCurr ) {
		List<MultiViewTrack> positionTracks = new ArrayList<MultiViewTrack>();

		// observation direction in world coordinates
		Point3D_F64 cw = new Point3D_F64();

		Vector3D_F64 XtoC1 = new Vector3D_F64();
		Vector3D_F64 XtoC2 = new Vector3D_F64();

		// find tracks which can be triangulated well from these two observations
		Se3_F64 inv = new Se3_F64();
		Se3_F64 viewToCurr = new Se3_F64();

		for( MultiViewTrack t : candidates ) {
			// skip tracks with no observations
			if( t.views.size() <= 0 )
				continue;

			// compare to most recent view to ensure it is not too far away now, when it might not
			// have been in the past
			MultiViewTrack.View v0 = t.views.get(t.views.size()-1);

			v0.worldToView.invert(inv);
			worldToCurr.concat(inv, viewToCurr);

			// triangulate the view
			triangulate2.triangulate(v0.o,t.currLoc,viewToCurr,t.candidate);

			// can't be behind the camera in the first view
			if( t.candidate.z < 0 ) {
//				System.out.println("Failed +z first");
				continue;
			}
			
			// vector from point to first view camera center
			XtoC1.x = -t.candidate.x;
			XtoC1.y = -t.candidate.y;
			XtoC1.z = -t.candidate.z;

			// vector from point to second view camera center
			XtoC2.x = -viewToCurr.T.x-t.candidate.x;
			XtoC2.y = -viewToCurr.T.y-t.candidate.y;
			XtoC2.z = -viewToCurr.T.z-t.candidate.z;

			double dot = XtoC1.dot(XtoC2);
			double theta = Math.acos( dot / (XtoC1.norm()*XtoC2.norm()));

			if( theta >= triangulateAngle ) {
				// check reprojection errors now
				SePointOps_F64.transform(viewToCurr, t.candidate, cw);

				if( cw.z < 0 ){
//					System.out.println("Failed +z second");
					continue;
				}

				double x1 = v0.o.x - t.candidate.x/t.candidate.z;
				double y1 = v0.o.y - t.candidate.y/t.candidate.z;
				double x2 = t.currLoc.x - cw.x/cw.z;
				double y2 = t.currLoc.y - cw.y/cw.z;

				double error = (Math.sqrt(x1*x1 + y1*y1) + Math.sqrt(x2*x2 + y2*y2))/2.0;

				if( error < largeReprojection) {
					positionTracks.add(t);
				}
//				else
//					System.out.println("Failed large error: " + error);
			} else {
//				System.out.println("Failed theta "+theta);
			}
		}

		System.out.println("   Total good triangulation features: "+positionTracks.size());
		return positionTracks;
	}

	/**
	 * Checks if most of the tracks have moved sufficiently since the key frame
	 */
	private boolean sufficientMotion() {
		List<MultiViewTrack> tracks = tracker.getPairs();

		double thresh = pixelMotionThreshold*pixelMotionThreshold;

		int count = 0;
		for( MultiViewTrack t : tracks ) {
			AssociatedPair p = t.getPixel();
			double d = p.currLoc.distance2(p.keyLoc);
			if( d >= thresh )
				count++;
		}
		
		return count/(double)tracks.size() > 0.7;
	}

	public Se3_F64 getWorldToKey() {
		Se3_F64 worldToKey = new Se3_F64();
		worldToStart.concat(startToKey,worldToKey);
		return worldToKey;
	}

	public KeyFramePointTracker<T, MultiViewTrack> getTracker() {
		return tracker;
	}
}
