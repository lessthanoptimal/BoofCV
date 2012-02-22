package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.KeyFramePointTracker;
import boofcv.abst.geo.epipolar.RefineFundamental;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.d3.epipolar.DecomposeEssential;
import boofcv.alg.geo.d3.epipolar.PositiveDepthConstraintCheck;
import boofcv.numerics.fitting.modelset.ModelFitter;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.image.ImageBase;
import georegression.struct.se.Se3_F64;
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
	KeyFramePointTracker<T> tracker;

	ModelMatcher<DenseMatrix64F,AssociatedPair> computeE;
	RefineFundamental refineE;
	
	DecomposeEssential decomposeE;
	PositiveDepthConstraintCheck depthChecker;

	ModelMatcher<Se3_F64,AssociatedPair> computeMotion;
	ModelFitter<Se3_F64,AssociatedPair> refineMotion;

	Se3_F64 keyToWorld = new Se3_F64();
	Se3_F64 currToKey = new Se3_F64();
	
	int mode;
	
	double minDistance;
	int minFeatures;
	
	double distance[];

	public MonocularSimpleVo( int minFeatures , double minDistance , ImagePointTracker<T> tracker ) {
		this.minFeatures = minFeatures;
		this.minDistance = minDistance;
		this.tracker = new KeyFramePointTracker<T>(tracker);

		distance = new double[ minFeatures*2 ];
	}
	
	public void process( T image ) {
		tracker.process(image);

		// TODO convert tracker feature points to normalized coordinates

		if( mode == 0 ) {
			tracker.setKeyFrame();
			tracker.spawnTracks();
			mode = 1;
		} else if( mode == 1 ) {
			checkInitialize();
		} else if( mode == 2 ) {
			updatePosition();
		}
	}

	private void checkInitialize() {
		List<AssociatedPair> pairs = tracker.getPairs();

		if( pairs.size() < minFeatures ) {
			tracker.reset();
			tracker.spawnTracks();
			tracker.setKeyFrame();
		} else {
			// see if there has been enough pixel motion to warrant the cost of computing E
			if( isSufficientMotion(pairs)) {
				// initial estimate of E
				if( computeE.process(pairs) ) {
					DenseMatrix64F initial = computeE.getModel();
					List<AssociatedPair> inliers = computeE.getMatchSet();

					// refine E using non-linear optimization
					if( refineE.process(initial,inliers) ) {
						DenseMatrix64F E = refineE.getRefinement();
						decomposeE.decompose(E);

						// select best possible motion from E
						List<Se3_F64> solutions = decomposeE.getSolutions();
						
						Se3_F64 best = selectBest(solutions,inliers);
						currToKey.set(best);

						// triangular feature points
						// TODO do that

						mode = 2;
					}
				}
			}
		}
	}

	private void updatePosition() {


	}

	/**
	 * When decomposing the essential matrix, several possible motion are created.  Since any
	 * object which is seen by the camera must be in front of the camera the positive depth
	 * constraint can be used to select the most likely motion from the set.
	 */
	private Se3_F64 selectBest(List<Se3_F64> motions, List<AssociatedPair> observations) {
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
	 * Checks to see if the pixels have the minimal amount of motion to consider updating a keyframe.
	 * @return
	 */
	private boolean isSufficientMotion( List<AssociatedPair> pairs ) {
		
		if( distance.length < pairs.size() ) {
			distance = new double[ pairs.size()*4/3 ];
		}
		for( int i = 0; i < pairs.size(); i++ ) {
			AssociatedPair p = pairs.get(i);
			distance[i] = p.currLoc.distance2(p.keyLoc);
		}

		double median = QuickSelectArray.select(distance,pairs.size()/2,pairs.size());
	
		return median <= minDistance*minDistance;
	}
}
