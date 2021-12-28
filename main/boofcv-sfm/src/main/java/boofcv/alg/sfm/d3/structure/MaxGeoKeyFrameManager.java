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

package boofcv.alg.sfm.d3.structure;

import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.misc.Histogram2D_S32;
import boofcv.alg.misc.ImageCoverage;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BCamera;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BObservation;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import georegression.struct.point.Point2D_F64;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.ddogleg.sorting.QuickSort_S32;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

/**
 * Designed to be frame rate independent and maximize geometric information across frames. Old frames are kept as long
 * as possible and attempts to drop/skip over bad frames due to motion blur.
 *
 * @author Peter Abeles
 */
public class MaxGeoKeyFrameManager implements VisOdomKeyFrameManager {
	/** If the fraction of the image covered by features drops below this the current frame is a new keyframe */
	public double minimumCoverage = 0.4;

	// list of frames to discard
	protected final DogArray_I32 discardKeyIndices = new DogArray_I32();

	// Information of each camera and is used to compute the coverage
	protected DogArray<CameraInfo> cameras = new DogArray<>(CameraInfo::new, CameraInfo::reset);

	// used to compute the feature coverage on the image
	ImageCoverage coverage = new ImageCoverage();

	//------------ Internal Workspace
	protected FastArray<PointTrack> activeTracks = new FastArray<>(PointTrack.class);
	protected Histogram2D_S32 histogram = new Histogram2D_S32();
	TLongIntMap frameToIndex = new TLongIntHashMap();
	QuickSort_S32 sorter = new QuickSort_S32(); // Used this to avoid calling new like other sorts do

	// if not null it prints debugging messages to this stream
	protected @Nullable PrintStream verbose;

	public MaxGeoKeyFrameManager() {}

	public MaxGeoKeyFrameManager( double minimumCoverage ) {
		this.minimumCoverage = minimumCoverage;
	}

	protected static class CameraInfo {
		// Camera's shape in pixels
		public int imageWidth, imageHeight;
		// the maximum number (known or inferred) of features visible in a frame
		public int maxFeaturesPerFrame;

		public void reset() {
			imageWidth = -1;
			imageHeight = -1;
			maxFeaturesPerFrame = 0;
		}
	}

	@Override
	public void initialize( FastAccess<BCamera> bundleCameras ) {
		cameras.reset();
		for (int i = 0; i < bundleCameras.size; i++) {
			BCamera bc = bundleCameras.get(i);
			CameraInfo ic = cameras.grow();

			ic.imageWidth = bc.original.width;
			ic.imageHeight = bc.original.height;
		}
		this.discardKeyIndices.reset();
	}

	@Override
	public DogArray_I32 selectFramesToDiscard( PointTracker<?> tracker, int maxKeyFrames, int newFrames, VisOdomBundleAdjustment<?> sba ) {
		// always add a new keyframe until it hits the max
		discardKeyIndices.reset();
		if (sba.frames.size <= maxKeyFrames)
			return discardKeyIndices;

		// Initialize data structures
		activeTracks.reset();
		tracker.getActiveTracks(activeTracks.toList());

		boolean keepCurrent = keepCurrentFrame(sba);
		if (!keepCurrent) {
			// The current frame is too similar to the a previous key frame
			for (int i = 0; i < newFrames; i++) {
				discardKeyIndices.add(sba.frames.size - i - 1);
			}
		}
		if (verbose != null) verbose.println("keep_current=" + keepCurrent + " coverage=" + coverage.getFraction());

		// Discard enough older frames to be at the desired number
		selectOldToDiscard(sba, sba.frames.size - maxKeyFrames - discardKeyIndices.size);

		// Ensure the post condition is true
		sorter.sort(discardKeyIndices.data, discardKeyIndices.size);

		return discardKeyIndices;
	}

	/**
	 * Perform different checks that attempt to see if too much has changed. If too much has changed then the
	 * current keyframe should be kept so that new features can be spawned and starvation avoided.
	 *
	 * @return true if it should keep the current frame
	 */
	protected boolean keepCurrentFrame( VisOdomBundleAdjustment<?> sba ) {
		BFrame current = sba.frames.getTail();
		CameraInfo camera = cameras.get(current.camera.index);

		// Compute fraction of the image covered by tracks
		coverage.reset(camera.maxFeaturesPerFrame, camera.imageWidth, camera.imageHeight);
		for (int i = 0; i < activeTracks.size; i++) {
			Point2D_F64 p = activeTracks.get(i).pixel;
			coverage.markPixel((int)p.x, (int)p.y);
		}
		coverage.process();

		return coverage.getFraction() < minimumCoverage;
	}

	/**
	 * Selects an older frame to discard. If a frame has zero features in common with the current frame it
	 * will be selected. After that it scores frames based on how many features it has in common with the other
	 * frames, excluding the current frame.
	 *
	 * @param totalDiscard How many need to be discarded
	 */
	protected void selectOldToDiscard( VisOdomBundleAdjustment<?> sba, int totalDiscard ) {
		if (totalDiscard <= 0 || sba.frames.size < 2)
			return;

		frameToIndex.clear();
		for (int i = 0; i < sba.frames.size; i++) {
			frameToIndex.put(sba.frames.get(i).id, i);
		}

		// Create a histogram showing how observations connect the frames
		final int N = sba.frames.size;
		histogram.reshape(N, N);
		histogram.zero();
		// Skip the last row since it's not needed
		for (int frameIdxA = 0; frameIdxA < N - 1; frameIdxA++) {
			BFrame frame = sba.frames.get(frameIdxA);
			for (int trackIdx = 0; trackIdx < frame.tracks.size; trackIdx++) {
				BTrack track = frame.tracks.get(trackIdx);
				for (int obsIdx = 0; obsIdx < track.observations.size; obsIdx++) {
					BObservation o = track.observations.get(obsIdx);
					int frameIdxB = frameToIndex.get(o.frame.id);
					if (frameIdxA == frameIdxB)
						continue;
					histogram.increment(frameIdxA, frameIdxB);
				}
			}
		}

		if (verbose != null) histogram.print("%4d");

		// See which keyframe the current frame has the best connection with
		// Most of the time it will be the previous frame, but if that was experienced a lot of motion blur it might
		// not be...
		int bestFrame = histogram.maximumRowIdx(N - 1);
		if (verbose != null) verbose.println("Frame with best connection to current " + bestFrame);
		histogram.set(bestFrame, bestFrame, Integer.MAX_VALUE); // mark it so that it's skipped

		for (int i = 0; i < totalDiscard; i++) {
			// Select the frame with the worst connection to the bestFrame. The reason the bestFrame is used and not
			// the current frame is that the current frame could be blurred too and might get discarded
			int lowestCount = Integer.MAX_VALUE;
			int worstIdx = -1;
			// N-1 to avoid the current frame
			for (int frameIdx = 0; frameIdx < N - 1; frameIdx++) {
				int connection = histogram.get(frameIdx, bestFrame);
				if (connection == Integer.MAX_VALUE)
					continue;

				// if a node has no connection to the frame with bestFrame drop it since it's unlikely to have much
				// influence over the current frame's state and have no direct influence over bestFrame's state
				if (connection == 0) {
					if (verbose != null) verbose.println("No connection index " + frameIdx);
					lowestCount = 0;
					worstIdx = frameIdx;
					break;
				}

				if (connection < lowestCount) {
					lowestCount = connection;
					worstIdx = frameIdx;
				}
			}
			if (verbose != null) verbose.println("Worst index " + worstIdx + "  count " + lowestCount);

			discardKeyIndices.add(worstIdx);
			// Mark the worst keyframe so it won't be selected again
			histogram.set(worstIdx, bestFrame, Integer.MAX_VALUE);
		}
	}

	@Override
	public void handleSpawnedTracks( PointTracker<?> tracker, BCamera camera ) {
		CameraInfo info = cameras.get(camera.index);
		// If the max spawn is known use it, otherwise keep track of the max seen
		if (tracker.getMaxSpawn() <= 0) {
			info.maxFeaturesPerFrame = Math.max(tracker.getTotalActive(), info.maxFeaturesPerFrame);
		} else {
			info.maxFeaturesPerFrame = tracker.getMaxSpawn();
		}
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}
}
