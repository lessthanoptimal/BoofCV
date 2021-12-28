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

package boofcv.alg.similar;

import boofcv.abst.tracker.PointTrack;
import boofcv.abst.tracker.PointTracker;
import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.feature.AssociatedIndex;
import georegression.struct.point.Point2D_F64;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Processes frames from {@link PointTracker} and converts the tracking results into a {@link LookUpSimilarImages}.
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
@SuppressWarnings({"NullAway.Init"})
public class SimilarImagesFromTracks<Track> implements LookUpSimilarImages {
	/**
	 * Maximum number of frames in forwards and backwards direction which will be searched for being related.
	 * If less than zero there are no hard limit.
	 */
	public int searchRadius = 5;

	/**
	 * If the number of common tracks between two frames drops below this then they are considered to be
	 * disconnected. If relative, then it's relative to the minimum number of observed tracks in each frame..
	 */
	public final ConfigLength minimumCommonTracks = ConfigLength.relative(0.01, 0.0);

	/** Stores observations and track ID for every frame */
	public final DogArray<Frame> frames = new DogArray<>(Frame::new, Frame::reset);
	/** Quick way to retrieve a frame based on its ID */
	public final Map<String, Frame> frameMap = new HashMap<>();
	/** List of all matches between frames */
	public final DogArray<Match> connections = new DogArray<>(Match::new, Match::reset);

	// Image that was recently queried for similar matches
	String recentQueryID;

	// Accessors to track information
	TrackToID<Track> lookupID;
	TrackToPixel<Track> lookupPixel;

	// shape of images
	public int imageWidth, imageHeight;

	// Storage for pixel coordinate from a track
	Point2D_F64 pixel = new Point2D_F64();

	//------------------- Internal Workspace ------------------------------------
	protected List<PointTrack> tracks = new ArrayList<>();
	protected DogArray<AssociatedIndex> pairs = new DogArray<>(AssociatedIndex::new);

	public SimilarImagesFromTracks( TrackToID<Track> lookupID, TrackToPixel<Track> lookupPixel ) {
		this.lookupID = lookupID;
		this.lookupPixel = lookupPixel;
	}

	/**
	 * Resets all data structures and saves the image size. Must be called before other functions
	 */
	public void initialize( int width, int height ) {
		this.imageWidth = width;
		this.imageHeight = height;

		// reset internal data structures to their initial state
		frames.reset();
		frameMap.clear();
		connections.reset();
	}

	/**
	 * Process the latest observations from the tracker. Save the results and
	 *
	 * @param tracks (Input) List of active tracks visible in the current frame
	 * @param frameID (Input) Identifier for this image/frame
	 */
	public void processFrame( List<Track> tracks, long frameID ) {
		BoofMiscOps.checkTrue(imageWidth != 0, "Must call initialize first and specify the image size");

		// Create a new frame and save the observations
		Frame current = createFrameSaveObservations(tracks, frameID);

		// find related frames
		findRelatedPastFrames(current);
	}

	/**
	 * Create a new frame from the tracker's current observations.
	 */
	protected Frame createFrameSaveObservations( List<Track> tracks, long frameID ) {
		// Create a new frame and record all the tracks visible in this frame
		Frame current = frames.grow();
		current.frameID = frameID + "";
		current.initActive(tracks.size());
		int index = 0;
		for (int trackCnt = 0; trackCnt < tracks.size(); trackCnt++) {
			Track t = tracks.get(trackCnt);
			lookupPixel.getPixel(t, pixel);
			current.observations[index++] = (float)pixel.x;
			current.observations[index++] = (float)pixel.y;
			current.ids[trackCnt] = lookupID.getId(t);
			current.id_to_index.put(lookupID.getId(t), trackCnt);
		}
		frameMap.put(current.frameID, current);
		return current;
	}

	/**
	 * Search the past N frames to see if they are related to the current frame. If related create a new {@link Match}
	 * and save which observations points to the same features
	 *
	 * @param current The current from for the tracker
	 */
	void findRelatedPastFrames( Frame current ) {

		// Both values below are needed for early termination of search
		int currentTrackCount = frames.getTail().featureCount();
		int oldestFrameConsidered = searchRadius < 0 ? 0 : Math.max(0, frames.size - 1 - searchRadius);

		// Check the most recent past frames first, up until it hits the limit or there are too few tracks
		for (int frameCnt = frames.size - 2; frameCnt >= oldestFrameConsidered; frameCnt--) {
			Frame previous = frames.get(frameCnt);

			// See if there are any features which have been observed in both frames
			pairs.reset();
			final int prevNumObs = previous.featureCount();
			for (int previousIdx = 0; previousIdx < prevNumObs; previousIdx++) {
				int currentIdx = current.id_to_index.get(previous.getID(previousIdx));
				if (currentIdx < 0)
					continue;
				pairs.grow().setTo(currentIdx, previousIdx);
			}

			// If the number of common tracks is too small, don't consider this pairing
			// The loop will stop here since there is the simplifying assumption that the number of common
			// tracks can only go down. The loop closure logic should partially make up for this if it isn't true
			// otherwise we have to consider the entire history
			if (pairs.size == 0 ||
					pairs.size < minimumCommonTracks.computeI(Math.min(currentTrackCount, previous.featureCount())))
				break;

			connectFrames(current, previous, pairs);
		}
	}

	/**
	 * Connects the two frames and remembers the associated features between them
	 */
	protected void connectFrames( Frame current, Frame previous, FastAccess<AssociatedIndex> associated ) {
		// Create a match that describes the observations of common features/tracks
		Match m = connections.grow();
		m.init(associated.size);
		m.frameSrc = current;
		m.frameDst = previous;
		for (int assocIdx = 0; assocIdx < associated.size; assocIdx++) {
			AssociatedIndex a = associated.get(assocIdx);
			m.src[assocIdx] = a.src;
			m.dst[assocIdx] = a.dst;
		}
		m.frameSrc.matches.add(m);
		m.frameDst.matches.add(m);
		previous.related.add(current);
		current.related.add(previous);
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
	public void findSimilar( String target, @Nullable BoofLambdas.Filter<String> filter, List<String> similarImages ) {
		similarImages.clear();
		Frame f = Objects.requireNonNull(frameMap.get(target), "Unknown image");

		// For later reference
		this.recentQueryID = target;

		// Retrieve the similar images while filtering them
		for (int i = 0; i < f.related.size(); i++) {
			if (filter !=null && !filter.keep(f.related.get(i).frameID))
				continue;

			similarImages.add(f.related.get(i).frameID);
		}
	}

	@Override
	public void lookupPixelFeats( String target, DogArray<Point2D_F64> features ) {
		features.reset();
		Frame f = frameMap.get(target);
		if (f == null)
			throw new IllegalArgumentException("Unknown view=" + target);

		final int N = f.featureCount();
		for (int i = 0; i < N; i++) {
			f.getPixel(i, features.grow());
		}
	}

	@Override public boolean lookupAssociated( String viewB, DogArray<AssociatedIndex> pairs ) {
		// clear the set of pairs so that if it fails it will be empty
		pairs.reset();

		Frame src = frameMap.get(recentQueryID);
		Frame dst = frameMap.get(viewB);
		if (src == null || dst == null)
			throw new IllegalArgumentException("Unknown view: src=" + recentQueryID + " dst=" + viewB);

		// Look up the two frames based on their ID.
		boolean matched = false;
		for (int i = 0; i < src.related.size(); i++) {
			if (src.related.get(i) == dst) {
				matched = true;
				break;
			}
		}
		if (!matched)
			return false;

		// See if the two frames have any matches together
		Match m = null;
		for (int i = 0; i < src.matches.size(); i++) {
			Match a = src.matches.get(i);
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
				pairs.grow().setTo(m.dst[i], m.src[i]);
			} else {
				pairs.grow().setTo(m.src[i], m.dst[i]);
			}
		}

		return true;
	}

	/**
	 * Describes how two frames are related to each other through common observations of the same feature
	 */
	@SuppressWarnings({"NullAway.Init"})
	public static class Match {
		// observation indexes for the same feature in each frame
		public int[] src;
		public int[] dst;

		// which frame in the src and which one is the dst
		public Frame frameSrc;
		public Frame frameDst;

		public void init( int total ) {
			src = new int[total];
			dst = new int[total];
		}

		/** Number of features/observations */
		public int size() {
			return src.length;
		}

		@SuppressWarnings({"NullAway"})
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
	@SuppressWarnings({"NullAway.Init"})
	public static class Frame {
		// Unique ID assigned to this frame
		public String frameID;
		// pixel observations for all the tracks (x,y) interleaved. This is floats and not doubles since we really
		// don't need double precision for sub-pixel accuracy of visual landmarks. 1/100 is very good precision
		public float[] observations;
		// list of all the active tracks in this frame
		public long[] ids;

		// lookup table from track ID to observation index. If ID not in the set then -1 is returned
		TLongIntMap id_to_index = new TLongIntHashMap() {{
			no_entry_value = -1;
		}};

		// List of frames which are related to this one
		public final List<Frame> related = new ArrayList<>();
		// Matches to related frames.
		public final List<Match> matches = new ArrayList<>();

		public void initActive( int totalFeatures ) {
			observations = new float[totalFeatures*2];
			ids = new long[totalFeatures];
		}

		/** Number of feature observations */
		public int featureCount() {
			return ids.length;
		}

		public void getPixel( int index, Point2D_F64 out ) {
			index *= 2;
			out.x = observations[index];
			out.y = observations[index + 1];
		}

		public long getID( int index ) {
			return ids[index];
		}

		/**
		 * Returns true if this frame is matche/dconnected to 'frame'
		 */
		public boolean isMatched( Frame frame ) {
			for (int i = 0; i < matches.size(); i++) {
				Match m = matches.get(i);
				if (m.frameSrc == frame || m.frameDst == frame)
					return true;
			}
			return false;
		}

		@SuppressWarnings({"NullAway"})
		public void reset() {
			observations = null;
			ids = null;
			frameID = null;
			id_to_index.clear();
			related.clear();
			matches.clear();
		}
	}

	/** Gets a unique ID from the track */
	public interface TrackToID<Track> {
		long getId( Track track );
	}

	/** Gets the pixel coordinate of the track */
	public interface TrackToPixel<Track> {
		void getPixel( Track track, Point2D_F64 pixel );
	}
}
