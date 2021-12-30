/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.abst.geo.Triangulate2ViewsMetric;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.feature.associate.StereoConsistencyCheck;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Stereo visual odometry algorithm which relies on tracking features independently in the left and right images
 * and then matching those tracks together. The idea behind this tracker is that the expensive task of association
 * features between left and right cameras only needs to be done once when track is spawned. Triangulation
 * is used to estimate each feature's 3D location. Motion is estimated robustly using a RANSAC type algorithm
 * provided by the user which internally uses {@link boofcv.abst.geo.Estimate1ofPnP PnP} type algorithm.
 *
 * Estimated motion is relative to left camera.
 *
 * FUTURE WORK: Save visual tracks without stereo matches and do monocular tracking on them. This is useful for stereo
 * systems with only a little bit of overlap.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisOdomDualTrackPnP<T extends ImageBase<T>, TD extends TupleDesc<TD>>
		extends VisOdomBundlePnPBase<VisOdomDualTrackPnP.TrackInfo> {

	// TODO must modify so that tracks can exist in only one camera after the initial spawn. Requiring tracks always
	//      be mutual greatly increases the number of dropped tracks when there is motion blur
	// TODO Apply a rigid constraints in SBA when that feature has been added
	// TODO Add more checks to stereo association. Similar to what's done in greedy now

	// index of the left camera in the camera list
	public static final int CAMERA_LEFT = 0;
	public static final int CAMERA_RIGHT = 1;

	// Left and right input images
	private T inputLeft;
	private T inputRight;

	// computes camera motion
	private @Getter final ModelMatcher<Se3_F64, Stereo2D3D> matcher;
	private @Getter final @Nullable ModelFitter<Se3_F64, Stereo2D3D> modelRefiner;

	// trackers for left and right cameras
	private final PointTracker<T> trackerLeft;
	private final PointTracker<T> trackerRight;
	/** Used to describe tracks so that they can be matches between the two cameras */
	private final DescribePointRadiusAngle<T, TD> describe;
	/** Radius of a descriptor's region */
	private @Getter @Setter double describeRadius = 11.0;

	// Data structures used when associating left and right cameras
	private final FastArray<Point2D_F64> pointsLeft = new FastArray<>(Point2D_F64.class);
	private final FastArray<Point2D_F64> pointsRight = new FastArray<>(Point2D_F64.class);
	private final DogArray<TD> descLeft;
	private final DogArray<TD> descRight;

	// matches features between left and right images
	private final AssociateDescription2D<TD> assocL2R;
	/** Triangulates points from the two stereo correspondences */
	private final Triangulate2ViewsMetric triangulate2;

	//----- Data structures for Bundle Adjustment and Track Information
	private BFrame currentLeft, currentRight;
	private BFrame previousLeft;

	// Ensures that the epipolar constraint still applies to the tracks
	private final StereoConsistencyCheck stereoCheck;

	// known stereo baseline
	private final Se3_F64 world_to_prev = new Se3_F64();

	/** List of tracks from left image that remain after geometric filters have been applied */
	private @Getter final List<PointTrack> candidates = new ArrayList<>();

	// Internal profiling
	private @Getter double timeTracking, timeEstimate, timeBundle, timeDropUnused, timeSceneMaintenance, timeSpawn;

	//---------------------------------------------------------------------------------------------------
	//----------- Internal Work Space
	DogArray<Stereo2D3D> listStereo2D3D = new DogArray<>(Stereo2D3D::new);

	private final Se3_F64 left_to_right = new Se3_F64();
	private final Se3_F64 right_to_left = new Se3_F64();
	Point4D_F64 prevLoc4 = new Point4D_F64();

	// storage for the triangulated location in the camera frame
	Point3D_F64 cameraP3 = new Point3D_F64();
	// Normalized image coordinate for pixel track observations
	Point2D_F64 normLeft = new Point2D_F64();
	Point2D_F64 normRight = new Point2D_F64();

	/**
	 * Specifies internal algorithms and parameters
	 *
	 * @param epilolarTol Tolerance in pixels for enforcing the epipolar constraint
	 * @param trackerLeft Tracker used for left camera
	 * @param trackerRight Tracker used for right camera
	 * @param describe Describes features in tracks
	 * @param assocL2R Assocation for left to right
	 * @param triangulate2 Triangulation for estimating 3D location from stereo pair
	 * @param matcher Robust motion model estimation with outlier rejection
	 * @param modelRefiner Non-linear refinement of motion model
	 */
	public VisOdomDualTrackPnP( double epilolarTol,
								PointTracker<T> trackerLeft, PointTracker<T> trackerRight,
								DescribePointRadiusAngle<T, TD> describe,
								AssociateDescription2D<TD> assocL2R,
								Triangulate2ViewsMetric triangulate2,
								ModelMatcher<Se3_F64, Stereo2D3D> matcher,
								@Nullable ModelFitter<Se3_F64, Stereo2D3D> modelRefiner ) {
		if (!assocL2R.uniqueSource() || !assocL2R.uniqueDestination())
			throw new IllegalArgumentException("Both unique source and destination must be ensure by association");

		this.describe = describe;
		this.trackerLeft = trackerLeft;
		this.trackerRight = trackerRight;
		this.assocL2R = assocL2R;
		this.triangulate2 = triangulate2;
		this.matcher = matcher;
		this.modelRefiner = modelRefiner;

		descLeft = new DogArray<>(describe::createDescription);
		descRight = new DogArray<>(describe::createDescription);

		stereoCheck = new StereoConsistencyCheck(epilolarTol, epilolarTol);

		bundleViso = new VisOdomBundleAdjustment<>(TrackInfo::new);

		// TODO would be best if this reduced pixel error and not geometric error
		// TODO remove and replace with calibrated homogenous coordinates when it exists
		ConfigTriangulation config = new ConfigTriangulation();
		config.type = ConfigTriangulation.Type.GEOMETRIC;
		config.converge.maxIterations = 10;
		triangulateN = FactoryMultiView.triangulateNViewMetric(config);
	}

	/**
	 * Specifies the stereo parameters. Note that classes which are passed into constructor are maintained outside.
	 * Example, the RANSAC distance model might need to have stereo parameters passed to it externally
	 * since there's no generic way to handle that.
	 */
	public void setCalibration( StereoParameters param ) {
		right_to_left.setTo(param.right_to_left);
		param.right_to_left.invert(left_to_right);

		CameraModel left = new CameraModel();
		left.pixelToNorm = LensDistortionFactory.narrow(param.left).undistort_F64(true, false);
		CameraModel right = new CameraModel();
		right.pixelToNorm = LensDistortionFactory.narrow(param.right).undistort_F64(true, false);

		stereoCheck.setCalibration(param);
		cameraModels.add(left);
		cameraModels.add(right);
		bundleViso.addCamera(param.left);
		bundleViso.addCamera(param.right);
	}

	/**
	 * Resets the algorithm into its original state
	 */
	@Override
	public void reset() {
		super.reset();
		trackerLeft.reset();
		trackerRight.reset();
	}

	/**
	 * Updates motion estimate using the stereo pair.
	 *
	 * @param left Image from left camera
	 * @param right Image from right camera
	 * @return true if motion estimate was updated and false if not
	 */
	public boolean process( T left, T right ) {
		if (verbose != null) {
			verbose.println("----------- Process --------------");
			verbose.println("Scene: Frames=" + bundleViso.frames.size + " Tracks=" + bundleViso.tracks.size);
			for (int frameIdx = 0; frameIdx < bundleViso.frames.size; frameIdx++) {
				BFrame bf = bundleViso.frames.get(frameIdx);
				verbose.printf("   frame[%2d] cam=%d tracks=%d\n", frameIdx, bf.camera.index, bf.tracks.size);
			}
		}
		this.inputLeft = left;
		this.inputRight = right;

		//=============================================================================================
		//========== Visually track features
		double time0 = System.nanoTime();
		inlierTracks.clear();
		visibleTracks.clear();
		initialVisible.clear();
		candidates.clear();

		// Create a new frame for the current image
		currentLeft = bundleViso.addFrame(CAMERA_LEFT, trackerLeft.getFrameID());
		currentRight = bundleViso.addFrame(CAMERA_RIGHT, trackerRight.getFrameID());
		// TODO in the future when bundle adjustment supports rigid relationships between two views use that here

		// Track objects given the new images
		trackerLeft.process(left);
		trackerRight.process(right);
		double time1 = System.nanoTime();

		//=============================================================================================
		//========== Initialize VO from the first image and return
		if (first) {
			first = false;
			frameManager.initialize(bundleViso.cameras);
			addNewTracks();
			// The left camera is the world frame right now
			currentLeft.frame_to_world.reset();
			currentRight.frame_to_world.setTo(right_to_left);
			return true;
		}

		// This will be used as a reference for motion estimation
		// tail(3) since the two visible frames (left + right) where just added
		previousLeft = bundleViso.frames.getTail(3);

		// If one tracker dropped a track then drop the same track in the other camera
		mutualTrackDrop();
		// Find tracks which pass a geometric test and put into candidates list
		selectCandidateStereoTracks();
		// Robustly estimate motion using features in candidates list
		if (!estimateMotion()) {
			if (verbose != null) verbose.println("!!! Motion Failed !!!");
			removedBundleTracks.clear();
			bundleViso.removeFrame(currentRight, removedBundleTracks);
			bundleViso.removeFrame(currentLeft, removedBundleTracks);
			return false;
		}

		addInlierObservationsToScene();
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
		boolean droppedCurrentFrame = performKeyFrameMaintenance(trackerLeft, 2);
		long time5 = System.nanoTime();
		if (!droppedCurrentFrame) {
			if (verbose != null) verbose.println("Saving new key frames");
			// We are keeping the current frame! Spawn new tracks inside of it
			addNewTracks();
		}
		long time6 = System.nanoTime();

		//=============================================================================================
		//========== Summarize profiling results
		timeTracking = (time1 - time0)*1e-6;
		timeEstimate = (time2 - time1)*1e-6;
		timeBundle = (time3 - time2)*1e-6;
		timeDropUnused = (time4 - time3)*1e-6;
		timeSceneMaintenance = (time5 - time4)*1e-6;
		timeSpawn = (time6 - time5)*1e-6;

		if (profileOut != null) {
			double timeTotal = (time6 - time0)*1e-6;
			profileOut.printf("TIME: TRK %5.1f Est %5.1f Bun %5.1f DU %5.1f Scene %5.1f Spn  %5.1f TOTAL %5.1f\n",
					timeTracking, timeEstimate, timeBundle, timeDropUnused, timeSceneMaintenance, timeSpawn, timeTotal);
		}

		return true;
	}

	/**
	 * Runs bundle adjustment and update the state of views and features
	 */
	private void optimizeTheScene() {
		// Update the state estimate
		if (bundleViso.isOptimizeActive()) {
			bundleViso.optimize(verbose);
			triangulateNotSelectedBundleTracks();
		}
		// Save the output
		current_to_world.setTo(currentLeft.frame_to_world);
	}

	/**
	 * Given the set of active tracks, estimate the cameras motion robustly
	 *
	 * @return true if successful
	 */
	private boolean estimateMotion() {
		CameraModel leftCM = cameraModels.get(CAMERA_LEFT);
		CameraModel rightCM = cameraModels.get(CAMERA_RIGHT);

		// Perform motion estimation relative to the most recent key frame
		previousLeft.frame_to_world.invert(world_to_prev);

		// Put observation and prior knowledge into a format the model matcher will understand
		listStereo2D3D.reserve(candidates.size());
		listStereo2D3D.reset();
		for (int candidateIdx = 0; candidateIdx < candidates.size(); candidateIdx++) {
			PointTrack l = candidates.get(candidateIdx);
			Stereo2D3D stereo = listStereo2D3D.grow();

			// Get the track location
			TrackInfo bt = l.getCookie();
			PointTrack r = bt.visualRight;

			// Get the 3D coordinate of the point in the 'previous' frame
			SePointOps_F64.transform(world_to_prev, bt.worldLoc, prevLoc4);
			PerspectiveOps.homogenousTo3dPositiveZ(prevLoc4, 1e8, 1e-8, stereo.location);

			// compute normalized image coordinate for track in left and right image
			leftCM.pixelToNorm.compute(l.pixel.x, l.pixel.y, stereo.leftObs);
			rightCM.pixelToNorm.compute(r.pixel.x, r.pixel.y, stereo.rightObs);
			// TODO Could this transform be done just once?
		}

		// Robustly estimate left camera motion
		if (!matcher.process(listStereo2D3D.toList()))
			return false;

		if (modelRefiner != null) {
			modelRefiner.fitModel(matcher.getMatchSet(), matcher.getModelParameters(), previous_to_current);
		} else {
			previous_to_current.setTo(matcher.getModelParameters());
		}

		// Convert the found transforms back to world
		previous_to_current.invert(current_to_previous);
		current_to_previous.concat(previousLeft.frame_to_world, currentLeft.frame_to_world);
		right_to_left.concat(currentLeft.frame_to_world, currentRight.frame_to_world);

		return true;
	}

	private void addInlierObservationsToScene() {
		// mark tracks that are in the inlier set and add their observations to the scene
		int N = matcher.getMatchSet().size();
		if (verbose != null) verbose.println("Total Inliers " + N + " / " + candidates.size());
		for (int i = 0; i < N; i++) {
			int index = matcher.getInputIndex(i);
			TrackInfo bt = candidates.get(index).getCookie();
			if (bt.visualTrack == null) throw new RuntimeException("BUG!");
			bt.lastInlier = getFrameID();
			bt.hasBeenInlier = true;

			PointTrack l = bt.visualTrack;
			PointTrack r = bt.visualRight;

			bundleViso.addObservation(currentLeft, bt, l.pixel.x, l.pixel.y);
			bundleViso.addObservation(currentRight, bt, r.pixel.x, r.pixel.y);

			inlierTracks.add(bt);
		}
	}

	/**
	 * If a track was dropped in one image make sure it was dropped in the other image
	 */
	private void mutualTrackDrop() {
		int total = 0;
		for (PointTrack t : trackerLeft.getDroppedTracks(null)) { // lint:forbidden ignore_line
			TrackInfo bt = t.getCookie();
			trackerRight.dropTrack(bt.visualRight);
			bt.visualTrack = null; // This tells the scene that it is no longer in the visual tracker
			total++;
		}
		for (PointTrack t : trackerRight.getDroppedTracks(null)) { // lint:forbidden ignore_line
			TrackInfo bt = t.getCookie();
			if (bt.visualTrack != null) {
				trackerLeft.dropTrack(bt.visualTrack);
				bt.visualTrack = null;
				total++;
			}
		}
		if (verbose != null) verbose.println("Dropped Tracks Mutual: " + total);
	}

	/**
	 * Searches for tracks which are active and meet the epipolar constraints
	 */
	private void selectCandidateStereoTracks() {
		final long frameID = getFrameID();
		// mark tracks in right frame that are active
		List<PointTrack> activeRight = trackerRight.getActiveTracks(null);
		for (PointTrack t : activeRight) { // lint:forbidden ignore_line
			TrackInfo bt = t.getCookie();
			// If the visual track is null then it got dropped earlier
			if (bt.visualTrack == null)
				continue;
			bt.lastSeenRightFrame = frameID;
			initialVisible.add(bt);
		}

		List<PointTrack> activeLeft = trackerLeft.getActiveTracks(null);
		candidates.clear();
		for (PointTrack left : activeLeft) { // lint:forbidden ignore_line
			TrackInfo bt = left.getCookie();

			if (bt.lastSeenRightFrame != frameID) {
				continue;
			}

			if (bt.visualTrack == null)
				throw new RuntimeException("BUG!!! Should have been skipped over in the right camera");

			// check epipolar constraint and see if it is still valid
			if (stereoCheck.checkPixel(bt.visualTrack.pixel, bt.visualRight.pixel)) {
				bt.lastStereoFrame = frameID;
				candidates.add(left);
			}
		}

		if (verbose != null)
			verbose.println("Visual Tracks: Left: " + activeLeft.size() + " Right: " + activeRight.size() + " Candidates: " + candidates.size());
	}

	/**
	 * Removes tracks which have not been included in the inlier set recently from the visual tracker
	 */
	private void removeOldUnusedVisibleTracks() {
		long currentFrameID = getFrameID();

		// Drop unused tracks from the left camera
		trackerLeft.dropTracks(track -> {
			TrackInfo bt = track.getCookie();
			if (bt == null) throw new RuntimeException("BUG!");
			if (currentFrameID - bt.lastInlier >= thresholdRetireTracks) {
//				System.out.println("Removing visible track due to lack of inlier");
				bt.visualTrack = null;
				return true;
			}
			return false;
		});

		// remove unused tracks from the right camera. Since the tracks are coupled
		// there should be no surprised here
		trackerRight.dropTracks(track -> {
			TrackInfo bt = track.getCookie();
			if (bt == null) throw new RuntimeException("BUG!");
			if (bt.visualTrack == null)
				return true;
			if (currentFrameID - bt.lastInlier >= thresholdRetireTracks) {
				throw new RuntimeException("BUG! Should have already been dropped by left camera");
			}
			return false;
		});
	}

	/**
	 * Spawns tracks in each image and associates features together.
	 */
	private void addNewTracks() {
		CameraModel leftCM = cameraModels.get(CAMERA_LEFT);
		CameraModel rightCM = cameraModels.get(CAMERA_RIGHT);

		final long frameID = getFrameID();
		trackerLeft.spawnTracks();
		trackerRight.spawnTracks();

		List<PointTrack> spawnedLeft = trackerLeft.getNewTracks(null);
		List<PointTrack> spawnedRight = trackerRight.getNewTracks(null);

		// get a list of new tracks and their descriptions
		describeSpawnedTracks(inputLeft, spawnedLeft, pointsLeft, descLeft);
		describeSpawnedTracks(inputRight, spawnedRight, pointsRight, descRight);

		// associate using L2R
		assocL2R.setSource(pointsLeft, descLeft);
		assocL2R.setDestination(pointsRight, descRight);
		assocL2R.associate();
		FastAccess<AssociatedIndex> matches = assocL2R.getMatches();

		int total = 0;
		for (int i = 0; i < matches.size; i++) {
			AssociatedIndex m = matches.get(i);

			PointTrack trackL = spawnedLeft.get(m.src);
			PointTrack trackR = spawnedRight.get(m.dst);

			TrackInfo bt = bundleViso.tracks.grow();

			// convert pixel observations into normalized image coordinates
			leftCM.pixelToNorm.compute(trackL.pixel.x, trackL.pixel.y, normLeft);
			rightCM.pixelToNorm.compute(trackR.pixel.x, trackR.pixel.y, normRight);

			// triangulate 3D coordinate in the current camera frame
			if (triangulate2.triangulate(normLeft, normRight, left_to_right, cameraP3)) {
				// put the track into the world coordinate system
				SePointOps_F64.transform(currentLeft.frame_to_world, cameraP3, cameraP3);
				bt.worldLoc.setTo(cameraP3.x, cameraP3.y, cameraP3.z, 1.0);

				// Finalize the track data structure
				bt.id = trackL.featureId;
				bt.visualTrack = trackL;
				bt.visualRight = trackR;
				bt.lastStereoFrame = bt.lastSeenRightFrame = frameID;
				trackL.cookie = bt;
				trackR.cookie = bt;

				bundleViso.addObservation(currentLeft, bt, trackL.pixel.x, trackL.pixel.y);
				bundleViso.addObservation(currentRight, bt, trackR.pixel.x, trackR.pixel.y);

				visibleTracks.add(bt);
				total++;
			} else {
				// triangulation failed, drop track
				trackerLeft.dropTrack(trackL);
				trackerRight.dropTrack(trackR);
				bundleViso.tracks.removeTail();
			}
		}
		if (verbose != null)
			verbose.println("New Tracks: left=" + spawnedLeft.size() + " right=" + spawnedRight.size() + " stereo=" + total);

		// drop visual tracks that were not associated
		DogArray_I32 unassignedRight = assocL2R.getUnassociatedDestination();
		for (int i = 0; i < unassignedRight.size; i++) {
			int index = unassignedRight.get(i);
			trackerRight.dropTrack(spawnedRight.get(index));
		}
		DogArray_I32 unassignedLeft = assocL2R.getUnassociatedSource();
		for (int i = 0; i < unassignedLeft.size; i++) {
			int index = unassignedLeft.get(i);
			trackerLeft.dropTrack(spawnedLeft.get(index));
		}

		// Let the frame manager know how many tracks were just spawned
		frameManager.handleSpawnedTracks(trackerLeft, bundleViso.cameras.get(CAMERA_LEFT));
		frameManager.handleSpawnedTracks(trackerRight, bundleViso.cameras.get(CAMERA_RIGHT));
	}

	/**
	 * Given list of new visual tracks, describe the region around each track using a descriptor
	 */
	private void describeSpawnedTracks( T image, List<PointTrack> tracks,
										FastArray<Point2D_F64> points, DogArray<TD> descs ) {
		describe.setImage(image);
		points.reset();
		descs.reset();

		for (int i = 0; i < tracks.size(); i++) {
			PointTrack t = tracks.get(i);
			// ignoring the return value. most descriptors never return false and the ones that due will rarely do so
			describe.process(t.pixel.x, t.pixel.y, 0, describeRadius, descs.grow());

			points.add(t.pixel);
		}
	}

	@Override
	public long getFrameID() {
		return trackerLeft.getFrameID();
	}

	/**
	 * If there are no candidates then a fault happened.
	 *
	 * @return true if fault. false is no fault
	 */
	public boolean isFault() {
		return candidates.isEmpty();
	}

	@Override
	protected void dropVisualTrack( PointTrack left ) {
		TrackInfo info = left.getCookie();
		PointTrack right = info.visualRight;
		trackerLeft.dropTrack(left);
		trackerRight.dropTrack(right);
	}

	/**
	 * A coupled track between the left and right cameras.
	 */
	@SuppressWarnings({"NullAway.Init"})
	public static class TrackInfo extends BTrack {
		// Image based tracks in left and right camera
		public PointTrack visualRight;
		public long lastStereoFrame;
		// last time it was in the inlier list
		public long lastInlier;
		// the last frame it was seen in
		public long lastSeenRightFrame;

		@SuppressWarnings({"NullAway"})
		@Override public void reset() {
			super.reset();
			visualRight = null;
			lastStereoFrame = -1;
			lastInlier = -1;
			lastSeenRightFrame = -1;
		}
	}
}
