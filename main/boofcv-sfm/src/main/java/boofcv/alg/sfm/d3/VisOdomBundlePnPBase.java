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

import boofcv.abst.geo.TriangulateNViewsMetric;
import boofcv.abst.sfm.d3.VisualOdometry;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.sfm.d3.structure.MaxGeoKeyFrameManager;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import boofcv.alg.sfm.d3.structure.VisOdomKeyFrameManager;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.struct.VerbosePrint;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Base class for all visual odometry algorithms based on PNP and use bundle adjustment.
 *
 * @author Peter Abeles
 */
public abstract class VisOdomBundlePnPBase<Track extends VisOdomBundleAdjustment.BTrack>  implements VerbosePrint {

	/** discard tracks after they have not been in the inlier set for this many updates in a row */
	protected @Getter @Setter int thresholdRetireTracks;

	/** Maximum number of allowed key frames in the scene */
	public int maxKeyFrames = 6;

	//----- Data structures for Bundle Adjustment and Track Information
	/** Describes the entire 3D scene's structure and optimizes with bundle adjustment */
	protected @Getter VisOdomBundleAdjustment<Track> scene;
	/** Decides when to create a new keyframe and discard them */
	protected @Getter @Setter VisOdomKeyFrameManager frameManager = new MaxGeoKeyFrameManager();

	/** location of tracks in the image that are included in the inlier set */
	protected @Getter final List<Track> inlierTracks = new ArrayList<>();
	/** Tracks which are visible in the most recently processed frame */
	protected @Getter final List<Track> visibleTracks = new ArrayList<>();
	// initial list of visible tracks before dropping during maintenance
	protected final List<Track> initialVisible = new ArrayList<>();

	// transform from the current camera view to the key frame
	protected final Se3_F64 current_to_previous = new Se3_F64();
	/** transform from the current camera view to the world frame */
	protected final Se3_F64 current_to_world = new Se3_F64();

	// is this the first camera view being processed?
	protected boolean first = true;

	/**
	 * Lens distortion camera models
	 */
	protected @Getter final List<CameraModel> cameraModels = new ArrayList<>();

	/** Triangulates points not optimized by bundle adjustment */
	protected @Getter TriangulateNViewsMetric triangulateN;

	// Internal profiling
	protected @Getter @Setter PrintStream profileOut;
	// Verbose debug information
	protected @Getter PrintStream verbose;

	//=================================================================
	//======== Workspace Variables
	List<BTrack> removedBundleTracks = new ArrayList<>();

	//======== Triangulation related
	// observations in normalized image coordinates
	protected FastQueue<Point2D_F64> observationsNorm = new FastQueue<>(Point2D_F64::new);
	protected FastQueue<Se3_F64> world_to_frame = new FastQueue<>(Se3_F64::new);
	protected Point3D_F64 found3D = new Point3D_F64();

	/**
	 * Resets the algorithm into its original state
	 */
	public void reset() {
		if( verbose != null ) verbose.println("VO: reset()");
		current_to_previous.reset();
		cameraModels.clear();
		scene.reset();
		first = true;
	}


	/**
	 * Goes through the list of initially visible tracks and see which ones have not been dropped
	 */
	protected void updateListOfVisibleTracksForOutput() {
		for (int i = 0; i < initialVisible.size(); i++) {
			Track t = initialVisible.get(i);
			if( t.visualTrack != null ) {
				visibleTracks.add(t);
			}
		}
	}

	/**
	 * Triangulate tracks which were not included in the optimization
	 */
	protected void triangulateNotSelectedBundleTracks() {
		for (int trackIdx = 0; trackIdx < scene.tracks.size; trackIdx++) {
			BTrack bt = scene.tracks.data[trackIdx];
			// skip selected since they have already been optimized or only two observations
			if( bt.selected || bt.observations.size < 3)
				continue;

			observationsNorm.reset();
			world_to_frame.reset();
			for (int obsIdx = 0; obsIdx < bt.observations.size; obsIdx++) {
				VisOdomBundleAdjustment.BObservation bo = bt.observations.get(obsIdx);
				CameraModel cm = cameraModels.get(bo.frame.camera.index);
				cm.pixelToNorm.compute(bo.pixel.x,bo.pixel.y,observationsNorm.grow());
				bo.frame.frame_to_world.invert(world_to_frame.grow());
			}

			if( triangulateN.triangulate(observationsNorm.toList(),world_to_frame.toList(),found3D) ) {
				bt.worldLoc.x = found3D.x;
				bt.worldLoc.y = found3D.y;
				bt.worldLoc.z = found3D.z;
				bt.worldLoc.w = 1.0;
			}
		}
	}

	protected boolean performKeyFrameMaintenance(PointTracker<?> tracker , int newFrames ) {
		GrowQueue_I32 dropFrameIndexes = frameManager.selectFramesToDiscard(tracker,maxKeyFrames,newFrames, scene);
		if( dropFrameIndexes.size == 0 )
			return false;
		boolean droppedCurrentFrame = dropFrameIndexes.getTail(0) == scene.frames.size-1;
		for (int i = dropFrameIndexes.size-1; i >= 0; i--) {
			// indexes are ordered from lowest to highest, so you can remove frames without
			// changing the index in the list
			VisOdomBundleAdjustment.BFrame frameToDrop = scene.frames.get(dropFrameIndexes.get(i));

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
	 * Drop tracks which are no longer being visually tracked and have less than two observations. In general
	 * 3 observations is much more stable than two and less prone to be a false positive.
	 */
	protected void dropTracksNotVisibleAndTooFewObservations() {
		for (int bidx = scene.tracks.size-1; bidx >= 0; bidx--) {
			BTrack t = scene.tracks.get(bidx);
			if( t.visualTrack == null && t.observations.size < 3 ) {
//				System.out.println("drop old bt="+t.id+" tt=NONE");
				// this marks it as dropped. Formally remove it in the next loop
				t.observations.reset();
				scene.tracks.removeSwap(bidx);
			}
		}
		for (int fidx = 0; fidx < scene.frames.size; fidx++) {
			VisOdomBundleAdjustment.BFrame f = scene.frames.get(fidx);
			for (int i = f.tracks.size-1; i >= 0; i--) {
				if( f.tracks.get(i).observations.size == 0 ) {
					f.tracks.removeSwap(i);
				}
			}
		}
	}

	/**
	 * Remove tracks with large errors and impossible geometry
	 */
	protected void dropBadBundleTracks() {
		Se3_F64 world_to_frame = new Se3_F64();
		Point4D_F64 cameraLoc = new Point4D_F64();

		for (int frameidx = 0; frameidx < scene.frames.size; frameidx++) {
			VisOdomBundleAdjustment.BFrame frame = scene.frames.get(frameidx);
			frame.frame_to_world.invert(world_to_frame);

			for (int trackidx = frame.tracks.size-1; trackidx >= 0; trackidx--) {
				BTrack track = frame.tracks.get(trackidx);
				SePointOps_F64.transform(world_to_frame, track.worldLoc, cameraLoc);

				// test to see if the feature is behind the camera while avoiding divded by zero errors
				if( Math.signum(cameraLoc.z) * Math.signum(cameraLoc.w) < 0 ) {
//					System.out.println("Dropping bad track");
					// this marks it for removal later on
					track.observations.reset();
					if( track.visualTrack != null ) {
						dropVisualTrack(track);
						track.visualTrack = null;
					}
				}

				// TODO test to see if residual is excessively large
			}
		}

		for (int bidx = scene.tracks.size-1; bidx >= 0; bidx--) {
			BTrack t = scene.tracks.get(bidx);
			if( t.observations.size == 0 ) {
				scene.tracks.removeSwap(bidx);
			}
		}

		for (int fidx = 0; fidx < scene.frames.size; fidx++) {
			VisOdomBundleAdjustment.BFrame f = scene.frames.get(fidx);
			for (int i = f.tracks.size-1; i >= 0; i--) {
				if( f.tracks.get(i).observations.size == 0 ) {
					f.tracks.removeSwap(i);
				}
			}
		}
	}

	protected abstract void dropVisualTrack( BTrack track );

	@Override
	public void setVerbose(@Nullable PrintStream out, @Nullable Set<String> configuration) {
		if( configuration == null ) {
			this.verbose = out;
			return;
		}

		if( configuration.contains(VisualOdometry.VERBOSE_RUNTIME))
			this.profileOut = out;
		if( configuration.contains(VisualOdometry.VERBOSE_TRACKING))
			this.verbose = out;
	}

	public Se3_F64 getCurrentToWorld() {
		return current_to_world;
	}

	public abstract long getFrameID();

	/**
	 * Contains the camera's lens distortion model
	 */
	protected static class CameraModel {
		/** converts from pixel to normalized image coordinates */
		protected Point2Transform2_F64 pixelToNorm;
		/** convert from normalized image coordinates to pixel */
		protected Point2Transform2_F64 normToPixel;
	}
}
