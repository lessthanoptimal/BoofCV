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

import boofcv.abst.geo.bundle.BundleAdjustment;
import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.abst.tracker.PointTrack;
import boofcv.alg.geo.bundle.cameras.BundlePinholeBrown;
import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.FactoryMultiView;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import gnu.trove.set.hash.TLongHashSet;
import org.ddogleg.optimization.lm.ConfigLevenbergMarquardt;
import org.ddogleg.struct.Factory;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Bundle adjustment specifically intended for use with visual odometry algorithms.
 *
 * @author Peter Abeles
 */
public class VisOdomBundleAdjustment<T extends VisOdomBundleAdjustment.BTrack> {

	public final FastQueue<T> tracks;
	public final FastQueue<BFrame> frames = new FastQueue<>(BFrame::new,BFrame::reset);

	BundlePinholeBrown camera;

	SceneStructureMetric structure = new SceneStructureMetric(true);
	SceneObservations observations = new SceneObservations();

	BundleAdjustment<SceneStructureMetric> bundleAdjustment;

	public VisOdomBundleAdjustment( Factory<T> factoryTracks ) {
		tracks = new FastQueue<>(factoryTracks,BTrack::reset);

		ConfigLevenbergMarquardt configLM = new ConfigLevenbergMarquardt();
		configLM.dampeningInitial = 1e-3;
		configLM.hessianScaling = true;
		ConfigBundleAdjustment configSBA = new ConfigBundleAdjustment();
		configSBA.configOptimizer = configLM;
		bundleAdjustment = FactoryMultiView.bundleSparseMetric(configSBA);
		bundleAdjustment.configure(1e-3, 1e-3, 3);
	}

	/**
	 * Performs bundle adjustment on the scene and updates parameters
	 */
	public void optimize() {
		setupBundleStructure();

		bundleAdjustment.setParameters(structure,observations);
		bundleAdjustment.optimize(structure);

		copyResults();
	}

	/**
	 * Converts input data into a format that bundle adjustment can understand
	 */
	private void setupBundleStructure() {

		// Need to count the total number of tracks that will be feed into bundle adjustment
		int totalBundleTracks = 0;
		for (int trackIdx = 0; trackIdx < tracks.size; trackIdx++) {
			BTrack t = tracks.get(trackIdx);
			if (t.active && t.observations.size > 1)
				totalBundleTracks++;
		}

		// Initialize data structures
		observations.initialize(frames.size);
		structure.initialize(1,frames.size,totalBundleTracks);
		structure.setCamera(0,true,camera);

		// TODO make the first frame at origin. This is done to avoid numerical after traveling a good distance
		final var worldToFrame = new Se3_F64();
		for (int frameIdx = 0; frameIdx < frames.size; frameIdx++) {
			frames.get(frameIdx).frameToWorld.invert(worldToFrame);
			structure.setView(frameIdx,frameIdx==0,worldToFrame);
			structure.connectViewToCamera(frameIdx,0);
			frames.get(frameIdx).listIndex = frameIdx; // save the index since it's needed in the next loop
		}

		// A feature is only passed to SBA if it is active and more than one view has seen it
		// this requires it to have a different index
		int featureBundleIdx = 0;
		for (int trackIdx = 0; trackIdx < tracks.size; trackIdx++) {
			BTrack t = tracks.get(trackIdx);
			if( !t.active || t.observations.size <= 1)
				continue;
			Point4D_F64 p = t.worldLoc;
			structure.setPoint(featureBundleIdx,p.x,p.y,p.z,p.w);

			for (int obsIdx = 0; obsIdx < t.observations.size; obsIdx++) {
				BObservation o = t.observations.get(obsIdx);
				SceneObservations.View view = observations.getView(o.frame.listIndex);
				view.add(featureBundleIdx,(float)o.pixel.x,(float)o.pixel.y);
			}
			featureBundleIdx++;
		}

		// Sanity check
		if( featureBundleIdx != structure.points.size )
			throw new RuntimeException("BUG! tracks feed in and points don't match");
	}

	/**
	 * Copies results back on to the local data structures
	 */
	private void copyResults() {
		// skip the first frame since it's fixed
		for (int frameIdx = 1; frameIdx < frames.size; frameIdx++) {
			BFrame frame = frames.get(frameIdx);
			structure.views.get(frameIdx).worldToView.invert(frame.frameToWorld);
		}

		int featureIdx = 0;
		for (int trackIdx = 0; trackIdx < tracks.size; trackIdx++) {
			BTrack t = tracks.get(trackIdx);
			if( !t.active || t.observations.size <= 1)
				continue;
			SceneStructureMetric.Point sp = structure.points.get(featureIdx);
			sp.get(t.worldLoc);
			featureIdx++;
		}
	}

	/**
	 * Returns to its original state with new views. The camera model is saved
	 */
	public void reset() {
		frames.reset();
		tracks.reset();
	}

	public void addObservation(BFrame frame , T track , double pixelX , double pixelY ) {
		BObservation o = track.observations.grow();
		o.frame = frame;
		o.pixel.set(pixelX,pixelY);
		frame.tracks.add(track);
	}

	public T addTrack( long id , double x , double y , double z , double w ) {
		T track = tracks.grow();
		track.id = id;
		track.worldLoc.set(x,y,z,w);
		return track;
	}

	public BFrame addFrame(long id ) {
		BFrame frame = frames.grow();
		frame.id = id;
		return frame;
	}

	/**
	 * Removes the frame and all references to it. If a track has no observations after this
	 * it is also removed from the master list.
	 */
	public void removeFrame( BFrame frame , List<BTrack> removedTracks ) {
		removedTracks.clear();
		int index = frames.indexOf(frame);
		if( index < 0 )
			throw new RuntimeException("BUG! frame not in frames list");

		// denotes if at least one observations was knocked down to zero observations
		boolean pruneObservations = false;

		// Remove all references to this frame from its tracks
		for (int i = 0; i < frame.tracks.size; i++ ) {
			BTrack t = frame.tracks.get(i);

			if( !t.removeRef(frame) )
				throw new RuntimeException("Bug: Track not in frame. frame.id "+frame.id+" track.id "+t.id);

			// If the track no longer has observations remove it from the master track list
			if( t.observations.size() == 0 ) {
				pruneObservations = true;
			}
		}

		if( pruneObservations ) {
			// Search through all observations and remove the ones not in use nay more
			for (int i = tracks.size - 1; i >= 0; i--) {
				if (tracks.get(i).observations.size == 0) {
					removedTracks.add(tracks.removeSwap(i));
				}
			}
		}

		frames.remove(index);
	}

	public BFrame getLastFrame() {
		return frames.get(frames.size-1);
	}
	public BFrame getFirstFrame() {
		return frames.get(0);
	}

	/**
	 * Sees if the graph structure is internally consistent
	 */
	public void sanityCheck() {
		var trackSet = new TLongHashSet();

		for (int i = 0; i < frames.size; i++) {
			BFrame frame = frames.get(i);

			for (int trackIdx = 0; trackIdx < frame.tracks.size; trackIdx++) {
				BTrack t = frame.tracks.get(trackIdx);
				trackSet.add(t.id);
				if( !t.isObservedBy(frame) ) {
					throw new RuntimeException("Frame's track list is out of date. frame.id="+frame.id+" track.id="+t.id+" obs.size "+t.observations.size);
				} else {
					if( tracks.isUnused((T)t) ) {
						throw new RuntimeException("BUG! Track is in unused list. frame.id="+frame.id+" track.id="+t.id);
					}
				}
			}
		}

		// TODO check to see if all observations are in a frame

//		if( trackSet.size() != tracks.size )
//			throw new IllegalArgumentException("Number of unique tracks in all frames: "+
//					trackSet.size()+" vs track.size "+tracks.size);
	}

	public static class BObservation{
		public final Point2D_F64 pixel = new Point2D_F64();
		public BFrame frame;

		public void reset() {
			pixel.set(-1,-1);
			frame = null;
		}
	}

	public static class BTrack
	{
		public long id;
		public PointTrack trackerTrack;
		public final Point4D_F64 worldLoc = new Point4D_F64();
		public final FastQueue<BObservation> observations = new FastQueue<>(BObservation::new, BObservation::reset);
		/** if true then the track should be optimized inside of bundle adjustment */
		public boolean active;

		public boolean isObservedBy( BFrame frame ) {
			for (int i = 0; i < observations.size; i++) {
				if( observations.data[i].frame == frame )
					return true;
			}
			return false;
		}

		public void reset() {
			id = -1;
			worldLoc.set(0,0,0,0);
			observations.reset();
			active = false;
		}

		/**
		 * Removes the observations to the specified frame
		 * @return true if a match was found and removed. False otherwise.
		 */
		public boolean removeRef(BFrame frame) {
			for (int i = observations.size-1; i >= 0; i-- ) {
				if( observations.data[i].frame == frame ) {
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
	public static class BFrame
	{
		// ID of the image used to create the BFrame
		public long id;
		// List of tracks that were observed in this BFrame
		public final FastArray<BTrack> tracks = new FastArray<>(BTrack.class);
		// current estimated transform to world from this view
		public final Se3_F64 frameToWorld = new Se3_F64();
		public int listIndex; // index in the list of BFrames

		public void reset() {
			id = -1;
			listIndex = -1;
			tracks.reset();
			frameToWorld.reset();
		}
	}
}
