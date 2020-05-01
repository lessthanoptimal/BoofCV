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

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.geo.Triangulate2ViewsMetric;
import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneStructureMetric;
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
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * Stereo visual odometry algorithm which relies on tracking features independently in the left and right images
 * and then matching those tracks together.  The idea behind this tracker is that the expensive task of association
 * features between left and right cameras only needs to be done once when track is spawned.  Triangulation
 * is used to estimate each feature's 3D location.  Motion is estimated robustly using a RANSAC type algorithm
 * provided by the user which internally uses {@link boofcv.abst.geo.Estimate1ofPnP PnP} type algorithm.
 *
 * Estimated motion is relative to left camera.
 *
 * FUTURE WORK: Save visual tracks without stereo matches and do monocular tracking on them. This is useful for stereo
 *              systems with only a little bit of overlap.
 *
 * @author Peter Abeles
 */
public class VisOdomDualTrackPnP<T extends ImageBase<T>,Desc extends TupleDesc>
		extends VisOdomBundlePnPBase<VisOdomDualTrackPnP.TrackInfo> {

	// index of the left camera in the camera list
	public static final int CAMERA_LEFT = 0;
	public static final int CAMERA_RIGHT = 1;

	// Left and right input images
	private T inputLeft;
	private T inputRight;

	// when the inlier set is less than this number new features are detected
	private final int thresholdAdd;

	// discard tracks after they have not been in the inlier set for this many updates in a row
	private final int thresholdRetire;

	// computes camera motion
	private @Getter final ModelMatcher<Se3_F64, Stereo2D3D> matcher;
	private @Getter final ModelFitter<Se3_F64, Stereo2D3D> modelRefiner;

	// trackers for left and right cameras
	private final PointTracker<T> trackerLeft;
	private final PointTracker<T> trackerRight;
	/** Used to describe tracks so that they can be matches between the two cameras */
	private final DescribeRegionPoint<T,Desc> describe;
	/** Radius of a descriptor's region */
	private @Getter @Setter double describeRadius=11.0;

	// Data structures used when associating left and right cameras
	private final FastArray<Point2D_F64> pointsLeft = new FastArray<>(Point2D_F64.class);
	private final FastArray<Point2D_F64> pointsRight = new FastArray<>(Point2D_F64.class);
	private final FastQueue<Desc> descLeft;
	private final FastQueue<Desc> descRight;

	// matches features between left and right images
	private final AssociateDescription2D<Desc> assocL2R;
	/** Triangulates points from the two stereo correspondences */
	private final Triangulate2ViewsMetric triangulate2;

	//----- Data structures for Bundle Adjustment and Track Information
	private BFrame currentLeft, currentRight;

	// Ensures that the epipolar constraint still applies to the tracks
	private final StereoConsistencyCheck stereoCheck;

	// known stereo baseline
	private final Se3_F64 left_to_right = new Se3_F64();

	/** List of tracks from left image that remain after geometric filters have been applied */
	private @Getter final List<PointTrack> candidates = new ArrayList<>();

	// transform from key frame to world frame
	private final Se3_F64 key_to_world = new Se3_F64();

	//---------------------------------------------------------------------------------------------------
	//----------- Internal Work Space
	FastQueue<Stereo2D3D> listStereo2D3D = new FastQueue<>(Stereo2D3D::new);

	/**
	 * Specifies internal algorithms and parameters
	 *
	 * @param thresholdAdd When the number of inliers is below this number new features are detected
	 * @param thresholdRetire When a feature has not been in the inlier list for this many ticks it is dropped
	 * @param epilolarTol Tolerance in pixels for enforcing the epipolar constraint
	 * @param trackerLeft Tracker used for left camera
	 * @param trackerRight Tracker used for right camera
	 * @param describe Describes features in tracks
	 * @param assocL2R Assocation for left to right
	 * @param triangulate2 Triangulation for estimating 3D location from stereo pair
	 * @param matcher Robust motion model estimation with outlier rejection
	 * @param modelRefiner Non-linear refinement of motion model
	 */
	public VisOdomDualTrackPnP(int thresholdAdd, int thresholdRetire, double epilolarTol,
							   PointTracker<T> trackerLeft, PointTracker<T> trackerRight,
							   DescribeRegionPoint<T,Desc> describe,
							   AssociateDescription2D<Desc> assocL2R,
							   Triangulate2ViewsMetric triangulate2,
							   ModelMatcher<Se3_F64, Stereo2D3D> matcher,
							   ModelFitter<Se3_F64, Stereo2D3D> modelRefiner,
							   BundleAdjustment<SceneStructureMetric> bundleAdjustment )
	{
		if( !assocL2R.uniqueSource() || !assocL2R.uniqueDestination() )
			throw new IllegalArgumentException("Both unique source and destination must be ensure by association");

		this.describe = describe;
		this.thresholdAdd = thresholdAdd;
		this.thresholdRetire = thresholdRetire;
		this.trackerLeft = trackerLeft;
		this.trackerRight = trackerRight;
		this.assocL2R = assocL2R;
		this.triangulate2 = triangulate2;
		this.matcher = matcher;
		this.modelRefiner = modelRefiner;

		descLeft = new FastQueue<>(describe::createDescription);
		descRight = new FastQueue<>(describe::createDescription);

		stereoCheck = new StereoConsistencyCheck(epilolarTol,epilolarTol);

		scene = new VisOdomBundleAdjustment<>(bundleAdjustment,TrackInfo::new);

		// TODO would be best if this reduced pixel error and not geometric error
		// TODO remove and replace with calibrated homogenous coordinates when it exists
		ConfigTriangulation config = new ConfigTriangulation();
		config.type = ConfigTriangulation.Type.GEOMETRIC;
		config.optimization.maxIterations = 10;
		triangulateN = FactoryMultiView.triangulateNViewCalibrated(config);
	}

	public void setCalibration(StereoParameters param) {

		param.rightToLeft.invert(left_to_right);

		CameraModel left = new CameraModel();
		left.pixelToNorm = LensDistortionFactory.narrow(param.left).undistort_F64(true,false);
		CameraModel right = new CameraModel();
		right.pixelToNorm = LensDistortionFactory.narrow(param.right).undistort_F64(true,false);

		cameraModels.clear();
		cameraModels.add(left);
		cameraModels.add(right);

		stereoCheck.setCalibration(param);

		scene.addCamera(param.left);
		scene.addCamera(param.right);
	}

	/**
	 * Resets the algorithm into its original state
	 */
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
	public boolean process( T left , T right ) {
//		System.out.println("----------- Process --------------");

		this.inputLeft = left;
		this.inputRight = right;

		// Create a new frame for the current image
		currentRight = scene.addFrame(trackerRight.getFrameID());
		currentLeft  = scene.addFrame(trackerLeft.getFrameID());

		// Track objects given the new images
		trackerLeft.process(left);
		trackerRight.process(right);

		if( first ) {
			first = false;
			frameManager.initialize(scene.cameras);
			addNewTracks();
			return true;
		}

		// if one tracker dropped a track then drop the same track in the other camera
		mutualTrackDrop();
		// Find tracks which are still good
		selectStereoVisualTracks();
		if( !estimateMotion() ) {
			removedBundleTracks.clear();
			scene.removeFrame(currentRight,removedBundleTracks);
			scene.removeFrame(currentLeft,removedBundleTracks);
			return false;
		}
		addInlierObservationsToScene();
		removeOldUnusedVisibleTracks();

		if( modelRefiner != null )
			refineMotionEstimate();

		//=============================================================================================
		//========== Refine the scene's state estimate
		double time2 = System.nanoTime();
		optimizeTheScene();
		double time3 = System.nanoTime();

		//=============================================================================================
		//========== Perform maintenance by dropping elements from the scene
		dropBadBundleTracks();

		long time4 = System.nanoTime();
		boolean droppedCurrentFrame = performKeyFrameMaintenance();
		long time5 = System.nanoTime();
		if( !droppedCurrentFrame ) {
			// We are keeping the current frame! Spawn new tracks inside of it
			addNewTracks();
		}
		long time6 = System.nanoTime();

		return true;
	}

	/**
	 * Runs bundle adjustment and update the state of views and features
	 */
	private void optimizeTheScene() {
		// Update the state estimate
		scene.optimize();
		// Save the output
		current_to_world.set(currentLeft.frame_to_world);
		triangulateNotSelectedBundleTracks();
	}

	private boolean performKeyFrameMaintenance() {
		GrowQueue_I32 dropFrameIndexes = frameManager.selectFramesToDiscard(trackerLeft,maxKeyFrames,2, scene);
		boolean droppedCurrentFrame = false;
		for (int i = dropFrameIndexes.size-1; i >= 0; i--) {
			// indexes are ordered from lowest to highest, so you can remove frames without
			// changing the index in the list
			BFrame frameToDrop = scene.frames.get(dropFrameIndexes.get(i));
			droppedCurrentFrame |= frameToDrop == scene.frames.getTail();

			// update data structures
			scene.removeFrame(frameToDrop, removedBundleTracks);

			// Drop tracks which have been dropped due to their error level
			for (int removeIdx = 0; removeIdx < removedBundleTracks.size(); removeIdx++) {
				dropVisualTrack(removedBundleTracks.get(removeIdx));
			}

			dropTracksNotVisibleAndTooFewObservations();
		}
		updateListOfVisibleTracksForOutput();
		return droppedCurrentFrame;
	}

	/**
	 * Given the set of active tracks, estimate the cameras motion robustly
	 * @return true if successful
	 */
	private boolean estimateMotion() {
		CameraModel leftCM = cameraModels.get(CAMERA_LEFT);
		CameraModel rightCM = cameraModels.get(CAMERA_RIGHT);

		// organize the data
		listStereo2D3D.growArray(candidates.size());
		listStereo2D3D.reset();
		for( PointTrack l : candidates ) {
			Stereo2D3D stereo = listStereo2D3D.grow();

			// Get the track location
			TrackInfo info = l.getCookie();
			PointTrack r = info.visualRight;

			// Get the 3D coordinate of the point
			PerspectiveOps.homogenousTo3dPositiveZ(info.worldLoc,1e8,1e-8,stereo.location);

			// compute normalized image coordinate for track in left and right image
			leftCM.pixelToNorm.compute(l.pixel.x,l.pixel.y,stereo.leftObs);
			rightCM.pixelToNorm.compute(r.pixel.x,r.pixel.y,stereo.rightObs);
		}

		// Robustly estimate left camera motion
		if( !matcher.process(listStereo2D3D.toList()) )
			return false;

		Se3_F64 keyToCurr = matcher.getModelParameters();
		keyToCurr.invert(current_to_key);

		return true;
	}

	private void addInlierObservationsToScene() {
		// mark tracks that are in the inlier set and add their observations to the scene
		int N = matcher.getMatchSet().size();
		for( int i = 0; i < N; i++ ) {
			int index = matcher.getInputIndex(i);
			TrackInfo info = candidates.get(index).getCookie();
			info.lastInlier = getFrameID();
			info.inlier = true;

			PointTrack l = info.visualTrack;
			PointTrack r = info.visualRight;

			scene.addObservation(currentLeft, info, l.pixel.x, l.pixel.y);
			scene.addObservation(currentRight, info, r.pixel.x, r.pixel.y);

			inlierTracks.add( info );
		}
	}

	/**
	 * Non-linear refinement of motion estimate
	 */
	private void refineMotionEstimate() {
		CameraModel leftCM = cameraModels.get(CAMERA_LEFT);
		CameraModel rightCM = cameraModels.get(CAMERA_RIGHT);

		int totalInleirs = matcher.getMatchSet().size();

		// use observations from the inlier set
		listStereo2D3D.growArray(totalInleirs);
		listStereo2D3D.reset();

		for( int i = 0; i < totalInleirs; i++ ) {
			Stereo2D3D stereo = listStereo2D3D.grow();
			int index = matcher.getInputIndex(i);
			TrackInfo info = candidates.get(index).getCookie();

			PointTrack l = info.visualTrack;
			PointTrack r = info.visualRight;

			// compute normalized image coordinate for track in left and right image
			leftCM.pixelToNorm.compute(l.pixel.x,l.pixel.y,stereo.leftObs);
			rightCM.pixelToNorm.compute(r.pixel.x,r.pixel.y,stereo.rightObs);

			// Save the 3D location
			PerspectiveOps.homogenousTo3dPositiveZ(info.worldLoc,1e8,1e-8,stereo.location);
		}

		// refine the motion estimate using non-linear optimization
		Se3_F64 key_to_curr = current_to_key.invert(null);
		Se3_F64 found = new Se3_F64();
		if( modelRefiner.fitModel(listStereo2D3D.toList(),key_to_curr,found) ) {
			found.invert(current_to_key);
		}
	}

	/**
	 * If a track was dropped in one image make sure it was dropped in the other image
	 */
	private void mutualTrackDrop() {
		for( PointTrack t : trackerLeft.getDroppedTracks(null) ) {
			TrackInfo info = t.getCookie();
			trackerRight.dropTrack(info.visualRight);
			info.visualTrack = null; // This tells the scene that it is no longer in the visual tracker
		}
		for( PointTrack t : trackerRight.getDroppedTracks(null) ) {
			TrackInfo info = t.getCookie();
			if( info.visualTrack != null ) {
				trackerLeft.dropTrack(info.visualTrack);
				info.visualTrack = null;
			}
		}
	}

	/**
	 * Searches for tracks which are active and meet the epipolar constraints
	 */
	private void selectStereoVisualTracks() {
		final long frameID = getFrameID();
		// mark tracks in right frame that are active
		List<PointTrack> activeRight = trackerRight.getActiveTracks(null);
		for( PointTrack t : activeRight ) {
			TrackInfo info = t.getCookie();
			info.lastSeenRightFrame = frameID;
		}

		List<PointTrack> activeLeft = trackerLeft.getActiveTracks(null);
		candidates.clear();
		for( PointTrack left : activeLeft ) {
			TrackInfo info = left.getCookie();

			if( info.lastSeenRightFrame != frameID ) {
				continue;
			}

			// check epipolar constraint and see if it is still valid
			if( stereoCheck.checkPixel(info.visualTrack.pixel, info.visualRight.pixel) ) {
				info.lastStereoFrame = frameID;
				candidates.add(left);
			}
		}
	}

	/**
	 * Removes tracks which have not been included in the inlier set recently
	 */
	private void removeOldUnusedVisibleTracks() {
		long currentFramID = getFrameID();

		// Drop unused tracks from the left camera
		trackerLeft.dropTracks(track -> {
			TrackInfo bt = track.getCookie();
			if( bt == null ) throw new RuntimeException("BUG!");
			if( currentFramID - bt.lastInlier >= thresholdRetireTracks) {
				if( bt.inlier ) throw new RuntimeException("BUG! if inlier it just got used");
				bt.visualTrack = null;
				return true;
			}
			return false;
		});

		// remove unused tracks from the right camera. Since the tracks are coupled
		// there should be no surprised here
		trackerRight.dropTracks(track -> {
			TrackInfo bt = track.getCookie();
			if( bt == null ) throw new RuntimeException("BUG!");
			if( bt.visualTrack == null )
				return true;
			if( currentFramID - bt.lastInlier >= thresholdRetireTracks ) {
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
		describeSpawnedTracks(inputRight,spawnedRight,pointsRight,descRight);

		// associate using L2R
		assocL2R.setSource(pointsLeft,descLeft);
		assocL2R.setDestination(pointsRight, descRight);
		assocL2R.associate();
		FastAccess<AssociatedIndex> matches = assocL2R.getMatches();

		// storage for the triangulated location in the camera frame
		Point3D_F64 cameraP3 = new Point3D_F64();
		// Normalized image coordinate for pixel track observations
		Point2D_F64 normLeft = new Point2D_F64();
		Point2D_F64 normRight = new Point2D_F64();

		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex m = matches.get(i);

			PointTrack trackL = spawnedLeft.get(m.src);
			PointTrack trackR = spawnedRight.get(m.dst);

			TrackInfo btrack = scene.tracks.grow();
			btrack.reset();

			// convert pixel observations into normalized image coordinates
			leftCM.pixelToNorm.compute(trackL.pixel.x,trackL.pixel.y,normLeft);
			rightCM.pixelToNorm.compute(trackR.pixel.x,trackR.pixel.y,normRight);

			// triangulate 3D coordinate in the current camera frame
			if( triangulate2.triangulate(normLeft,normRight, left_to_right,cameraP3) )
			{
				// put the track into the current keyframe coordinate system
				SePointOps_F64.transform(current_to_key,cameraP3,cameraP3);
				btrack.worldLoc.set(cameraP3.x, cameraP3.y, cameraP3.z, 1.0);

				// Finalize the track data structure
				btrack.visualTrack = trackL;
				btrack.visualRight = trackR;
				btrack.lastStereoFrame = btrack.lastInlier = frameID;
				trackL.cookie = btrack;
				trackR.cookie = btrack;
			} else {
				// triangulation failed, drop track
				trackerLeft.dropTrack(trackL);
				trackerRight.dropTrack(trackR);
				scene.tracks.removeTail();
			}
		}

		// drop tracks that were not associated
		GrowQueue_I32 unassignedRight = assocL2R.getUnassociatedDestination();
		for( int i = 0; i < unassignedRight.size; i++ ) {
			int index = unassignedRight.get(i);
//			System.out.println(" unassigned right "+newRight.get(index).x+" "+newRight.get(index).y);
			trackerRight.dropTrack(spawnedRight.get(index));
		}
		GrowQueue_I32 unassignedLeft = assocL2R.getUnassociatedSource();
		for( int i = 0; i < unassignedLeft.size; i++ ) {
			int index = unassignedLeft.get(i);
			trackerLeft.dropTrack(spawnedLeft.get(index));
		}

		// Let the frame manager know how many tracks were just spawned
		frameManager.handleSpawnedTracks(trackerLeft, scene.cameras.get(CAMERA_LEFT));
	}

	/**
	 * Given list of new visual tracks, describe the region around each track using a descriptor
	 */
	private void describeSpawnedTracks(T image,  List<PointTrack> tracks ,
									   FastArray<Point2D_F64> points , FastQueue<Desc> descs )
	{
		describe.setImage(image);
		points.reset();
		descs.reset();

		for( int i = 0; i < tracks.size(); i++ ) {
			PointTrack t = tracks.get(i);
			// ignoring the return value.  most descriptors never return false and the ones that due will rarely do so
			describe.process(t.pixel.x,t.pixel.y,0,describeRadius,descs.grow());

			points.add( t.pixel );
		}
	}

	@Override
	public long getFrameID() {
		return trackerLeft.getFrameID();
	}

	/**
	 * If there are no candidates then a fault happened.
	 * @return true if fault.  false is no fault
	 */
	public boolean isFault() {
		return candidates.isEmpty();
	}

	@Override
	protected void dropVisualTrack(BTrack track) {
		PointTrack left = track.visualTrack;
		TrackInfo info = left.getCookie();
		PointTrack right = info.visualTrack;
		trackerLeft.dropTrack(left);
		trackerRight.dropTrack(right);
	}

	/**
	 * A coupled track between the left and right cameras.
	 */
	public static class TrackInfo extends BTrack
	{
		// Image based tracks in left and right camera
		public PointTrack visualRight;
		public long lastStereoFrame;
		// last time it was in the inlier list
		public long lastInlier;
		// the last frame it was seen in
		public long lastSeenRightFrame;

		public void reset() {
			super.reset();
			visualRight = null;
			lastStereoFrame = lastInlier = -1;
		}
	}
}
