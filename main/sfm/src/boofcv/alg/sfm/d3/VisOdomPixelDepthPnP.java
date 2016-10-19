/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.sfm.d3;

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.feature.tracker.PointTrackerTwoPass;
import boofcv.abst.geo.RefinePnP;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import boofcv.struct.sfm.Point2D3DTrack;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Full 6-DOF visual odometry where a ranging device is assumed for pixels in the primary view and the motion is estimated
 * using a {@link boofcv.abst.geo.Estimate1ofPnP}.  Range is usually estimated using stereo cameras, structured
 * light or time of flight sensors.  New features are added and removed as needed.  Features are removed
 * if they are not part of the inlier feature set for some number of consecutive frames.  New features are detected
 * and added if the inlier set falls below a threshold or every turn.
 *
 * Non-linear refinement is optional and appears to provide a very modest improvement in performance.  It is recommended
 * that motion is estimated using a P3P algorithm, which is the minimal case.  Adding features every frame can be
 * computationally expensive, but having too few features being tracked will degrade accuracy. The algorithm was
 * designed to minimize magic numbers and to be insensitive to small changes in their values.
 *
 * Due to the level of abstraction, it can't take full advantage of the sensors used to estimate 3D feature locations.
 * For example if a stereo camera is used then 3-view geometry can't be used to improve performance.
 *
 * @author Peter Abeles
 */
public class VisOdomPixelDepthPnP<T extends ImageBase> {

	// when the inlier set is less than this number new features are detected
	private int thresholdAdd;

	// discard tracks after they have not been in the inlier set for this many updates in a row
	private int thresholdRetire;

	// run the tracker once or twice?
	private boolean doublePass;

	// tracks features in the image
	private PointTrackerTwoPass<T> tracker;
	// used to estimate a feature's 3D position from image range data
	private ImagePixelTo3D pixelTo3D;
	// converts from pixel to normalized image coordinates
	private Point2Transform2_F64 pixelToNorm;
	// convert from normalized image coordinates to pixel
	private Point2Transform2_F64 normToPixel;

	// non-linear refinement of pose estimate
	private RefinePnP refine;

	// estimate the camera motion up to a scale factor from two sets of point correspondences
	private ModelMatcher<Se3_F64, Point2D3D> motionEstimator;

	// location of tracks in the image that are included in the inlier set
	private List<Point2D3DTrack> inlierTracks = new ArrayList<>();

	// transform from key frame to world frame
	private Se3_F64 keyToWorld = new Se3_F64();
	// transform from the current camera view to the key frame
	private Se3_F64 currToKey = new Se3_F64();
	// transform from the current camera view to the world frame
	private Se3_F64 currToWorld = new Se3_F64();

	// is this the first camera view being processed?
	private boolean first = true;
	// number of frames processed.
	private long tick;

	// used when concating motion
	private Se3_F64 temp = new Se3_F64();

	/**
	 * Configures magic numbers and estimation algorithms.
	 *
	 * @param thresholdAdd Add new tracks when less than this number are in the inlier set.  Tracker dependent. Set to
	 *                     a value &le; 0 to add features every frame.
	 * @param thresholdRetire Discard a track if it is not in the inlier set after this many updates.  Try 2
	 * @param doublePass Associate image features a second time using the estimated model from the first
	 *                   try to improve results
	 * @param motionEstimator PnP motion estimator.  P3P algorithm is recommended/
	 * @param pixelTo3D Computes the 3D location of pixels.
	 * @param refine Optional algorithm for refining the pose estimate.  Can be null.
	 * @param tracker Point feature tracker.
	 * @param pixelToNorm Converts from raw image pixels into normalized image coordinates.
	 * @param normToPixel Converts from normalized image coordinates into raw pixels
	 */
	public VisOdomPixelDepthPnP(int thresholdAdd,
								int thresholdRetire ,
								boolean doublePass ,
								ModelMatcher<Se3_F64, Point2D3D> motionEstimator,
								ImagePixelTo3D pixelTo3D,
								RefinePnP refine ,
								PointTrackerTwoPass<T> tracker ,
								Point2Transform2_F64 pixelToNorm ,
								Point2Transform2_F64 normToPixel )
	{
		this.thresholdAdd = thresholdAdd;
		this.thresholdRetire = thresholdRetire;
		this.doublePass = doublePass;
		this.motionEstimator = motionEstimator;
		this.pixelTo3D = pixelTo3D;
		this.refine = refine;
		this.tracker = tracker;
		this.pixelToNorm = pixelToNorm;
		this.normToPixel = normToPixel;
	}

	/**
	 * Resets the algorithm into its original state
	 */
	public void reset() {
		tracker.reset();
		keyToWorld.reset();
		currToKey.reset();
		first = true;
		tick = 0;
	}

	/**
	 * Estimates the motion given the left camera image.  The latest information required by ImagePixelTo3D
	 * should be passed to the class before invoking this function.
	 *
	 * @param image Camera image.
	 * @return true if successful or false if it failed
	 */
	public boolean process( T image ) {
		tracker.process(image);

		tick++;
		inlierTracks.clear();

		if( first ) {
			addNewTracks();
			first = false;
		} else {
			if( !estimateMotion() ) {
				return false;
			}

			dropUnusedTracks();
			int N = motionEstimator.getMatchSet().size();

			if( thresholdAdd <= 0 || N < thresholdAdd ) {
				changePoseToReference();
				addNewTracks();
			}

//			System.out.println("  num inliers = "+N+"  num dropped "+numDropped+" total active "+tracker.getActivePairs().size());
		}

		return true;
	}


	/**
	 * Updates the relative position of all points so that the current frame is the reference frame.  Mathematically
	 * this is not needed, but should help keep numbers from getting too large.
	 */
	private void changePoseToReference() {
		Se3_F64 keyToCurr = currToKey.invert(null);

		List<PointTrack> all = tracker.getAllTracks(null);

		for( PointTrack t : all ) {
			Point2D3DTrack p = t.getCookie();
			SePointOps_F64.transform(keyToCurr,p.location,p.location);
		}

		concatMotion();
	}

	/**
	 * Removes tracks which have not been included in the inlier set recently
	 *
	 * @return Number of dropped tracks
	 */
	private int dropUnusedTracks() {

		List<PointTrack> all = tracker.getAllTracks(null);
		int num = 0;

		for( PointTrack t : all ) {
			Point2D3DTrack p = t.getCookie();
			if( tick - p.lastInlier > thresholdRetire ) {
				tracker.dropTrack(t);
				num++;
			}
		}

		return num;
	}

	/**
	 * Detects new features and computes their 3D coordinates
	 */
	private void addNewTracks() {
//		System.out.println("----------- Adding new tracks ---------------");

		tracker.spawnTracks();
		List<PointTrack> spawned = tracker.getNewTracks(null);

		// estimate 3D coordinate using stereo vision
		for( PointTrack t : spawned ) {
			Point2D3DTrack p = t.getCookie();
			if( p == null) {
				t.cookie = p = new Point2D3DTrack();
			}

			// discard point if it can't localized
			if( !pixelTo3D.process(t.x,t.y) || pixelTo3D.getW() == 0 ) {
				tracker.dropTrack(t);
			} else {
				Point3D_F64 X = p.getLocation();

				double w = pixelTo3D.getW();
				X.set(pixelTo3D.getX() / w, pixelTo3D.getY() / w, pixelTo3D.getZ() / w);

				// translate the point into the key frame
				// SePointOps_F64.transform(currToKey,X,X);
				// not needed since the current frame was just set to be the key frame

				p.lastInlier = tick;
				pixelToNorm.compute(t.x, t.y, p.observation);
			}
		}
	}

	/**
	 * Estimates motion from the set of tracks and their 3D location
	 *
	 * @return true if successful.
	 */
	private boolean estimateMotion() {
		List<PointTrack> active = tracker.getActiveTracks(null);
		List<Point2D3D> obs = new ArrayList<>();

		for( PointTrack t : active ) {
			Point2D3D p = t.getCookie();
			pixelToNorm.compute( t.x , t.y , p.observation );
			obs.add( p );
		}

		// estimate the motion up to a scale factor in translation
		if( !motionEstimator.process( obs ) )
			return false;

		if( doublePass ) {
			if (!performSecondPass(active, obs))
				return false;
		}
		tracker.finishTracking();

		Se3_F64 keyToCurr;

		if( refine != null ) {
			keyToCurr = new Se3_F64();
			refine.fitModel(motionEstimator.getMatchSet(), motionEstimator.getModelParameters(), keyToCurr);
		} else {
			keyToCurr = motionEstimator.getModelParameters();
		}

		keyToCurr.invert(currToKey);

		// mark tracks as being inliers and add to inlier list
		int N = motionEstimator.getMatchSet().size();
		for( int i = 0; i < N; i++ ) {
			int index = motionEstimator.getInputIndex(i);
			Point2D3DTrack t = active.get(index).getCookie();
			t.lastInlier = tick;
			inlierTracks.add( t );
		}

		return true;
	}

	private boolean performSecondPass(List<PointTrack> active, List<Point2D3D> obs) {
		Se3_F64 keyToCurr = motionEstimator.getModelParameters();

		Point3D_F64 cameraPt = new Point3D_F64();
		Point2D_F64 predicted = new Point2D_F64();

		// predict where each track should be given the just estimated motion
		List<PointTrack> all = tracker.getAllTracks(null);
		for( PointTrack t : all ) {
			Point2D3D p = t.getCookie();

			SePointOps_F64.transform(keyToCurr, p.location, cameraPt);
			normToPixel.compute(cameraPt.x / cameraPt.z, cameraPt.y / cameraPt.z, predicted);
			tracker.setHint(predicted.x,predicted.y,t);
		}

		// redo tracking with the additional information
		tracker.performSecondPass();

		active.clear();
		obs.clear();
		tracker.getActiveTracks(active);

		for( PointTrack t : active ) {
			Point2D3D p = t.getCookie();
			pixelToNorm.compute( t.x , t.y , p.observation );
			obs.add( p );
		}

		return motionEstimator.process(obs);
	}

	private void concatMotion() {
		currToKey.concat(keyToWorld,temp);
		keyToWorld.set(temp);
		currToKey.reset();
	}

	public Se3_F64 getCurrToWorld() {
		currToKey.concat(keyToWorld,currToWorld);
		return currToWorld;
	}

	public PointTracker<T> getTracker() {
		return tracker;
	}

	public ModelMatcher<Se3_F64, Point2D3D> getMotionEstimator() {
		return motionEstimator;
	}

	public List<Point2D3DTrack> getInlierTracks() {
		return inlierTracks;
	}

	public void setPixelToNorm(Point2Transform2_F64 pixelToNorm) {
		this.pixelToNorm = pixelToNorm;
	}

	public void setNormToPixel(Point2Transform2_F64 normToPixel) {
		this.normToPixel = normToPixel;
	}

	public long getTick() {
		return tick;
	}
}
