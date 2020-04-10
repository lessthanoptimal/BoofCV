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
import boofcv.abst.geo.TriangulateNViewsMetric;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.sfm.d3.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.VisOdomBundleAdjustment.BObservation;
import boofcv.alg.sfm.d3.VisOdomBundleAdjustment.BTrack;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.io.PrintStream;
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
	private @Getter @Setter int thresholdRetireTracks;

	// tracks features in the image
	private final @Getter PointTracker<T> tracker;
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

	private @Getter final VisOdomBundleAdjustment<Track> bundle;
	private BFrame frameCurrent;
	private BFrame framePrevious;

	// estimate the camera motion up to a scale factor from two sets of point correspondences
	private final ModelMatcher<Se3_F64, Point2D3D> motionEstimator;
	private final FastQueue<Point2D3D> observationsPnP = new FastQueue<>(Point2D3D::new);

	/** location of tracks in the image that are included in the inlier set */
	private @Getter final List<Track> inlierTracks = new ArrayList<>();
	/** tracks which are currently visible */
	private @Getter final List<Track> visibleTracks = new ArrayList<>();
	// initial list of visible tracks before dropping
	final List<Track> initialVisible = new ArrayList<>();

	// transform from the current camera view to the key frame
	private final Se3_F64 current_to_key = new Se3_F64();
	/** transform from the current camera view to the world frame */
	private final Se3_F64 current_to_world = new Se3_F64();

	// is this the first camera view being processed?
	private boolean first = true;

	// Internal profiling
	private @Getter @Setter	PrintStream profileOut;
	private @Getter double timeTracking,timeEstimate,timeBundle,timeDropUnused,timeDropFrame,timeSpawn;

	//=================================================================
	// workspace variables
	List<PointTrack> removedTrackerTracks = new ArrayList<>();
	GrowQueue_I32 listCommon = new GrowQueue_I32();
	GrowQueue_F64 listMotion = new GrowQueue_F64();

	/**
	 * Configures magic numbers and estimation algorithms.
	 *
	 * @param motionEstimator PnP motion estimator.  P3P algorithm is recommended/
	 * @param pixelTo3D Computes the 3D location of pixels.
	 * @param refine Optional algorithm for refining the pose estimate.  Can be null.
	 * @param tracker Point feature tracker.
	 */
	public VisOdomPixelDepthPnP(ModelMatcher<Se3_F64, Point2D3D> motionEstimator,
								ImagePixelTo3D pixelTo3D,
								RefinePnP refine ,
								PointTracker<T> tracker ,
								BundleAdjustment<SceneStructureMetric> bundleAdjustment )
	{
		this.motionEstimator = motionEstimator;
		this.pixelTo3D = pixelTo3D;
		this.refine = refine;
		this.tracker = tracker;
		this.bundle = new VisOdomBundleAdjustment<>(bundleAdjustment,Track::new);
	}

	/**
	 * Resets the algorithm into its original state
	 */
	public void reset() {
		System.out.println("RESET CALLED");
		tracker.reset();
		current_to_key.reset();
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
		timeTracking = timeBundle = timeDropFrame = timeDropUnused = timeEstimate = timeSpawn = 0;

		long time0 = System.nanoTime();
		tracker.process(image);
		long time1 = System.nanoTime();
		timeTracking = (time1-time0)*1e-6;

		System.out.println("-----------------------------------------------------------------------------------------");
		System.out.println("Input Frame Count   "+tracker.getFrameID());
		System.out.println("   Bundle Frames    "+bundle.frames.size);
		System.out.println("   Bundle tracks    "+bundle.tracks.size);
		System.out.println("   Tracker active   "+tracker.getTotalActive());
		System.out.println("   Tracker inactive "+tracker.getTotalInactive());


		inlierTracks.clear();
		visibleTracks.clear();
		initialVisible.clear();

		// TODO don't always use previous. Use frame that it has the most common tracks. This will enable it to skip
		//      over bad frames
		// Previous key frame is the most recently added one, which is the last
		framePrevious = first ? null : bundle.getLastFrame();
		// Create a new frame for the current image
		frameCurrent = bundle.addFrame(tracker.getFrameID());

		// Handle the very first image differently
		if( first ) {
			current_to_world.reset();
			addNewTracks(visibleTracks);
			first = false;
			return true;
		}

		// handle tracks that the visual tracker dropped
		removedTrackerTracks.clear();
		tracker.getDroppedTracks(removedTrackerTracks);
		for (int i = 0; i < removedTrackerTracks.size(); i++) {
			// Tell the bundle track that they are no longer associated with a visual track
			BTrack bt = removedTrackerTracks.get(i).getCookie();
//			System.out.println("tracker dropped bt="+bt.id+" tt.id="+bt.trackerTrack.featureId);
			bt.trackerTrack = null;
		}

		if( !estimateMotion() ) {
			System.out.println("ESTIMATE MOTION FAILED");
			// discard the current frame and attempt to jump over it
			bundle.removeFrame(frameCurrent, removedTrackerTracks);
			dropRemovedBundleTracks();
			keepStillVisibleTracks();
			return false;
		}
		// Drop tracker tracks which aren't being used inside of the inlier set
		dropUnusedTrackerTracks();

		double time2 = System.nanoTime();
		timeEstimate = (time2-time1)*1e-6;

		// Update the state estimate
		bundle.optimize();
		// Save the output
		current_to_world.set(frameCurrent.frame_to_world);

		triangulateNotSelectedBundleTracks();
		dropBadBundleTracks();

		double time3 = System.nanoTime();
		timeBundle = (time3-time2)*1e-6; // todo update

		long time4 = System.nanoTime();
		timeDropUnused = (time4-time3)*1e-6;

		// Always add key frames until it hits the limit
		if( bundle.frames.size() >= maxKeyFrames ) {
			// Select the "optimal" key frame to drop and drop it
			BFrame target = selectFrameToDrop();

			bundle.removeFrame(target, removedTrackerTracks);
			dropRemovedBundleTracks();
			// Drop tracks which are not visible in the latest frame and have fewer than 3 observations
			dropTracksNotVisibleTooFewObservations();
			keepStillVisibleTracks();

			if( target != frameCurrent ) {
				System.out.println("   Spawning new tracks");
				// it decided to keep the current track. Spawn new tracks in the current frame
				long timeSpawn0 = System.nanoTime();
				addNewTracks(visibleTracks);
				timeSpawn = (System.nanoTime()-timeSpawn0)*1e-6;
			}
		} else {
			keepStillVisibleTracks();
			// it decided to keep the current track. Spawn new tracks in the current frame
			long timeSpawn0 = System.nanoTime();
			addNewTracks(visibleTracks);
			timeSpawn = (System.nanoTime()-timeSpawn0)*1e-6;
		}

		long time5 = System.nanoTime();
		timeDropFrame = (time5-time4)*1e-6;
		double timeTotal = (time5-time0)*1e-6;

		if( profileOut != null ) {
			profileOut.printf("StereoVO: TRK %5.1f Est %5.1f Bun %5.1f DU %5.1f DF %5.1f Swn %5.1f TOTAL %5.1f\n",
					timeTracking, timeEstimate, timeBundle, timeDropUnused, timeDropFrame,timeSpawn,timeTotal);
		}

//		bundle.sanityCheck();

		return true;
	}

	/**
	 * Drop tracks which are no longer being visually tracked and have less than two observations. In general
	 * 3 observations is much more stable than two and less prone to be a false positive.
	 */
	private void dropTracksNotVisibleTooFewObservations() {
		for (int bidx = bundle.tracks.size-1; bidx >= 0; bidx--) {
			BTrack t = bundle.tracks.get(bidx);
			if( t.trackerTrack == null && t.observations.size < 3 ) {
//				System.out.println("drop old bt="+t.id+" tt=NONE");
				// this marks it as dropped. Formally remove it in the next loop
				t.observations.reset();
				bundle.tracks.removeSwap(bidx);
			}
		}
		for (int fidx = 0; fidx < bundle.frames.size; fidx++) {
			BFrame f = bundle.frames.get(fidx);
			for (int i = f.tracks.size-1; i >= 0; i--) {
				if( f.tracks.get(i).observations.size == 0 ) {
					f.tracks.removeSwap(i);
				}
			}
		}
	}

	/**
	 * Goes through the list of initially visible tracks and see which ones have not been dropped
	 */
	private void keepStillVisibleTracks() {
		for (int i = 0; i < initialVisible.size(); i++) {
			Track t = initialVisible.get(i);
			if( t.trackerTrack != null ) {
				visibleTracks.add(t);
			}
		}
	}

	// TODO comments
	private void dropRemovedBundleTracks() {
		for (int i = 0; i < removedTrackerTracks.size(); i++) {
			PointTrack pt = removedTrackerTracks.get(i);
			tracker.dropTrack(pt);
		}
	}

	/**
	 * Triangulate tracks which were not included in the optimization
	 */
	private void triangulateNotSelectedBundleTracks() {
		ConfigTriangulation config = new ConfigTriangulation();
		config.type = ConfigTriangulation.Type.GEOMETRIC;
		config.optimization.maxIterations = 10;

		// TODO would be best if this reduced pixel error
		// TODO remove and replace with calibrated homogenous coordinates when it exists
		TriangulateNViewsMetric triangulator = FactoryMultiView.triangulateNViewCalibrated(config);

		FastQueue<Point2D_F64> observationsNorm = new FastQueue<>(Point2D_F64::new);
		FastQueue<Se3_F64> world_to_frame = new FastQueue<>(Se3_F64::new);
		Point3D_F64 found3D = new Point3D_F64();

		for (int trackIdx = 0; trackIdx < bundle.tracks.size; trackIdx++) {
			BTrack bt = bundle.tracks.data[trackIdx];
			// skip selected since they have already been optimized or only two observations
			if( bt.selected || bt.observations.size < 3)
				continue;

			observationsNorm.reset();
			world_to_frame.reset();
			for (int obsIdx = 0; obsIdx < bt.observations.size; obsIdx++) {
				BObservation bo = bt.observations.get(obsIdx);
				pixelToNorm.compute(bo.pixel.x,bo.pixel.y,observationsNorm.grow());
				bo.frame.frame_to_world.invert(world_to_frame.grow());
			}

			if( triangulator.triangulate(observationsNorm.toList(),world_to_frame.toList(),found3D) ) {
				bt.worldLoc.x = found3D.x;
				bt.worldLoc.y = found3D.y;
				bt.worldLoc.z = found3D.z;
				bt.worldLoc.w = 1.0;
			}
		}
	}

	/**
	 * Remove tracks with large errors and impossible geometry
	 */
	private void dropBadBundleTracks() {
		Se3_F64 world_to_frame = new Se3_F64();
		Point4D_F64 cameraLoc = new Point4D_F64();

		for (int frameidx = 0; frameidx < bundle.frames.size; frameidx++) {
			BFrame frame = bundle.frames.get(frameidx);
			frame.frame_to_world.invert(world_to_frame);

			for (int trackidx = frame.tracks.size-1; trackidx >= 0; trackidx--) {
				BTrack track = frame.tracks.get(trackidx);
				SePointOps_F64.transform(world_to_frame, track.worldLoc, cameraLoc);

				// test to see if the feature is behind the camera while avoiding divded by zero errors
				if( Math.signum(cameraLoc.z) * Math.signum(cameraLoc.w) < 0 ) {
//					System.out.println("Dropping bad track");
					// this marks it for removal later on
					track.observations.reset();
					if( track.trackerTrack != null ) {
						tracker.dropTrack(track.trackerTrack);
						track.trackerTrack = null;
					}
				}

				// TODO test to see if residual is excessively large
			}
		}

		for (int bidx = bundle.tracks.size-1; bidx >= 0; bidx--) {
			BTrack t = bundle.tracks.get(bidx);
			if( t.observations.size == 0 ) {
				bundle.tracks.removeSwap(bidx);
			}
		}

		for (int fidx = 0; fidx < bundle.frames.size; fidx++) {
			BFrame f = bundle.frames.get(fidx);
			for (int i = f.tracks.size-1; i >= 0; i--) {
				if( f.tracks.get(i).observations.size == 0 ) {
					f.tracks.removeSwap(i);
				}
			}
		}
	}

	/**
	 * Sets the known fixed camera parameters
	 */
	public void setCamera( CameraPinholeBrown camera ) {
		bundle.setCamera(camera);
		LensDistortionNarrowFOV factory = LensDistortionFactory.narrow(camera);
		pixelToNorm = factory.undistort_F64(true,false);
		normToPixel = factory.distort_F64(false,true);
	}

	/**
	 * Selects a key frame to drop. A frame is dropped based on two values which measure relative
	 * to the previous key frame X.. 1) How similar it is X in appearance. 2) How correlated the state is to X.
	 */
	private BFrame selectFrameToDrop() {
		// higher values are worse
		double worstScore = Double.MAX_VALUE;
		int worstIdx = -1;

		listCommon.reset();
		listMotion.reset();

		double largestMotion = 0;

		for( int frameIdx = 0; frameIdx < bundle.frames.size(); frameIdx++ ) {
			BFrame frameI = bundle.frames.get(frameIdx);
			if( frameI == framePrevious ) {
				// dummy value to keep the indexes te same
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
				averageMotion += obsI.pixel.distance(obsP.pixel);
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

				// TODO remove back. this makes it want to keep the most recent frame and spawn tracks
				if( frameIdx == bundle.frames.size-1 )
					score *= 1.5;

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

		System.out.println("   dropping frame "+bundle.frames.get(worstIdx).id);
		return bundle.frames.get(worstIdx);
	}

	/**
	 * Removes tracks which have not been included in the inlier set recently
	 */
	private void dropUnusedTrackerTracks() {
		final long trackerFrame = tracker.getFrameID();

		tracker.dropTracks(track -> {
			Track bt = track.getCookie();
			if( bt == null ) {
				// sanity check
				throw new RuntimeException("BUG!");
			} else if( trackerFrame - bt.lastUsed >= thresholdRetireTracks) {
				// See if it is visible in the current frame. if so remove the observations from it
				BObservation obs = bt.observations.getTail();
				if( obs.frame == frameCurrent ) {
					bt.observations.removeTail();
				}
				bt.trackerTrack = null;
				return true;
			}
			return false;
		});

		// Remove tracks which are no longer visible in the current frame since they were dropped
		for( int i = frameCurrent.tracks.size-1; i >= 0; i-- ) {
			// The most recent observation for any track in the current frame would be the current frame
			// unless it has been dropped
			BTrack t = frameCurrent.tracks.get(i);
			BObservation o = t.observations.getTail();
			if( o.frame != frameCurrent ) {
				frameCurrent.tracks.removeSwap(i);
			}

			if( t.observations.size == 0 )
				throw new RuntimeException("BUG");
		}
	}

	/**
	 * Detects new features and computes their 3D coordinates
	 */
	private void addNewTracks( List<Track> visibleTracks ) {
//		System.out.println("addNewTracks() current frame="+frameCurrent.id);

		long frameID = tracker.getFrameID();

		tracker.spawnTracks();
		List<PointTrack> spawned = tracker.getNewTracks(null);

		// estimate 3D coordinate using stereo vision
		for( PointTrack t : spawned ) {
			for (int i = 0; i < visibleTracks.size(); i++) {
				if( visibleTracks.get(i).trackerTrack == t ) {
					throw new RuntimeException("Bug. Adding duplicate track: " + visibleTracks.get(i).id + " " + t.featureId);
				}
			}

			// discard point if it can't localized
			if( !pixelTo3D.process(t.pixel.x,t.pixel.y) || pixelTo3D.getW() == 0 ) { // TODO don't drop infinity
//				System.out.println("Dropped pixelTo3D  tt="+t.featureId);
				tracker.dropTrack(t);
			} else {
				if( bundle.findByTrackerTrack(t) != null ) {
					Track btrack = bundle.findByTrackerTrack(t);
					System.out.println("BUG! Tracker recycled... bt="+btrack.id+" tt="+t.featureId);
					throw new RuntimeException("BUG! Recycled tracker track too early tt="+t.featureId);
				}
				// Save the track's 3D location and add it to the current frame
				Track btrack = bundle.addTrack(pixelTo3D.getX(),pixelTo3D.getY(),pixelTo3D.getZ(),pixelTo3D.getW());
				btrack.lastUsed = frameID;
				btrack.trackerTrack = t;
				btrack.id = t.featureId;
				t.cookie = btrack;

//				System.out.println("new track bt="+btrack.id+" tt.id="+t.featureId);

				// Convert the location from local coordinate system to world coordinates
				SePointOps_F64.transform(frameCurrent.frame_to_world,btrack.worldLoc,btrack.worldLoc);
				// keep the scale of floats manageable and normalize the vector to have a norm of 1
				// Homogeneous coordinates so the distance is determined by the ratio of w and other elements
				btrack.worldLoc.normalize();

				bundle.addObservation(frameCurrent, btrack, t.pixel.x , t.pixel.y);

				for (int i = 0; i < visibleTracks.size(); i++) {
					if( visibleTracks.get(i).trackerTrack == t )
						throw new RuntimeException("Bug. Adding duplicate track: "+t.featureId);
				}

				visibleTracks.add(btrack);
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

		var prevLoc4 = new Point4D_F64();
		var world_to_prev = new Se3_F64();
		framePrevious.frame_to_world.invert(world_to_prev);

		// Create a list of observations for PnP
		// normalized image coordinates and 3D in the previous keyframe's reference frame
		observationsPnP.reset();
		for( PointTrack pt : active ) {
			// Build the list of tracks which are currently visible
			initialVisible.add((Track)pt.cookie);

			// Extract info needed to estimate motion
			Point2D3D p = observationsPnP.grow();
			pixelToNorm.compute( pt.pixel.x , pt.pixel.y , p.observation );
			Track bt = pt.getCookie();

			// Go from world coordinates to the previous frame
			SePointOps_F64.transform(world_to_prev,bt.worldLoc,prevLoc4);

			// Go from homogenous coordinates into 3D coordinates
			double w = prevLoc4.w;
			if( w != 0.0)
				p.location.set( prevLoc4.x/w, prevLoc4.y/w, prevLoc4.z/w);
			else {
				// it's off at infinity. There is no great option here. Let's just make it really far away
				double n = prevLoc4.norm();
				if( n == 0.0 )
					n = 1.0;
				// the appropriate value for "far away" depends on units. This might not always work
				double scale = 1e6/n;
				// it was observed so it has to be in front of the camera
				if( prevLoc4.z < 0 )
					scale *= -1;
				p.location.set(prevLoc4.x*scale, prevLoc4.y*scale, prevLoc4.z*scale);
			}
		}

		// estimate the motion up to a scale factor in translation
		if( !motionEstimator.process( observationsPnP.toList() ) )
			return false;

		Se3_F64 key_to_current;

		if( refine != null ) {
			key_to_current = new Se3_F64();
			refine.fitModel(motionEstimator.getMatchSet(), motionEstimator.getModelParameters(), key_to_current);
		} else {
			key_to_current = motionEstimator.getModelParameters();
		}

		// Change everything back to the world frame
		key_to_current.invert(current_to_key);
		current_to_key.concat(framePrevious.frame_to_world,frameCurrent.frame_to_world);

		// mark tracks as being inliers and add to inlier list
		int N = motionEstimator.getMatchSet().size();
		long tick = tracker.getFrameID();
		for( int i = 0; i < N; i++ ) {
			int index = motionEstimator.getInputIndex(i);
			PointTrack p = active.get(index);
			Track t = p.getCookie();
			t.lastUsed = tick;
			t.active = true;
			bundle.addObservation(frameCurrent, t, p.pixel.x, p.pixel.y);
			inlierTracks.add( t );
		}

		return true;
	}

	public long getFrameID() {
		return tracker.getFrameID();
	}

	public Se3_F64 getCurrToWorld() {
		return current_to_world;
	}

	public static class Track extends BTrack {
		public long lastUsed;
	}
}
