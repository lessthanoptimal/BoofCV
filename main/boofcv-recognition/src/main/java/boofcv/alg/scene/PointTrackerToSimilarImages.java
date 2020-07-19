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

package boofcv.alg.scene;

import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.sfm.structure2.LookupSimilarImages;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import gnu.trove.map.hash.TLongIntHashMap;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * Processes frames from {@link PointTracker} and converts the tracking results into a {@link LookupSimilarImages}.
 * This is intended for scene reconstruction from video sequences.
 *
 * <ul>
 *     <li>Results are ready as soon as {@link #processFrame} finishes</li>
 *     <li>The unique string for each image is set to the frameID</li>
 *     <li>Frames can only be matched to each other with they are withing {@link #searchRadius} of each other</li>
 *     <li>No loop closure is performed</li>
 * </ul>
 *
 * Internally arrays are used as much as possible to reduce memory overhead as higher level objects have a very
 * large amount of overhead.
 *
 * @author Peter Abeles
 */
public class PointTrackerToSimilarImages implements LookupSimilarImages {

	/** Maximum number of frames in forwards and backwards direction which will be searched for being related  */
	public int searchRadius = 5;

	/** Stores observations and track ID for every frame */
	public final FastQueue<Frame> frames = new FastQueue<>(Frame::new, Frame::reset);
	/** Quick way to retrieve a frame based on its ID */
	public final Map<String, Frame> frameMap = new HashMap<>();
	/**  List of all matches between frames */
	public final FastQueue<Matches> matches = new FastQueue<>(Matches::new, Matches::reset);

	// shape of images
	public int imageWidth, imageHeight;

	//------------------- Internal Workspace ------------------------------------
	List<PointTrack> tracks = new ArrayList<>();
	FastQueue<AssociatedIndex> pairs = new FastQueue<>(AssociatedIndex::new);

	/**
	 * Resets all data structures and saves the image size. Must be called before other functions
	 */
	public void initialize(int width, int height) {
		this.imageWidth = width;
		this.imageHeight = height;

		// reset internal data structures to their initial state
		frames.reset();
		frameMap.clear();
		matches.reset();
	}

	/**
	 * Process the latest observations from the tracker. Save the results and
	 *
	 * @param tracker Track after processing the latest frame/image
	 */
	public void processFrame(PointTracker<?> tracker) {
		assertBoof(imageWidth!=0,"Must call initialize first and specify the image size");

		// Create a new frame and save the observations
		Frame current = createFrameSaveObservations(tracker);

		// find related frames
		findRelatedPastFrames(current);
	}

	/**
	 * Create a new frame from the tracker's current observations.
	 */
	Frame createFrameSaveObservations(PointTracker<?> tracker) {
		// get a list of all the tracks which are visible in this frame
		tracker.getActiveTracks(tracks);

		// Create a new frame and record all the tracks visible in this frame
		Frame current = frames.grow();
		current.frameID = tracker.getFrameID() + "";
		current.initActive(tracks.size());
		int index = 0;
		for (int trackCnt = 0; trackCnt < tracks.size(); trackCnt++) {
			PointTrack t = tracks.get(trackCnt);
			current.observations[index++] = (float) t.pixel.x;
			current.observations[index++] = (float) t.pixel.y;
			current.ids[trackCnt] = t.featureId;
			current.id_to_index.put(t.featureId, trackCnt);
		}
		frameMap.put(current.frameID, current);
		return current;
	}

	/**
	 * Search the past N frames to see if they are related to the current frame. If related create a new {@link Matches}
	 * and save which observations points to the same features
	 * @param current The current from for the tracker
	 */
	void findRelatedPastFrames(Frame current) {
		// only check past frames. When the future frames they will check this one for a connection
		for (int frameCnt = Math.max(0, frames.size - searchRadius - 1); frameCnt < frames.size - 1; frameCnt++) {
			Frame previous = frames.get(frameCnt);

			// See if there are any features which have been observed in both frames
			pairs.reset();
			final int prevNumObs = previous.size();
			for (int previousIdx = 0; previousIdx < prevNumObs; previousIdx++) {
				int currentIdx = current.id_to_index.get(previous.getID(previousIdx));
				if (currentIdx < 0)
					continue;
				pairs.grow().set(currentIdx, previousIdx);
			}

			if (pairs.size == 0)
				continue;

			// Create a match that describes the observations of common features/tracks
			Matches m = matches.grow();
			m.init(pairs.size);
			m.frameSrc = current;
			m.frameDst = previous;
			for (int pairCnt = 0; pairCnt < pairs.size; pairCnt++) {
				AssociatedIndex a = pairs.get(pairCnt);
				m.src[pairCnt] = a.src;
				m.dst[pairCnt] = a.dst;
			}
			m.frameSrc.matches.add(m);
			m.frameDst.matches.add(m);
			previous.related.add(current);
			current.related.add(previous);
		}
	}

	/**
	 * Returns the list of image ID's in the same order they were processed.
	 */
	@Override
	public List<String> getImageIDs() {
		var list = new ArrayList<String>();
		for (int i = 0; i < frames.size; i++) {
			list.add(frames.get(i).frameID);
		}
		return list;
	}

	@Override
	public void findSimilar(String target, List<String> similar) {
		similar.clear();
		Frame f = frameMap.get(target);
		assertBoof(f != null, "Unknown image");
		for (int i = 0; i < f.related.size(); i++) {
			similar.add(f.related.get(i).frameID);
		}
	}

	@Override
	public void lookupPixelFeats(String target, FastQueue<Point2D_F64> features) {
		features.reset();
		Frame f = frameMap.get(target);
		assertBoof(f != null, "Unknown image");

		final int N = f.size();
		for (int i = 0; i < N; i++) {
			f.getPixel(i, features.grow());
		}
	}

	@Override
	public boolean lookupMatches(String viewA, String viewB, FastQueue<AssociatedIndex> pairs) {
		// clear the set of pairs so that if it fails it will be empty
		pairs.reset();

		// Look up the two frames based on their ID.
		Frame src = frameMap.get(viewA);
		if (src == null)
			return false;
		Frame dst = null;
		for (int i = 0; i < src.related.size(); i++) {
			if (src.related.get(i).frameID.equals(viewB)) {
				dst = src.related.get(i);
				break;
			}
		}
		if (dst == null)
			return false;

		// See if the two frames have any matches together
		Matches m = null;
		for (int i = 0; i < src.matches.size(); i++) {
			Matches a = src.matches.get(i);
			if (a.frameSrc == dst || a.frameDst == dst) {
				m = a;
				break;
			}
		}
		if (m == null)
			return false;

		// Go through the match and construct the list of associated features
		boolean swapped = m.frameSrc != src;
		int size = m.size();
		for (int i = 0; i < size; i++) {
			if (swapped) {
				pairs.grow().set(m.dst[i], m.src[i]);
			} else {
				pairs.grow().set(m.src[i], m.dst[i]);
			}
		}

		return true;
	}

	@Override
	public void lookupShape(String target, ImageDimension shape) {
		shape.set(imageWidth, imageHeight);
	}

	/**
	 * Describes how two frames are related to each other through common observations of the same feature
	 */
	protected static class Matches {
		// observation indexes for the same object in each frame
		public int[] src;
		public int[] dst;

		// which frame in the src and which one is the dst
		public Frame frameSrc;
		public Frame frameDst;

		public void init(int total) {
			src = new int[total];
			dst = new int[total];
		}

		/** Number of features/observations */
		public int size() {
			return src.length;
		}

		public void reset() {
			src = null;
			dst = null;
			frameSrc = null;
			frameDst = null;
		}
	}

	/**
	 * Observations for a frame. Arrays are used to reduce memory footprint.
	 */
	protected static class Frame {
		// Unique ID assigned to this frame
		public String frameID;
		// pixel observations for all the tracks (x,y) interleaved. This is floats and not doubles since we really
		// don't need double precision for sub-pixel accuracy of visual landmarks. 1/100 is very good precision
		public float[] observations;
		// list of all the active tracks in this frame
		public long[] ids;

		// lookup table from track ID to observation index. If ID not in the set then -1 is returned
		TLongIntHashMap id_to_index = new TLongIntHashMap() {{
			no_entry_value = -1;
		}};

		// List of frames which are related to this one
		public final List<Frame> related = new ArrayList<>();
		// Matches to related frames.
		public final List<Matches> matches = new ArrayList<>();

		public void initActive(int totalFeatures) {
			observations = new float[totalFeatures * 2];
			ids = new long[totalFeatures];
		}

		/** Number of feature observations */
		public int size() {
			return ids.length;
		}

		public void getPixel(int index, Point2D_F64 out) {
			index *= 2;
			out.x = observations[index];
			out.y = observations[index + 1];
		}

		public long getID(int index) {
			return ids[index];
		}

		public void reset() {
			observations = null;
			ids = null;
			frameID = null;
			id_to_index.clear();
			related.clear();
			matches.clear();
		}
	}
}
