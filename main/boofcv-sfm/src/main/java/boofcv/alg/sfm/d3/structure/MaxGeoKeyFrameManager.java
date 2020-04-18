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

import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.misc.Histogram2D_S32;
import boofcv.alg.misc.ImageCoverage;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BObservation;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import georegression.struct.point.Point2D_F64;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.GrowQueue_I32;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.Set;

/**
 * Designed to be frame rate independent and maximize geometric information across frames. Old frames are kept as long
 * as possible and attempts to drop/skip over bad frames due to motion blur.
 *
 * @author Peter Abeles
 */
public class MaxGeoKeyFrameManager implements VisOdomKeyFrameManager {
	/** Maximum number of keyframes it will keep */
	public int maxKeyFrames = 5;
	/** If the fraction of the image covered by features drops below this the current frame is a new keyframe */
	public double minimumCoverage = 0.4;

	// list of frames to discard
	private final GrowQueue_I32 keyframeIndexes = new GrowQueue_I32();

	// the maximum number (known or inferred) of features visible in a frame
	protected int maxFeaturesPerFrame;

	// used to compute the feature coverage on the iamge
	protected ImageCoverage coverage = new ImageCoverage();
	protected int imageWidth, imageHeight;

	//------------ Internal Workspace
	protected FastArray<PointTrack> activeTracks = new FastArray<>(PointTrack.class);
	protected Histogram2D_S32 histogram = new Histogram2D_S32();
	TLongIntMap frameToIndex = new TLongIntHashMap();

	// if not null it prints debugging messages to this stream
	protected PrintStream verbose;

	@Override
	public void configure(int imageWidth, int imageHeight) {
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.maxFeaturesPerFrame = 0;
		this.keyframeIndexes.reset();
	}

	@Override
	public GrowQueue_I32 selectFramesToDiscard( PointTracker<?> tracker , VisOdomBundleAdjustment<?> sba) {
		// always add a new keyframe until it hits the max
		keyframeIndexes.reset();
		if( sba.frames.size <= maxKeyFrames )
			return keyframeIndexes;

		// Initialize data structures
		activeTracks.reset();
		tracker.getActiveTracks(activeTracks.toList());

		// See if it should keep the current frame.
		if( keepCurrentFrame() ) {
			// discard an older key frame so that the current frame can be kept
			if( verbose != null) verbose.println("Keeping current    at="+activeTracks.size+" coverage="+coverage.getFraction());
			keyframeIndexes.add(selectOldToDiscard(sba));
		} else {
			if( verbose != null) verbose.println("discarding current at="+activeTracks.size+" coverage="+coverage.getFraction());
			// The current frame is too similar to the a previous key frame
			keyframeIndexes.add( sba.frames.size-1 );
		}

		return keyframeIndexes;
	}

	/**
	 * Perform different checks that attempt to see if too much has changed. If too much has changed then the
	 * current keyframe should be kept so that new features can be spawned and starvation avoided.
	 * @return true if it should keep the current frame
	 */
	protected boolean keepCurrentFrame() {
		// Compute fraction of the image covered by tracks
		coverage.reset(maxFeaturesPerFrame,imageWidth,imageHeight);
		for (int i = 0; i < activeTracks.size; i++) {
			Point2D_F64 p = activeTracks.get(i).pixel;
			coverage.markPixel((int)p.x,(int)p.y);
		}
		coverage.process();

		return coverage.getFraction() < minimumCoverage;
	}

	/**
	 * Selects an older frame to discard. If a frame has zero features in common with the current frame it
	 * will be selected. After that it scores frames based on how many features it has in common with the other
	 * frames, excluding the current frame.
	 *
	 * @return frame's index
	 */
	protected int selectOldToDiscard(VisOdomBundleAdjustment<?> sba) {
		frameToIndex.clear();
		for (int i = 0; i < sba.frames.size; i++) {
			frameToIndex.put(sba.frames.get(i).id,i);
		}

		// Create a histogram showing how observations connect the frames
		final int N = sba.frames.size;
		histogram.reshape(N,N);
		histogram.zero();
		// Skip the last row since it's not needed
		for (int frameIdxA = 0; frameIdxA < N-1; frameIdxA++) {
			BFrame frame = sba.frames.get(frameIdxA);
			for (int trackIdx = 0; trackIdx < frame.tracks.size; trackIdx++) {
				BTrack track = frame.tracks.get(trackIdx);
				for (int obsIdx = 0; obsIdx < track.observations.size; obsIdx++) {
					BObservation o = track.observations.get(obsIdx);
					int frameIdxB = frameToIndex.get(o.frame.id);
					if( frameIdxA == frameIdxB )
						continue;
					histogram.increment(frameIdxA,frameIdxB);
				}
			}
		}

		if( verbose != null) histogram.print("%4d");

		// See which keyframe the current frame has the best connection with
		// Most of the time it will be the previous frame, but if that was experienced a lot of motion blur it might
		// not be...
		int bestFrame = histogram.maximumRowIdx(N-1);
		if( verbose != null) verbose.println("Frame with best connection to current "+bestFrame);

		// Select the frame with the worst connection to the bestFrame. The reason the bestFrame is used and not
		// the current frame is that the current frame could be blurred too and might get discarded
		int lowestCount = Integer.MAX_VALUE;
		int worstIdx = -1;
		for (int frameIdx = 0; frameIdx < N - 1; frameIdx++) {
			if( frameIdx==bestFrame )
				continue;
			// if a node has no connection to the frame with bestFrame drop it since it's unlikely to have much
			// influence over the current frame's state and have no direct influence over bestFrame's state
			int connection = histogram.get(frameIdx,bestFrame);
			if( connection == 0 ) {
				if( verbose != null) verbose.println("No connection index "+frameIdx);
				return frameIdx;
			}

			if( connection < lowestCount ) {
				lowestCount = connection;
				worstIdx = frameIdx;
			}
		}
		if( verbose != null) verbose.println("Worst index "+worstIdx+"  count "+lowestCount);
		return worstIdx;
	}

	@Override
	public void handleSpawnedTracks(PointTracker<?> tracker) {
		// If the max spawn is known use it, otherwise keep track of the max seen
		if( tracker.getMaxSpawn() <= 0 ) {
			maxFeaturesPerFrame = Math.max(tracker.getTotalActive(),maxFeaturesPerFrame);
		} else {
			maxFeaturesPerFrame = tracker.getMaxSpawn();
		}
	}

	@Override
	public void setVerbose(@Nullable PrintStream out, @Nullable Set<String> configuration) {
		this.verbose = out;
	}
}
