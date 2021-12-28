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

import boofcv.BoofVerbose;
import boofcv.abst.geo.TriangulateNViewsMetric;
import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.d3.structure.MaxGeoKeyFrameManager;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BObservation;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import boofcv.alg.sfm.d3.structure.VisOdomKeyFrameManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Base class for all visual odometry algorithms based on PNP and use bundle adjustment.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class VisOdomBundlePnPBase<Track extends VisOdomBundleAdjustment.BTrack> implements VerbosePrint {

	/** discard tracks after they have not been in the inlier set for this many updates in a row */
	protected @Getter @Setter int thresholdRetireTracks;

	/** Maximum number of allowed key frames in the scene */
	public int maxKeyFrames = 6;

	/** Minimum number of feature to be triangulated which was not included in bundle adjustment. */
	public int minObservationsTriangulate = 3;

	/**
	 * Drop tracks which are no longer visible if their total number of observations is less than this number.
	 * At a minimum this can be 2, but 3 is recommended for stability.
	 */
	public int minObservationsNotVisible = 3;

	//----- Data structures for Bundle Adjustment and Track Information
	/** Describes the entire 3D scene's structure and optimizes with bundle adjustment */
	protected @Getter VisOdomBundleAdjustment<Track> bundleViso;
	/** Decides when to create a new keyframe and discard them */
	protected @Getter @Setter VisOdomKeyFrameManager frameManager = new MaxGeoKeyFrameManager();

	/** location of tracks in the image that are included in the inlier set */
	protected @Getter final List<Track> inlierTracks = new ArrayList<>();
	/** Tracks which are visible in the most recently processed frame */
	protected @Getter final List<Track> visibleTracks = new ArrayList<>();
	// initial list of visible tracks before dropping during maintenance
	// The original list of visual tracks isn't reported since we need full tracking information and some of those
	// tracks will be dropped and data recycled.
	protected final List<Track> initialVisible = new ArrayList<>();

	// transform from the current camera view to the key frame
	protected final Se3_F64 current_to_previous = new Se3_F64();
	protected final Se3_F64 previous_to_current = new Se3_F64();
	/** transform from the current camera view to the world frame */
	protected final Se3_F64 current_to_world = new Se3_F64();

	// is this the first camera view being processed?
	protected boolean first = true;

	/** Lens distortion fpr the cameras */
	protected @Getter final List<CameraModel> cameraModels = new ArrayList<>();

	/** Triangulates points not optimized by bundle adjustment */
	protected @Getter TriangulateNViewsMetric triangulateN;

	// Internal profiling
	protected @Getter @Setter @Nullable PrintStream profileOut;
	// Verbose debug information
	protected @Getter @Nullable PrintStream verbose;

	// Total number of tracks dropped due to large bundle adjustment errors
	protected int totalDroppedTracksBadBundle;

	//=================================================================
	//======== Workspace Variables
	List<PointTrack> removedBundleTracks = new ArrayList<>();

	//======== Triangulation related
	// observations in normalized image coordinates
	protected DogArray<Point2D_F64> observationsNorm = new DogArray<>(Point2D_F64::new);
	protected DogArray<Se3_F64> listOf_world_to_frame = new DogArray<>(Se3_F64::new);
	protected Point3D_F64 found3D = new Point3D_F64();

	protected Se3_F64 world_to_frame = new Se3_F64();
	protected Point4D_F64 cameraLoc = new Point4D_F64();

	/**
	 * Resets the algorithm into its original state
	 */
	public void reset() {
		if (verbose != null) verbose.println("VO: reset()");
		current_to_world.reset();
		current_to_previous.reset();
		cameraModels.clear();
		bundleViso.reset();
		first = true;
	}

	/**
	 * Goes through the list of initially visible tracks and see which ones have not been dropped
	 */
	protected void updateListOfVisibleTracksForOutput() {
		for (int i = 0; i < initialVisible.size(); i++) {
			Track t = initialVisible.get(i);
			if (t.visualTrack != null) {
				visibleTracks.add(t);
			}
		}
	}

	/**
	 * Triangulate tracks which were not included in the optimization
	 */
	protected void triangulateNotSelectedBundleTracks() {
		final int minObservationsTriangulate = this.minObservationsTriangulate;

		for (int trackIdx = 0; trackIdx < bundleViso.tracks.size; trackIdx++) {
			final BTrack bt = bundleViso.tracks.data[trackIdx];
			// skip selected since they have already been optimized or only too few observations since
			// results will be unstable
			if (bt.selected || bt.observations.size < minObservationsTriangulate)
				continue;

			observationsNorm.reset();
			listOf_world_to_frame.reset();
			for (int obsIdx = 0; obsIdx < bt.observations.size; obsIdx++) {
				BObservation bo = bt.observations.get(obsIdx);
				CameraModel cm = cameraModels.get(bo.frame.camera.index);
				cm.pixelToNorm.compute(bo.pixel.x, bo.pixel.y, observationsNorm.grow());
				bo.frame.frame_to_world.invert(listOf_world_to_frame.grow());
				// NOTE: This invert could be cached. Doesn't need to be done a million times
			}

			// NOTE: If there is a homogenous metric triangulation added in the future replace this with that
			if (triangulateN.triangulate(observationsNorm.toList(), listOf_world_to_frame.toList(), found3D)) {
				bt.worldLoc.x = found3D.x;
				bt.worldLoc.y = found3D.y;
				bt.worldLoc.z = found3D.z;
				bt.worldLoc.w = 1.0;
			}
		}
	}

	/**
	 * Drops specified keyframes from the scene. Returns true if the current frame was dropped
	 *
	 * @param tracker tracker
	 * @param newFrames Number of new frames added to the scene
	 * @return true if current frame
	 */
	protected boolean performKeyFrameMaintenance( PointTracker<?> tracker, int newFrames ) {
		DogArray_I32 dropFrameIndexes = frameManager.selectFramesToDiscard(tracker, maxKeyFrames, newFrames, bundleViso);
		boolean droppedCurrentFrame = false;
		if (dropFrameIndexes.size != 0) {
			droppedCurrentFrame = dropFrameIndexes.getTail(0) == bundleViso.frames.size - 1;
			dropFramesFromScene(dropFrameIndexes);
		}
		dropTracksNotVisibleAndTooFewObservations();
		updateListOfVisibleTracksForOutput();

		return droppedCurrentFrame;
	}

	/**
	 * Removes the frames listed from the scene
	 *
	 * @param dropFrameIndexes List of indexes to drop. Sorted from lowest to highest
	 */
	protected void dropFramesFromScene( DogArray_I32 dropFrameIndexes ) {
		for (int i = dropFrameIndexes.size - 1; i >= 0; i--) {
			// indexes are ordered from lowest to highest, so you can remove frames without
			// changing the index in the list
			BFrame frameToDrop = bundleViso.frames.get(dropFrameIndexes.get(i));
//			System.out.println("Dropping frame ID "+frameToDrop.id);

			// update data structures
			bundleViso.removeFrame(frameToDrop, removedBundleTracks);

			// These tracks were visually being tracked and were removed. So drop them from the visual tracker
			for (int removeIdx = 0; removeIdx < removedBundleTracks.size(); removeIdx++) {
				dropVisualTrack(removedBundleTracks.get(removeIdx));
			}
		}
	}

	/**
	 * Drop tracks which are no longer being visually tracked and have less than two observations. In general
	 * 3 observations is much more stable than two and less prone to be a false positive.
	 */
	protected void dropTracksNotVisibleAndTooFewObservations() {
		final int minObservationsNotVisible = this.minObservationsNotVisible;

		// iteration through track lists in reverse order because of removeSwap()
		for (int tidx = bundleViso.tracks.size - 1; tidx >= 0; tidx--) {
			BTrack bt = bundleViso.tracks.get(tidx);
			if (bt.visualTrack == null && bt.observations.size < minObservationsNotVisible) {
				bt.observations.reset(); // Mark it as dropped. Formally remove it in the next loop
				bundleViso.tracks.removeSwap(tidx);
//				System.out.println("drop old bt="+bt.id+" vt=NONE");
			}
		}

		// Need to remove the dropped tracks from each frame that saw them.
		for (int fidx = 0; fidx < bundleViso.frames.size; fidx++) {
			BFrame bf = bundleViso.frames.get(fidx);
			for (int tidx = bf.tracks.size - 1; tidx >= 0; tidx--) {
				BTrack bt = bf.tracks.get(tidx);
				if (bt.observations.size == 0) {
					bf.tracks.removeSwap(tidx);
//					System.out.println("removing track="+bt.id+" from frame="+bf.id);
				}
			}
		}
	}

	/**
	 * Remove tracks with large errors and impossible geometry. For now it just removes tracks behind a camera.
	 */
	void dropBadBundleTracks() {
		int totalBehind = 0;

		// Go through each frame and look for tracks which are bad
		for (int fidx = 0; fidx < bundleViso.frames.size; fidx++) {
			BFrame bf = bundleViso.frames.get(fidx);
			bf.frame_to_world.invert(world_to_frame);

			for (int tidx = bf.tracks.size - 1; tidx >= 0; tidx--) {
				BTrack bt = bf.tracks.get(tidx);
				// Remove from frame if it was already marked for removal
				if (bt.observations.size == 0) {
					continue;
				}

				// test to see if the feature is behind the camera while avoiding divided by zero errors
				SePointOps_F64.transform(world_to_frame, bt.worldLoc, cameraLoc);

				if (PerspectiveOps.isBehindCamera(cameraLoc)) {
					totalBehind++;

					// this marks it for removal later on
					bt.observations.reset();
//					System.out.println("Dropping bad track. id="+bt.id+" z="+(cameraLoc.z/cameraLoc.w));
				}

				// Isn't it a bit excessive to drop the entire track if it's bad in just one frame?
				// TODO test to see if residual is excessively large
			}
		}

		// Remove it from the master tracks list
		totalDroppedTracksBadBundle = bundleViso.tracks.size;
		for (int tidx = bundleViso.tracks.size - 1; tidx >= 0; tidx--) {
			BTrack bt = bundleViso.tracks.get(tidx);
			if (bt.observations.size == 0) {
				if (bt.id == -1) {
					throw new RuntimeException("BUG! Dropping a track that was never initialized");
				}
				bundleViso.tracks.removeSwap(tidx);
			}
		}
		totalDroppedTracksBadBundle -= bundleViso.tracks.size; // the delta is the number of dropped tracks

		if (verbose != null)
			verbose.printf("drop bundle: total=%d {behind=%d}\n", totalBehind, totalDroppedTracksBadBundle);

		// Do a second pass since if it was removed in the first pass it might not be removed from all the frames
		// if it was good in an earlier one
		for (int fidx = 0; fidx < bundleViso.frames.size; fidx++) {
			BFrame bf = bundleViso.frames.get(fidx);
			for (int tidx = bf.tracks.size - 1; tidx >= 0; tidx--) {
				BTrack bt = bf.tracks.get(tidx);
				if (bt.observations.size == 0) {
//					System.out.println("  Removing track from frame: "+bt.id);
					bf.tracks.removeSwap(tidx);
					if (bt.visualTrack != null) {
						dropVisualTrack(bt.visualTrack);
						bt.visualTrack = null;
					}
				}
			}
		}
	}

	/**
	 * Given the BTrack drop all visual tracks which belong to it.
	 */
	protected abstract void dropVisualTrack( PointTrack track );

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		this.profileOut = null;

		if (configuration == null) {
			return;
		}

		if (configuration.contains(BoofVerbose.RUNTIME)) {
			this.profileOut = verbose;
		}
	}

	public Se3_F64 getCurrentToWorld() {
		return current_to_world;
	}

	public abstract long getFrameID();

	/**
	 * Contains the camera's lens distortion model
	 */
	@SuppressWarnings({"NullAway.Init"})
	protected static class CameraModel {
		/** converts from pixel to normalized image coordinates */
		protected Point2Transform2_F64 pixelToNorm;
		/** convert from normalized image coordinates to pixel */
		protected Point2Transform2_F64 normToPixel;
	}
}
