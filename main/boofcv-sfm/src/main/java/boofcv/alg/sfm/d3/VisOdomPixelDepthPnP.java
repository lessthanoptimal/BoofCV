/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.RefinePnP;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTrackerTwoPass;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.alg.sfm.d3.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.VisOdomBundleAdjustment.BObservation;
import boofcv.alg.sfm.d3.VisOdomBundleAdjustment.BTrack;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

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
public class VisOdomPixelDepthPnP<T extends ImageBase<T>> {

	// discard tracks after they have not been in the inlier set for this many updates in a row
	private int thresholdRetire;

	// run the tracker once or twice?
	private boolean doublePass;

	// tracks features in the image
	private final @Getter PointTrackerTwoPass<T> tracker;
	/** used to estimate a feature's 3D position from image range data */
	private final @Getter ImagePixelTo3D pixelTo3D;
	/** converts from pixel to normalized image coordinates */
	private @Getter Point2Transform2_F64 pixelToNorm;
	/** convert from normalized image coordinates to pixel */
	private @Getter Point2Transform2_F64 normToPixel;

	// non-linear refinement of pose estimate
	private final RefinePnP refine;

	/** Maximum number of key frames */
	private @Getter @Setter int maxKeyFrames = 5;

	private final VisOdomBundleAdjustment<Track> bundle = new VisOdomBundleAdjustment<>(Track::new);
	private BFrame frameCurrent;
	private BFrame framePrevious;

	// estimate the camera motion up to a scale factor from two sets of point correspondences
	private final ModelMatcher<Se3_F64, Point2D3D> motionEstimator;
	private final FastQueue<Point2D3D> observationsPnP = new FastQueue<>(Point2D3D::new);

	/** location of tracks in the image that are included in the inlier set */
	private @Getter final List<Track> inlierTracks = new ArrayList<>();

	// transform from the current camera view to the key frame
	private final Se3_F64 currToKey = new Se3_F64();
	/** transform from the current camera view to the world frame */
	private @Getter final Se3_F64 currToWorld = new Se3_F64();

	// is this the first camera view being processed?
	private boolean first = true;

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
	public VisOdomPixelDepthPnP(int thresholdAdd, // TODO remove
								int thresholdRetire ,
								boolean doublePass ,
								ModelMatcher<Se3_F64, Point2D3D> motionEstimator,
								ImagePixelTo3D pixelTo3D,
								RefinePnP refine ,
								PointTrackerTwoPass<T> tracker ,
								Point2Transform2_F64 pixelToNorm ,
								Point2Transform2_F64 normToPixel )
	{
		this.thresholdRetire = thresholdRetire;
		this.doublePass = doublePass;
		this.motionEstimator = motionEstimator;
		this.pixelTo3D = pixelTo3D;
		this.refine = refine;
		this.tracker = tracker;
		this.pixelToNorm = pixelToNorm;
		this.normToPixel = normToPixel;

//		bundle.bundleAdjustment.setVerbose(System.out,0);
	}

	/**
	 * Resets the algorithm into its original state
	 */
	public void reset() {
		tracker.reset();
		currToKey.reset();
		bundle.reset();
		first = true;
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

		inlierTracks.clear();

		// TODO don't always use previous. Use frame that it has the most common tracks. This will enable it to skip
		//      over bad frames
		// Previous key frame is the most recently added one, which is the last
		framePrevious = first ? null : bundle.getLastFrame();
		// Create a new frame for the current image
		frameCurrent = bundle.addFrame(tracker.getFrameID());

		// Handle the very first image differently
		if( first ) {
			addNewTracks();
			bundle.sanityCheck();
			currToWorld.reset();
			first = false;
		} else {
			if( framePrevious == null )
				throw new RuntimeException("BUG! No previous frame and not the first frame");

			if( !estimateMotion() ) {
				// discard the current frame and attempt to jump over it
				bundle.removeFrame(frameCurrent);
				bundle.sanityCheck();
				return false;
			}

			// Update the state estimate
			bundle.sanityCheck();
			bundle.optimize();
			// Save the output
			currToWorld.set(frameCurrent.frameToWorld);

			// Drop tracks which aren't being used
			bundle.sanityCheck();
			dropUnusedTracks();
			bundle.sanityCheck();

			// Always add key frames until it hits the limit
			if( bundle.frames.size() >= maxKeyFrames ) {
				// Select the "optimal" key frame to drop and drop it
				BFrame target = selectFrameToDrop();
//				System.out.println("Dropping frame "+(bundle.frames.indexOf(target)+" / "+bundle.frames.size));

				bundle.sanityCheck();
				bundle.removeFrame(target);
				bundle.sanityCheck();
				if( target != frameCurrent ) {
					// it decided to keep the current track. Spawn new tracks in the current frame
					addNewTracks();
					bundle.sanityCheck();
				}
			}
		}

		return true;
	}

	/**
	 * Sets the known fixed camera parameters
	 */
	public void setCamera( CameraPinholeBrown camera ) {
		bundle.camera = new BundlePinholeBrown(camera);
		LensDistortionNarrowFOV factory = LensDistortionFactory.narrow(camera);
		pixelToNorm = factory.undistort_F64(true,false);
		normToPixel = factory.distort_F64(false,true);
	}

	/**
	 * Selects a key frame to drop. A frame is dropped based on two values which measure relative
	 * to the previous key frame X.. 1) How similar it is X in appearance. 2) How correlated the state is to X.
	 */
	private BFrame selectFrameToDrop() {
		// TODO avoid clusters of similar frames
		System.out.println("Selecting frame to drop. prev = "+framePrevious.id);
		// higher values are worse
		double worstScore = Double.MAX_VALUE;
		int worstIdx = -1;

		GrowQueue_I32 listCommon = new GrowQueue_I32(bundle.frames.size());
		GrowQueue_F64 listMotion = new GrowQueue_F64(bundle.frames.size());
		double largestMotion = 0;

		for( int frameIdx = 0; frameIdx < bundle.frames.size(); frameIdx++ ) {
			BFrame frameI = bundle.frames.get(frameIdx);
			if( frameI == framePrevious ) {
				listCommon.add(0);
				listMotion.add(0);
				continue;
			}

			int totalCommon = 0;      // total number of tracks in common
			double averageMotion = 0; // average motion in image pixels of distorted image
			for (int trackIdx = 0; trackIdx < frameI.tracks.size; trackIdx++) {
				BObservation obsI = null;
				BObservation obsP = null;

				BTrack t = frameI.tracks.get(trackIdx);

				for (int i = 0; i < t.observations.size; i++) {
					BObservation o = t.observations.data[i];
					if( o.frame == frameI ) {
						obsI = o;
					} else if( o.frame == framePrevious ) {
						obsP = o;
					}
				}

				if( obsI == null )
					throw new RuntimeException(
							"BUG! No observation of frame I inside of the observation that was in its tracks list");
				if( obsP == null )
					continue;
				totalCommon++;
				averageMotion += obsI.distance(obsP);
			}

			listCommon.add(totalCommon);
			listMotion.add(averageMotion/(1+totalCommon));
			largestMotion = Math.max(largestMotion, averageMotion/(1+totalCommon));
		}

		boolean before = true;
		for (int frameIdx = 0; frameIdx < bundle.frames.size; frameIdx++) {
			BFrame frameI = bundle.frames.get(frameIdx);
			if( frameI == framePrevious ) {
				System.out.printf("Frame ID %5d is the previous frame\n",frameI.id);
				before = false;
				continue;
			}

			int totalCommon = listCommon.get(frameIdx);
			double motion = listMotion.get(frameIdx);

			double score;
			if( totalCommon > 0 ) {
				int N = framePrevious.tracks.size;
				double fractionInCommon = totalCommon/(double)N;

				if( before ) {
					// Keep older frames if they have a lot of features in common since that means they are
					// more relevant
					score = fractionInCommon;
				} else {
					// if the latest has lots in common that means not much has changed and you can skip it
					score = 1.0-fractionInCommon;
				}
				score = score*(0.05+motion/largestMotion);

				System.out.printf("Frame ID %5d score = %.5f common %4d / %4d motion %6.2f\n",frameI.id,score,totalCommon,N,(motion/largestMotion));
			} else {
				System.out.printf("Frame ID %5d failed\n",frameI.id);
				score = 0.0;
			}

//			System.out.println("Frame ID "+frameI.id+" score "+score+"  common "+totalCommon+" motion "+averageMotion);
			if( score < worstScore ) {
				worstIdx = frameIdx;
				worstScore = score;
			}

		}

		System.out.println("       dropping "+bundle.frames.get(worstIdx).id);

		return bundle.frames.get(worstIdx);
	}

	/**
	 * Removes tracks which have not been included in the inlier set recently
	 *
	 * @return Number of dropped tracks
	 */
	private int dropUnusedTracks() {

		List<PointTrack> all = tracker.getAllTracks(null);
		long trackerFrame = tracker.getFrameID();
		int num = 0;

		for( PointTrack t : all ) {
			Track p = t.getCookie();
			if( p==null|| (trackerFrame - p.lastUsed >= thresholdRetire) ) {
				tracker.dropTrack(t);
				p.observations.reset(); // This marks it as needing to be removed.
				num++;
			}
		}

		// TODO remove them from all frames
		// todo remove from master track list

		return num;
	}

	/**
	 * Detects new features and computes their 3D coordinates
	 */
	private void addNewTracks() {
//		System.out.println("----------- Adding new tracks ---------------");

		System.out.println("addNewTracks() current frame="+frameCurrent.id);

		long frameID = tracker.getFrameID();

		tracker.spawnTracks();
		List<PointTrack> spawned = tracker.getNewTracks(null);

		bundle.sanityCheck();


		// estimate 3D coordinate using stereo vision
		for( PointTrack t : spawned ) {
//			if( t.cookie != null )
//				throw new RuntimeException("BUG!");
			// discard point if it can't localized
			if( !pixelTo3D.process(t.x,t.y) || pixelTo3D.getW() == 0 ) { // TODO don't drop infinity
				tracker.dropTrack(t);
			} else {
				bundle.sanityCheck();
				// Save the track's 3D location and add it to the current frame
				Track track = bundle.addTrack(t.featureId,pixelTo3D.getX(),pixelTo3D.getY(),pixelTo3D.getZ(),pixelTo3D.getW());
				System.out.println("Created track "+track.id+"  currentFrame "+frameCurrent.id+"  obs.size "+track.observations.size);
				bundle.sanityCheck();
				// Convert the location from local coordinate system to world coordinates
				SePointOps_F64.transform(frameCurrent.frameToWorld,track.worldLoc,track.worldLoc);
				bundle.sanityCheck();

				bundle.addObservation(frameCurrent, track, t.x , t.y);

				track.lastUsed = frameID;
				t.cookie = track;

//				if( !track.isObservedBy(frameCurrent)) {
//					throw new RuntimeException("WTF");
//				}
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

		Se3_F64 world_to_prev = new Se3_F64();
		framePrevious.frameToWorld.invert(world_to_prev);

		// Create a list of observations for PnP
		// normalized image coordinates and 3D in the previous keyframe's reference frame
		observationsPnP.reset();
		for( PointTrack pt : active ) {
			Point2D3D p = observationsPnP.grow();
			pixelToNorm.compute( pt.x , pt.y , p.observation );
			Track bt = pt.getCookie();

			// TODO Handle infinity better. avoid divide by zero?
			// Put the point into
			double w = bt.worldLoc.w;
			if( w != 0.0)
				p.location.set( bt.worldLoc.x/w, bt.worldLoc.y/w, bt.worldLoc.z/w);
			else {
				p.location.set(bt.worldLoc.x, bt.worldLoc.y, bt.worldLoc.z);
				// it was observed so it hsa to be in front
				if( p.location.z < 0 )
					p.location.scale(-1);
			}

			SePointOps_F64.transform(world_to_prev,p.location,p.location);
		}

		// estimate the motion up to a scale factor in translation
		if( !motionEstimator.process( observationsPnP.toList() ) )
			return false;

//		if( doublePass ) {
//			if (!performSecondPass(active, observationsPnP.toList()))
//				return false;
//		}
		tracker.finishTracking();

		Se3_F64 keyToCurr;

		if( refine != null ) {
			keyToCurr = new Se3_F64();
			refine.fitModel(motionEstimator.getMatchSet(), motionEstimator.getModelParameters(), keyToCurr);
		} else {
			keyToCurr = motionEstimator.getModelParameters();
		}

		// Change everything back to the world frame
		keyToCurr.invert(currToKey);
		currToKey.concat(framePrevious.frameToWorld,frameCurrent.frameToWorld);

		// mark tracks as being inliers and add to inlier list
		int N = motionEstimator.getMatchSet().size();
		long tick = tracker.getFrameID();
		for( int i = 0; i < N; i++ ) {
			int index = motionEstimator.getInputIndex(i);
			PointTrack p = active.get(index);
			Track t = p.getCookie();
			t.lastUsed = tick;
			t.active = true;
			bundle.addObservation(frameCurrent, t, p.x, p.y);
			inlierTracks.add( t );
		}
		bundle.sanityCheck();

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

	public long getTick() {
		return tracker.getFrameID();
	}

	public static class Track extends BTrack {
		public long lastUsed;
	}
}
