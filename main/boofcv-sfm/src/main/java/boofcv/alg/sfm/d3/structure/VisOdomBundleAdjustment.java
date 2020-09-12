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

package boofcv.alg.sfm.d3.structure;

import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.tracker.PointTrack;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import gnu.trove.set.hash.TLongHashSet;
import lombok.Getter;
import org.ddogleg.struct.Factory;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Bundle adjustment specifically intended for use with visual odometry algorithms.
 *
 * @author Peter Abeles
 */
public class VisOdomBundleAdjustment<T extends VisOdomBundleAdjustment.BTrack> {

	/** List of all tracks that can be feed into bundle adjustment */
	public final FastQueue<T> tracks;
	/** List of all frames that can be feed into bundle adjustment */
	public final FastQueue<BFrame> frames = new FastQueue<>(BFrame::new, BFrame::reset);
	/** List of all the cameras */
	public final FastQueue<BCamera> cameras = new FastQueue<>(BCamera::new, BCamera::reset);

	public SceneStructureMetric structure = new SceneStructureMetric(true);
	public SceneObservations observations = new SceneObservations();

	public BundleAdjustment<SceneStructureMetric> bundleAdjustment;
	public List<BTrack> selectedTracks = new ArrayList<>();

	// Reduce the number of tracks feed into bundle adjustment to make it run at a reasonable speed
	@Getter SelectTracksInFrameForBundleAdjustment selectTracks = new SelectTracksInFrameForBundleAdjustment(0xBEEF);

	final Se3_F64 world_to_view = new Se3_F64();

	public VisOdomBundleAdjustment(BundleAdjustment<SceneStructureMetric> bundleAdjustment, Factory<T> factoryTracks) {
		this.tracks = new FastQueue<>(factoryTracks, BTrack::reset);
		this.bundleAdjustment = bundleAdjustment;
	}

	/**
	 * Performs bundle adjustment on the scene and updates parameters
	 */
	public void optimize() {
		selectTracks.selectTracks(this, selectedTracks);
		setupBundleStructure();

		bundleAdjustment.setParameters(structure, observations);
		bundleAdjustment.optimize(structure);

		copyResults();
	}

	/** Returns true if it is configured to be optimized */
	public boolean isOptimizeActive() {
		return bundleAdjustment != null;
	}

	/**
	 * Adds a new camera to the scene
	 */
	public BCamera addCamera(CameraPinholeBrown camera) {
		BCamera output = cameras.grow();
		output.index = cameras.size - 1;
		output.original = camera;
		output.bundleCamera.set(camera);
		return output;
	}

	/**
	 * Converts input data into a format that bundle adjustment can understand
	 */
	private void setupBundleStructure() {
		// Need to count the total number of tracks that will be feed into bundle adjustment
		int totalBundleTracks = selectedTracks.size();

		// Initialize data structures
		observations.initialize(frames.size);
		structure.initialize(cameras.size, frames.size, totalBundleTracks);
		for (int cameraIdx = 0; cameraIdx < cameras.size; cameraIdx++) {
			structure.setCamera(cameraIdx, true, cameras.get(cameraIdx).bundleCamera);
		}

		// TODO make the first frame at origin. This is done to avoid numerical after traveling a good distance
		for (int frameIdx = 0; frameIdx < frames.size; frameIdx++) {
			BFrame bf = frames.get(frameIdx);
			bf.frame_to_world.invert(world_to_view);
			structure.setView(frameIdx, frameIdx == 0, world_to_view);
			structure.connectViewToCamera(frameIdx, bf.camera.index);
			frames.get(frameIdx).listIndex = frameIdx; // save the index since it's needed in the next loop
		}

		// A feature is only passed to SBA if it is active and more than one view has seen it
		// this requires it to have a different index
		int featureBundleIdx = 0;
		for (int trackIdx = 0; trackIdx < tracks.size; trackIdx++) {
			BTrack bt = tracks.get(trackIdx);
			if (!bt.selected) {
				continue;
			}
			Point4D_F64 p = bt.worldLoc;
			structure.setPoint(featureBundleIdx, p.x, p.y, p.z, p.w);

			for (int obsIdx = 0; obsIdx < bt.observations.size; obsIdx++) {
				BObservation o = bt.observations.get(obsIdx);
				SceneObservations.View view = observations.getView(o.frame.listIndex);
				view.add(featureBundleIdx, (float) o.pixel.x, (float) o.pixel.y);
			}
			featureBundleIdx++;
		}

		// Sanity check
		if (featureBundleIdx != structure.points.size)
			throw new RuntimeException("BUG! tracks feed in and points don't match");
	}

	/**
	 * Copies results back on to the local data structures
	 */
	private void copyResults() {
		// skip the first frame since it's fixed
		for (int frameIdx = 1; frameIdx < frames.size; frameIdx++) {
			BFrame bf = frames.get(frameIdx);
			structure.views.get(frameIdx).parent_to_view.invert(bf.frame_to_world);
		}

		int featureIdx = 0;
		for (int trackIdx = 0; trackIdx < tracks.size; trackIdx++) {
			BTrack bt = tracks.get(trackIdx);
			if (!bt.selected) {
				continue;
			}
			SceneStructureCommon.Point sp = structure.points.get(featureIdx);
			sp.get(bt.worldLoc);
			featureIdx++;
		}
	}

	/**
	 * Returns to its original state with new views. The camera model is saved
	 */
	public void reset() {
		frames.reset();
		tracks.reset();
		cameras.reset();
	}

	public void addObservation(BFrame frame, T track, double pixelX, double pixelY) {
		BObservation o = track.observations.grow();
		o.frame = frame;
		o.pixel.set(pixelX, pixelY);
		frame.tracks.add(track);
	}

	/** Searches for a track that has the following tracker track. null is none were found */
	public T findByTrackerTrack(PointTrack target) {
		for (int i = 0; i < tracks.size; i++) {
			if (tracks.get(i).visualTrack == target) {
				return tracks.get(i);
			}
		}
		return null;
	}

	public T addTrack(double x, double y, double z, double w) {
		T track = tracks.grow();
		track.worldLoc.set(x, y, z, w);
		return track;
	}

	public BFrame addFrame(long id) {
		if (cameras.size != 1)
			throw new IllegalArgumentException("To use this function there must be one and only one camera");
		return addFrame(0, id);
	}

	public BFrame addFrame(int cameraIndex, long id) {
		BFrame frame = frames.grow();
		frame.camera = cameras.get(cameraIndex);
		frame.id = id;
		return frame;
	}

	BFrame addFrameDebug(long id) {
		BFrame frame = frames.grow();
		frame.id = id;
		return frame;
	}

	/**
	 * Removes the frame and all references to it. If a track has no observations after this
	 * it is also removed from the master list.
	 *
	 * @param removedVisualTracks List of tracks which were removed and were being visually tracked
	 *                            because they had no more observations. Cleared each call.
	 */
	public void removeFrame(BFrame frame, List<PointTrack> removedVisualTracks) {
		removedVisualTracks.clear();
		int index = frames.indexOf(frame);
		if (index < 0) {
			throw new RuntimeException("BUG! frame not in frames list");
		}

		// denotes if at least one observations was knocked down to zero observations
		boolean pruneObservations = false;

		// Remove all references to this frame from its tracks
		for (int trackIdx = 0; trackIdx < frame.tracks.size; trackIdx++) {
			BTrack bt = frame.tracks.get(trackIdx);

			if (!bt.removeRef(frame))
				throw new RuntimeException("Bug: Track not in frame. frame.id " + frame.id + " track.id " + bt.id);

			// If the track no longer has observations remove it from the master track list
			if (bt.observations.size() == 0) {
				pruneObservations = true;
			}
		}

		if (pruneObservations) {
			// Search through all observations and remove the ones not in use any more
			for (int i = tracks.size - 1; i >= 0; i--) {
				if (tracks.get(i).observations.size == 0) {
					BTrack t = tracks.removeSwap(i);
					if (t.visualTrack != null) {
						removedVisualTracks.add(t.visualTrack);
						if (t.visualTrack.cookie != t) {
							System.out.println("BUG! bt=" + t.id + " tt=" + t.visualTrack.featureId);
							throw new RuntimeException("BUG!");
						}
						t.visualTrack = null; // mark it as null so that we know it has been dropped
					}
				}
			}
		}

		frames.remove(index);
	}

	public BFrame getLastFrame() { return frames.get(frames.size - 1); }

	public BFrame getFirstFrame() { return frames.get(0);}

	public BCamera getCamera(int index) {return cameras.get(index);}

	/**
	 * Sees if the graph structure is internally consistent. Used for debugging
	 */
	public void sanityCheck() {
		var trackSet = new TLongHashSet();

		for (int frameIdx = 0; frameIdx < frames.size; frameIdx++) {
			BFrame bf = frames.get(frameIdx);

			for (int trackIdx = 0; trackIdx < bf.tracks.size; trackIdx++) {
				BTrack bt = bf.tracks.get(trackIdx);
				trackSet.add(bt.id);
				if (!bt.isObservedBy(bf)) {
					throw new RuntimeException("Frame's track list is out of date. frame.id=" + bf.id + " track.id=" + bt.id + " obs.size " + bt.observations.size);
				} else {
					if (tracks.isUnused((T) bt)) {
						throw new RuntimeException("BUG! Track is in unused list. frame.id=" + bf.id + " track.id=" + bt.id);
					}
				}
			}
		}

		// TODO check to see if all observations are in a frame

//		if( trackSet.size() != tracks.size )
//			throw new IllegalArgumentException("Number of unique tracks in all frames: "+
//					trackSet.size()+" vs track.size "+tracks.size);
	}

	public static class BObservation {
		public final Point2D_F64 pixel = new Point2D_F64();
		public BFrame frame;

		public void reset() {
			pixel.set(-1, -1);
			frame = null;
		}
	}

	public static class BTrack {
		// the ID of the PointTrack which created this
		public long id;
		/**
		 * Reference to the a track in the image based tracker
		 * if null that means the track is no longer being tracked by the tracker
		 */
		public PointTrack visualTrack;
		public final Point4D_F64 worldLoc = new Point4D_F64();
		public final FastQueue<BObservation> observations = new FastQueue<>(BObservation::new, BObservation::reset);
		/** if true then the track has been an inlier at least once and should be considered for optimization */
		public boolean hasBeenInlier;
		/** true if it was selected for inclusion in the optimization */
		public boolean selected;

		public boolean isObservedBy(BFrame frame) {
			for (int i = 0; i < observations.size; i++) {
				if (observations.data[i].frame == frame) {
					return true;
				}
			}
			return false;
		}

		public BObservation findObservationBy(BFrame frame) {
			for (int i = 0; i < observations.size; i++) {
				if (observations.data[i].frame == frame) {
					return observations.data[i];
				}
			}
			return null;
		}

		public void reset() {
			worldLoc.set(0, 0, 0, 0);
			observations.reset();
			hasBeenInlier = false;
			selected = false;
			visualTrack = null;
			id = -1;
		}

		/**
		 * Removes the observations to the specified frame
		 *
		 * @return true if a match was found and removed. False otherwise.
		 */
		public boolean removeRef(BFrame frame) {
			for (int i = observations.size - 1; i >= 0; i--) {
				if (observations.data[i].frame == frame) {
					observations.removeSwap(i);
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * A BFrame is a key frame. Each keyframe represents the state at the time of a specific image frame in the
	 * sequence.
	 */
	public static class BFrame {
		// ID of the image used to create the BFrame
		public long id;
		// Which camera generated the image the BFrame is derived from
		public BCamera camera;
		// List of tracks that were observed in this BFrame
		public final FastArray<BTrack> tracks = new FastArray<>(BTrack.class);
		// current estimated transform to world from this view
		public final Se3_F64 frame_to_world = new Se3_F64();
		public int listIndex; // index in the list of BFrames

		public void reset() {
			id = -1;
			listIndex = -1;
			tracks.reset();
			frame_to_world.reset();
		}
	}

	public static class BCamera {
		// array index
		public int index;
		public CameraPinholeBrown original;
		public BundlePinholeBrown bundleCamera = new BundlePinholeBrown();

		public void reset() {
			index = -1;
			original = null;
		}
	}
}
