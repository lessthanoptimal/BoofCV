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
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;

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
public class VisOdomPixelDepthPnP<T extends ImageBase<T>>
		extends VisOdomBundlePnPBase<VisOdomPixelDepthPnP.Track> {

	// tracks features in the image
	private final @Getter PointTracker<T> tracker;
	/** used to estimate a feature's 3D position from image range data */
	private final @Getter ImagePixelTo3D pixelTo3D;

	// non-linear refinement of pose estimate
	private final RefinePnP refine;

	private BFrame frameCurrent;
	private BFrame framePrevious;

	// estimate the camera motion up to a scale factor from two sets of point correspondences
	private final ModelMatcher<Se3_F64, Point2D3D> motionEstimator;
	private final FastQueue<Point2D3D> observationsPnP = new FastQueue<>(Point2D3D::new);

	// Internal profiling
	private @Getter double timeTracking,timeEstimate,timeBundle,timeDropUnused, timeSceneMaintenance,timeSpawn;

	//=================================================================
	//======== Workspace Variables
	List<PointTrack> tmpVisualTracks = new ArrayList<>();
	Point4D_F64 prevLoc4 = new Point4D_F64();
	Se3_F64 world_to_prev = new Se3_F64();

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
		this.scene = new VisOdomBundleAdjustment<>(bundleAdjustment,Track::new);

		// TODO would be best if this reduced pixel error and not geometric error
		// TODO remove and replace with calibrated homogenous coordinates when it exists
		ConfigTriangulation config = new ConfigTriangulation();
		config.type = ConfigTriangulation.Type.GEOMETRIC;
		config.optimization.maxIterations = 10;
		triangulateN = FactoryMultiView.triangulateNViewCalibrated(config);
	}

	/**
	 * Resets the algorithm into its original state
	 */
	public void reset() {
		super.reset();
		tracker.reset();
		current_to_previous.reset();
	}

	/**
	 * Estimates the motion given the left camera image.  The latest information required by ImagePixelTo3D
	 * should be passed to the class before invoking this function.
	 *
	 * @param image Camera image.
	 * @return true if successful or false if it failed
	 */
	public boolean process( T image ) {
		timeTracking = timeBundle = timeSceneMaintenance = timeDropUnused = timeEstimate = timeSpawn = 0;

		//=============================================================================================
		//========== Visually track features
		long time0 = System.nanoTime();
		tracker.process(image);
		long time1 = System.nanoTime();
		verbosePrintTrackerSummary();

		//=============================================================================================
		//========== Setup data structures
		initializeForProcess();

		//=============================================================================================
		//========== Initialize VO from the first image and return
		if( first ) {
			if( verbose != null ) verbose.println("VO: First Frame");
			first = false;
			spawnNewTracksForNewKeyFrame(visibleTracks);
			frameManager.initialize(scene.cameras);
			frameManager.handleSpawnedTracks(tracker, scene.cameras.getTail());
			return true;
		}

		//=============================================================================================
		//========== Update the current motion estimate

		// handle tracks that the visual tracker dropped
		handleDroppedVisualTracks();

		// Estimate motion
		List<PointTrack> activeVisualTracks = tracker.getActiveTracks(null);
		if( !estimateMotion(activeVisualTracks) ) {
			if( verbose != null ) verbose.println("VO: estimate motion failed");
			// discard the current frame and attempt to jump over it
			scene.removeFrame(frameCurrent, removedBundleTracks);
			dropRemovedBundleTracks();
			updateListOfVisibleTracksForOutput();
			return false;
		}
		// what the name says and also marks the inliers as inliers
		addObservationsOfInliersToScene(activeVisualTracks);
		// Drop tracker tracks which aren't being used inside of the inlier set
		removeOldUnusedVisibleTracks();

		//=============================================================================================
		//========== Refine the scene's state estimate
		double time2 = System.nanoTime();
		optimizeTheScene();
		double time3 = System.nanoTime();

		//=============================================================================================
		//========== Perform maintenance by dropping elements from the scene
		dropBadBundleTracks();
		long time4 = System.nanoTime();
		boolean droppedCurrentFrame = performKeyFrameMaintenance(tracker,1);
		long time5 = System.nanoTime();
		if( !droppedCurrentFrame ) {
			// it decided to keep the current track. Spawn new tracks in the current frame
			spawnNewTracksForNewKeyFrame(visibleTracks);
			frameManager.handleSpawnedTracks(tracker, scene.cameras.getTail());
		}
		long time6 = System.nanoTime();

		//=============================================================================================
		//========== Summarize profiling results
		timeTracking = (time1-time0)*1e-6;
		timeEstimate = (time2-time1)*1e-6;
		timeBundle = (time3-time2)*1e-6;
		timeDropUnused = (time4-time3)*1e-6;
		timeSceneMaintenance = (time5-time4)*1e-6;
		timeSpawn = (time6-time5)*1e-6;

		if( profileOut != null ) {
			double timeTotal = (time6-time0)*1e-6;
			profileOut.printf("StereoVO: TRK %5.1f Est %5.1f Bun %5.1f DU %5.1f Scene %5.1f Spn  %5.1f TOTAL %5.1f\n",
					timeTracking, timeEstimate, timeBundle, timeDropUnused, timeSceneMaintenance,timeSpawn,timeTotal);
		}

//		bundle.sanityCheck();

		return true;
	}

	private void initializeForProcess() {
		inlierTracks.clear();
		visibleTracks.clear();
		initialVisible.clear();

		// Previous key frame is the most recently added one, which is the last
		framePrevious = first ? null : scene.getLastFrame();
		// Create a new frame for the current image
		frameCurrent = scene.addFrame(tracker.getFrameID());
	}

	private void verbosePrintTrackerSummary() {
		if( verbose != null ) {
			verbose.println("-----------------------------------------------------------------------------------------");
			verbose.println("Input Frame Count   " + tracker.getFrameID());
			verbose.println("   Bundle Frames    " + scene.frames.size);
			verbose.println("   Bundle tracks    " + scene.tracks.size);
			verbose.println("   Tracker active   " + tracker.getTotalActive());
			verbose.println("   Tracker inactive " + tracker.getTotalInactive());
		}
	}

	/**
	 * Runs bundle adjustment and update the state of views and features
	 */
	private void optimizeTheScene() {
		// Update the state estimate
		scene.optimize();
		// Save the output
		current_to_world.set(frameCurrent.frame_to_world);
		triangulateNotSelectedBundleTracks();
	}

	private void handleDroppedVisualTracks() {
		tmpVisualTracks.clear();
		tracker.getDroppedTracks(tmpVisualTracks);
		for (int i = 0; i < tmpVisualTracks.size(); i++) {
			// Tell the bundle track that they are no longer associated with a visual track
			BTrack bt = tmpVisualTracks.get(i).getCookie();
			bt.visualTrack = null;
		}
	}

	// TODO comments
	private void dropRemovedBundleTracks() {
		for (int i = 0; i < removedBundleTracks.size(); i++) {
			dropVisualTrack(removedBundleTracks.get(i));
		}
	}

	@Override
	protected void dropVisualTrack(BTrack track) {
		if( track.visualTrack != null )
			tracker.dropTrack(track.visualTrack);
	}

	/**
	 * Sets the known fixed camera parameters
	 */
	public void setCamera( CameraPinholeBrown camera ) {
		scene.cameras.reset();
		scene.addCamera(camera);
		LensDistortionNarrowFOV factory = LensDistortionFactory.narrow(camera);

		CameraModel cm = new CameraModel();
		cm.pixelToNorm = factory.undistort_F64(true,false);
		cm.normToPixel = factory.distort_F64(false,true);
		cameraModels.clear();
		cameraModels.add(cm);
	}

	private void addObservationsOfInliersToScene(List<PointTrack> active) {
		// mark tracks as being inliers and add to inlier list
		int N = motionEstimator.getMatchSet().size();
		long frameID = getFrameID();
		for( int i = 0; i < N; i++ ) {
			int index = motionEstimator.getInputIndex(i);
			PointTrack p = active.get(index);
			Track t = p.getCookie();
			t.lastUsed = frameID;
			t.inlier = true;
			scene.addObservation(frameCurrent, t, p.pixel.x, p.pixel.y);
			inlierTracks.add( t );
		}
	}

	/**
	 * Looks at tracks in the current frame. Sees if they have not been used in a while. If so they are
	 * dropped from the visible tracker.
	 */
	private void removeOldUnusedVisibleTracks() {
		final long trackerFrame = getFrameID();

		// This will go through all tracks, active and inactive
		tracker.dropTracks(track -> {
			Track bt = track.getCookie();
			if( bt == null ) throw new RuntimeException("BUG!");
			if( trackerFrame - bt.lastUsed >= thresholdRetireTracks) {
				bt.visualTrack = null;
				return true;
			}
			return false;
		});
	}

	/**
	 * Detects new features and computes their 3D coordinates
	 *
	 * @param visibleTracks newly spawned tracks are added to this list
	 */
	private void spawnNewTracksForNewKeyFrame(List<Track> visibleTracks ) {
//		System.out.println("addNewTracks() current frame="+frameCurrent.id);

		long frameID = tracker.getFrameID();

		tracker.spawnTracks();
		List<PointTrack> spawned = tracker.getNewTracks(null);

		// TODO make this optionally concurrent
		// estimate 3D coordinate using stereo vision
		for( PointTrack t : spawned ) {
			for (int i = 0; i < visibleTracks.size(); i++) {
				if( visibleTracks.get(i).visualTrack == t ) {
					throw new RuntimeException("Bug. Adding duplicate track: " + visibleTracks.get(i).id + " " + t.featureId);
				}
			}

			// discard point if it can't localized
			if( !pixelTo3D.process(t.pixel.x,t.pixel.y) || pixelTo3D.getW() == 0 ) { // TODO don't drop infinity
//				System.out.println("Dropped pixelTo3D  tt="+t.featureId);
				tracker.dropTrack(t);
			} else {
				if( scene.findByTrackerTrack(t) != null ) {
					Track btrack = scene.findByTrackerTrack(t);
					System.out.println("BUG! Tracker recycled... bt="+btrack.id+" tt="+t.featureId);
					throw new RuntimeException("BUG! Recycled tracker track too early tt="+t.featureId);
				}
				// Save the track's 3D location and add it to the current frame
				Track btrack = scene.addTrack(pixelTo3D.getX(),pixelTo3D.getY(),pixelTo3D.getZ(),pixelTo3D.getW());
				btrack.lastUsed = frameID;
				btrack.visualTrack = t;
				btrack.id = t.featureId;
				t.cookie = btrack;

//				System.out.println("new track bt="+btrack.id+" tt.id="+t.featureId);

				// Convert the location from local coordinate system to world coordinates
				SePointOps_F64.transform(frameCurrent.frame_to_world,btrack.worldLoc,btrack.worldLoc);
				// keep the scale of floats manageable and normalize the vector to have a norm of 1
				// Homogeneous coordinates so the distance is determined by the ratio of w and other elements
				btrack.worldLoc.normalize();

				scene.addObservation(frameCurrent, btrack, t.pixel.x , t.pixel.y);

				for (int i = 0; i < visibleTracks.size(); i++) {
					if( visibleTracks.get(i).visualTrack == t )
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
	private boolean estimateMotion( List<PointTrack> active ) {
		Point2Transform2_F64 pixelToNorm = cameraModels.get(0).pixelToNorm;

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
			PerspectiveOps.homogenousTo3dPositiveZ(prevLoc4,1e8,1e-7,p.location);
		}

		// estimate the motion up to a scale factor in translation
		if( !motionEstimator.process( observationsPnP.toList() ) )
			return false;

		Se3_F64 previous_to_current;

		if( refine != null ) {
			previous_to_current = new Se3_F64();
			refine.fitModel(motionEstimator.getMatchSet(), motionEstimator.getModelParameters(), previous_to_current);
		} else {
			previous_to_current = motionEstimator.getModelParameters();
		}

		// Change everything back to the world frame
		previous_to_current.invert(current_to_previous);
		current_to_previous.concat(framePrevious.frame_to_world,frameCurrent.frame_to_world);

		return true;
	}

	@Override
	public long getFrameID() {return tracker.getFrameID();}

	public static class Track extends BTrack {
		// last frame ID that this track was used to estimate the state of a visual frame
		public long lastUsed;
	}
}
