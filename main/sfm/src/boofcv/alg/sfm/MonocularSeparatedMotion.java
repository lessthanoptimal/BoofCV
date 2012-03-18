package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.BundleAdjustmentCalibrated;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.PointPositionPair;
import boofcv.alg.geo.bundle.CalibratedPoseAndPoint;
import boofcv.alg.geo.bundle.ViewPointObservations;
import boofcv.alg.sfm.robust.ModelMatcherTranGivenRot;
import boofcv.factory.geo.FactoryTriangulate;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */

// TODo Handle case where location is never regained but tracks are being dropped too fast
// TODO Make it work with bundle adjustment better
public class MonocularSeparatedMotion<T extends ImageBase> {

	BundleAdjustmentCalibrated bundleAdjustment;
	
	KeyFramePointTracker<T,MultiViewTrack> tracker;
	ModelMatcher<Se3_F64,MultiViewTrack> epipolarMotion;
	TriangulateTwoViewsCalibrated triangulate2 = FactoryTriangulate.twoGeometric();

	ModelMatcherTranGivenRot estimateTran;


	List<MultiViewTrack> inliersTriangulated = new ArrayList<MultiViewTrack>();
	
	// what is considered a large reprojection error
	double largeReprojection;
	
	double pixelMotionThreshold;
	double triangulateAngle;

	// transform from the world frame to 'start'
	Se3_F64 worldToStart = new Se3_F64();
	// transform from 'start' to the latest keyframe
	Se3_F64 startToKey = new Se3_F64();

	boolean rotationOnlyUpdate;
	boolean fatalError;
	
	int minTracks;
	int numUpdateSkip;
	
	boolean hasLocation = false;
	boolean hasFirstSpawn = false;
	boolean lostScale = false;

	public MonocularSeparatedMotion(ImagePointTracker<T> tracker,
									PointTransform_F64 pixelToNormalized,
									ModelMatcher<Se3_F64, AssociatedPair> epipolarMotion,
									ModelMatcherTranGivenRot estimateTran,
									BundleAdjustmentCalibrated bundleAdjustment ,
									double largeReprojection, int minTracks,
									double pixelMotionThreshold, double triangulateAngle)
	{
		this.tracker = new KeyFramePointTracker<T,MultiViewTrack>(tracker,pixelToNormalized,MultiViewTrack.class);;
		this.epipolarMotion = (ModelMatcher)epipolarMotion;
		this.estimateTran = estimateTran;
		this.bundleAdjustment = bundleAdjustment;
		this.largeReprojection = largeReprojection;
		this.minTracks = minTracks;
		this.pixelMotionThreshold = pixelMotionThreshold;
		this.triangulateAngle = triangulateAngle;
	}

	public void reset() {
		worldToStart.reset();
		startToKey.reset();
		numUpdateSkip = 0;
		fatalError = false;
		hasLocation = false;
		hasFirstSpawn = false;
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
		if( hasLocation ) {
			if( !rotationOnlyUpdate )
				performBundleAdjustment();
			spawnTracks();
		}
		tracker.setKeyFrame();
		debugCrap();
		
		return true;
	}

	public void debugCrap() {
		int numTracksWithObs = 0;
		int numTriangulated = 0;
		for( MultiViewTrack t : tracker.getPairs() ) {
			if( t.views.size() > 0 )
				numTracksWithObs++;
			if( t.views.size >= 2 )
				numTriangulated++;
		}

		System.out.printf("\n   total: %4d  triangulated: %4d   observed %4d : Full "+(!rotationOnlyUpdate),
				tracker.getPairs().size(), numTriangulated, numTracksWithObs);
		if( lostScale )
			System.out.println("  LOST SCALE");
		else
			System.out.println();
	}
	
	public void spawnTracks() {
		List<MultiViewTrack> spawned = tracker.spawnTracks();

//		if( !rotationOnlyUpdate ) {
			for( MultiViewTrack t : spawned ) {
				MultiViewTrack.View v = t.views.pop();
				v.o.set(t.currLoc);
				v.worldToView.set(startToKey);
			}
//		}
		hasFirstSpawn = true;
	}
	
	public void estimateMotion() {
		// extract rotation from Essential matrix
		List<MultiViewTrack> inliers = epipolarMotion.getMatchSet();
		Se3_F64 keyToCurr = epipolarMotion.getModel().copy();

		DenseMatrix64F rotationToCurr = new DenseMatrix64F(3,3);
		CommonOps.mult(keyToCurr.getR(), startToKey.getR(),rotationToCurr);

		estimateTran.setRotation(rotationToCurr);
		
		// compute translation from features with valid locations
		List<PointPositionPair> list = new ArrayList<PointPositionPair>();
		inliersTriangulated.clear();
		
		for( MultiViewTrack t : inliers ) {
			if( t.views.size > 1 ) {
				inliersTriangulated.add(t);
				list.add( new PointPositionPair(t.currLoc,t.location));
			}
		}

		// todo if rotation only for a while then assume scale lost
		lostScale = list.size() <= 4;
		if( lostScale ) numUpdateSkip++;

		if( hasLocation && lostScale ) {  // todo handle first frame better
			// no triangulated tracks to update with so scale has been lost
			System.out.println("LOST SCALE!!!");
			handleLostScale();
		} else {
			// estimate and see if it succeeded or not
			if( estimateTran.process(list) ) {
				System.out.println("Updating Position: inlier "+estimateTran.getMatchSet().size()+"  total "+list.size());
				// TODO non-linear refinement

				rotationOnlyUpdate = false;
				keyToCurr.getT().set(estimateTran.getModel());
			} else {
				if( hasLocation ) {
					System.out.println("Updating Rotation Only");
					rotationOnlyUpdate = true;
					keyToCurr.getT().set(0,0,0);
				}
			}
		}

		Se3_F64 temp = new Se3_F64();
		startToKey.concat(keyToCurr,temp);
		startToKey.set(temp);

		if( hasLocation ) {
			// must have the full pose estimate to update the structure
			if( !rotationOnlyUpdate )
				updateStructure();
		} else {
			updateStructure_No_Location();
		}
	}

	private void handleLostScale() {
		hasLocation = false;
		Se3_F64 temp = new Se3_F64();
		worldToStart.concat(startToKey,temp);
		worldToStart.set(temp);
		startToKey.reset();
		for( MultiViewTrack t : tracker.getPairs() ) {
			t.views.reset();
			MultiViewTrack.View v = t.views.pop();
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
			MultiViewTrack.View v = t.views.pop();
			v.o.set(t.currLoc);
			v.worldToView.set(startToKey);
			
			// triangulate using all the views
			if( t.views.size == 2 ) {
				MultiViewTrack.View v0 = t.views.get(0);
				MultiViewTrack.View v1 = t.views.get(1);

				triangulate2.triangulate(v0.o,v1.o,v1.worldToView,t.location);
			} else {
				// TODO N-view triangulate
				MultiViewTrack.View v0 = t.views.get(t.views.size-2);
				MultiViewTrack.View v1 = t.views.get(t.views.size-1);

				triangulate2.triangulate(v0.o,v1.o,v1.worldToView,t.location);
			}
		}
	}

	// TODO detect case where location is lost

	public void updateStructure_No_Location() {
		List<MultiViewTrack> inliers = epipolarMotion.getMatchSet();

		inliersTriangulated.clear();
		
		// tracks which are good to estimate position from
		List<MultiViewTrack> positionTracks = findGoodTriangulate(inliers, startToKey);

		// make sure there are at least 2 tracks good enough to triangulate to accept
		// the initial position
		if( positionTracks.size() >= 2 ) {
			// add the second view to these tracks
			double maxZ = 0;
			for( MultiViewTrack t : positionTracks ) {
				t.location.set(t.candidate);
				MultiViewTrack.View v = t.views.pop();
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
				if( t.views.size != 0 )
					continue;
				MultiViewTrack.View v = t.views.pop();
				v.o.set(t.currLoc);
				v.worldToView.set(startToKey);
			}

			inliersTriangulated.addAll(positionTracks);
			rotationOnlyUpdate = false;
			hasLocation = true;
		} else {
			// discard translation info
			rotationOnlyUpdate = true;
			startToKey.getT().set(0,0,0);
		}
	}

	// todo special unit test for this function
	private List<MultiViewTrack> findGoodTriangulate(List<MultiViewTrack> candidates , Se3_F64 worldToKey ) {
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
			MultiViewTrack.View v0 = t.views.get(t.views.size-1);

			v0.worldToView.invert(inv);
			inv.concat(worldToKey,viewToCurr);

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
			XtoC2.x = -worldToKey.T.x-t.candidate.x;
			XtoC2.y = -worldToKey.T.y-t.candidate.y;
			XtoC2.z = -worldToKey.T.z-t.candidate.z;

			double dot = XtoC1.dot(XtoC2);
			double theta = Math.acos( dot / (XtoC1.norm()*XtoC2.norm()));

			if( theta > triangulateAngle ) {
				// todo what about views without a known point, go over logic again
				// check reprojection errors now
				SePointOps_F64.transform(worldToKey, t.candidate, cw);

				if( cw.z < 0 ){
//					System.out.println("Failed +z second");
					continue;
				}

				double x1 = t.keyLoc.x - t.candidate.x/t.candidate.z;
				double y1 = t.keyLoc.y - t.candidate.y/t.candidate.z;
				double x2 = t.currLoc.x - cw.x/cw.z;
				double y2 = t.currLoc.y - cw.y/cw.z;

				double error = (Math.sqrt(x1*x1 + y1*y1) + Math.sqrt(x2*x2 + y2*y2))/2.0;

				if( error < largeReprojection)
					positionTracks.add(t);
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
		
		return count/(double)tracks.size() > 0.9;
	}

	private void performBundleAdjustment() {
		if( bundleAdjustment == null )
			return;

		CalibratedPoseAndPoint model = new CalibratedPoseAndPoint();
		model.configure(1,inliersTriangulated.size());
		
		model.getWorldToCamera(0).set(startToKey);
		
		ViewPointObservations view = new ViewPointObservations();
		
		for( int i = 0; i < inliersTriangulated.size(); i++ ) {
			MultiViewTrack t = inliersTriangulated.get(i);
			MultiViewTrack.View v = t.views.get(t.views.size-1);
			view.getPoints().pop().set(i,v.o);
			model.getPoint(i).set(t.location);
		}

		List<ViewPointObservations> views = new ArrayList<ViewPointObservations>();
		views.add(view);

		if( bundleAdjustment.process(model,views) ) {
			startToKey.set(model.getWorldToCamera(0));
			for( int i = 0; i < inliersTriangulated.size(); i++ ) {
				MultiViewTrack t = inliersTriangulated.get(i);
				t.location.set(model.getPoint(i));
			}
		}
	}
	
	public Se3_F64 getWorldToKey() {
		Se3_F64 worldToKey = new Se3_F64();
		worldToStart.concat(startToKey,worldToKey);
		return worldToKey;
	}
}
