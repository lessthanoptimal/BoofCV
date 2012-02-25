package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.epipolar.RefineEpipolarMatrix;
import boofcv.abst.geo.epipolar.RefinePerspectiveNPoint;
import boofcv.abst.geo.epipolar.TriangulateTwoViewsCalibrated;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.PointPositionPair;
import boofcv.alg.geo.epipolar.DecomposeEssential;
import boofcv.alg.geo.epipolar.PositiveDepthConstraintCheck;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.FastQueue;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageBase;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import pja.sorting.QuickSelectArray;

import java.util.List;

/**
 * Very basic and straight forward visual odometry algorithm. Because of its simplicity it is more
 * prone to errors caused by a single event.  A high level summary of each processing step is shown
 * below.  Key frames are identified based on the number of frames processed and track motion.  Fixed position
 * updates are performed at key frames while temporary ones are done as each frame is processed to provide
 * a continuous update.  Updating motion from two consecutive frames with little motion greatly increases
 * the amount of noise.
 *
 * <ol>
 * <li> Compute Essential matrix. </li>
 * <li> Select best motion solution. </li>
 * <li> Triangular point location</li>
 * <li> Update camera position</li>
 * <li> Bundle adjustment</li>
 * <li> Repeat last 3 steps</li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class MonocularSimpleVo<T extends ImageBase> {
	KeyFramePointTracker<T,PointPoseTrack> tracker;

	ModelMatcher<DenseMatrix64F,AssociatedPair> computeE;
	RefineEpipolarMatrix refineE;
	
	DecomposeEssential decomposeE;
	PositiveDepthConstraintCheck depthChecker;

	TriangulateTwoViewsCalibrated triangulateAlg;

	ModelMatcher<Se3_F64,PointPositionPair> computeMotion;
	RefinePerspectiveNPoint refineMotion;

	// transform from the current image to the world frame
	Se3_F64 currToWorld = new Se3_F64();
	
	int mode;

	int inlierSize;
	double minDistance;
	int minFeatures;
	int setKeyThreshold;
	
	// storage for pixel motion, used to decide if the camera has moved or not
	double distance[];
	
	Se3_F64 nextInitialPose = new Se3_F64();
	boolean nextSet = false;
	
	
	FastQueue<PointPositionPair> queuePointPose = new FastQueue<PointPositionPair>(200,PointPositionPair.class,true);

	public MonocularSimpleVo( int minFeatures , double minDistance ,
							  ImagePointTracker<T> tracker ,
							  PointTransform_F64 pixelToNormalized ,
							  ModelMatcher<DenseMatrix64F,AssociatedPair> computeE ,
							  RefineEpipolarMatrix refineE ,
							  ModelMatcher<Se3_F64,PointPositionPair> computeMotion ,
							  RefinePerspectiveNPoint refineMotion )
	{
		this.minFeatures = minFeatures;
		this.minDistance = minDistance;
		this.tracker = new KeyFramePointTracker<T,PointPoseTrack>(tracker,pixelToNormalized,PointPoseTrack.class);
		this.computeE = computeE;
		this.refineE = refineE;
		this.computeMotion = computeMotion;
		this.refineMotion = refineMotion;

		distance = new double[ minFeatures*2 ];
	}

	/**
	 * Estimates the camera's ego motion by processing the image.  If true is returned then
	 * the position was estimated.  If false is returned then the position was not estimated
	 * and any past history has been discarded.
	 *
	 * @param image Image being processed.
	 *
	 * @return True if the motion was estimated and false if it was not.
	 */
	public boolean process( T image )
	{
		tracker.process(image);

		if( mode == 0 ) {
			tracker.setKeyFrame();
			tracker.spawnTracks();
			mode = 1;
		} else if( mode == 1 ) {
			checkInitialize();
		} else if( mode == 2 ) {
			if( !updatePosition() ) {
				// update failed so reset
				mode = 0;
				currToWorld.reset();
				tracker.reset();
				tracker.spawnTracks();
				return false;
			} else {
				// check and triangulate new features
				if( nextSet && (inlierSize<minFeatures) || isSufficientMotion() ) {
					triangulateNew();
				}
			}
		}

		return true;
	}

	private void checkInitialize() {
		List<PointPoseTrack> pairs = tracker.getPairs();

		if( pairs.size() < minFeatures ) {
			tracker.reset();
			tracker.spawnTracks();
			tracker.setKeyFrame();
		} else {
			// see if there has been enough pixel motion to warrant the cost of computing E
			if( isSufficientMotion()) {
				// initial estimate of E
				if( computeE.process((List)pairs) ) {
					DenseMatrix64F initial = computeE.getModel();
					List<AssociatedPair> inliers = computeE.getMatchSet();

					// refine E using non-linear optimization
					if( refineE.process(initial,inliers) ) {
						DenseMatrix64F E = refineE.getRefinement();
						decomposeE.decompose(E);

						// select best possible motion from E
						List<Se3_F64> solutions = decomposeE.getSolutions();
						
						Se3_F64 best = selectBestPose(solutions, inliers);
						currToWorld.set(best);

						// triangular feature points
						for( PointPoseTrack t : pairs ) {
							triangulateAlg.triangulate(t.currLoc,t.keyLoc, currToWorld,t.location);
						}

						nextSet = false;
						mode = 2;
					}
				}
			}
		}
	}

	/**
	 * Updates the position estimate using triangulation.
	 *
	 * @return true if successful or false if it failed
	 */
	private boolean updatePosition() {
		queuePointPose.reset();
		for( PointPoseTrack t : tracker.getPairs() ) {
			if( !t.active )
				continue;
			PointPositionPair p = queuePointPose.pop();
			p.location = t.location;
			p.observed = t.currLoc;
		}
		
		// estimate the camera's motion
		if( !computeMotion.process(queuePointPose.toList()) ) {
			return false;
		}
		
		List<PointPositionPair> inliers = computeMotion.getMatchSet();
		inlierSize = inliers.size();
		
		refineMotion.process(computeMotion.getModel(), inliers);

		currToWorld.set(refineMotion.getRefinement());

		// update point positions
		for( PointPoseTrack t : tracker.getPairs() ) {
			triangulateAlg.triangulate(t.currLoc,t.keyLoc, currToWorld,t.location);
		}
		
		// handle spawning new features
		if( !nextSet && inlierSize <= setKeyThreshold ) {
			tracker.spawnTracks();
			nextInitialPose.set(currToWorld);
			nextSet = true;
		}

		return true;
	}

	/**
	 * Triangulates the location of new features
	 */
	protected void triangulateNew() {
		// transform to take a feature from triangulateNew coordinate system into currToKey
		Se3_F64 inv = nextInitialPose.invert(null);
		// change in position from nextInitialPose to currToKey
		Se3_F64 motion = currToWorld.concat(inv,null);

		for( PointPoseTrack t : tracker.getPairs() ) {
			if( t.active )
				continue;
			
			triangulateAlg.triangulate(t.currLoc, t.keyLoc, motion, t.location);
			
			// put the location into the correct reference frame
			SePointOps_F64.transform(inv,t.location,t.location);
			
			t.active = true;
		}
		nextSet = false;
	}

	/**
	 * When decomposing the essential matrix, several possible motion are created.  Since any
	 * object which is seen by the camera must be in front of the camera the positive depth
	 * constraint can be used to select the most likely motion from the set.
	 */
	private Se3_F64 selectBestPose(List<Se3_F64> motions, List<AssociatedPair> observations) {
		int bestCount = 0;
		Se3_F64 bestModel = null;

		for( Se3_F64 s : motions ) {
			int count = 0;
			for( AssociatedPair p : observations ) {
				if( depthChecker.checkConstraint(p.currLoc,p.keyLoc,s) ) {
					count++;
				}
			}
			
			if( count < bestCount ) {
				bestCount = count;
				bestModel = s;
			}
		}
		
		return bestModel;
	}

	/**
	 * Looks at inactive points and decides if there is enough motion to estimate their 
	 * position.
	 * 
	 * @return true if there is sufficient motion
	 */
	private boolean isSufficientMotion() {
		
		List<PointPoseTrack> tracks = tracker.getPairs();
		
		if( distance.length < tracks.size() ) {
			distance = new double[ tracks.size()*4/3 ];
		}
		
		int count = 0;
		for( int i = 0; i < tracks.size(); i++ ) {
			PointPoseTrack p = tracks.get(i);
			if( p.active ) {
				distance[count++] = p.currLoc.distance2(p.keyLoc);
			}
		}

		double median = QuickSelectArray.select(distance,count/2,count);
	
		return median <= minDistance*minDistance;
	}

	public Se3_F64 getCameraLocation() {
		return currToWorld;
	}
}
