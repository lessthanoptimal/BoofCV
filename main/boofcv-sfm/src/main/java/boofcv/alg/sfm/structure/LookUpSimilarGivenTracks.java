/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import gnu.trove.impl.Constants;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I64;

import java.util.ArrayList;
import java.util.List;

/**
 * Given a set of feature tracks across several images, convert into {@link LookUpSimilarImages}.
 *
 * @author Peter Abeles
 */
public class LookUpSimilarGivenTracks<Track> implements LookUpSimilarImages {

	TrackToID<Track> lookupID;
	TrackToPixel<Track> lookupPixel;

	DogArray<Frame> frames = new DogArray<>(Frame::new, Frame::reset);
	List<String> frameIds = new ArrayList<>();

	DogArray_I64 keys = new DogArray_I64();

	public LookUpSimilarGivenTracks( TrackToID<Track> lookupID, TrackToPixel<Track> lookupPixel ) {
		this.lookupID = lookupID;
		this.lookupPixel = lookupPixel;
	}

	public void reset() {
		frames.reset();
		frameIds.clear();
	}

	/**
	 * Creates a new image/frame from the tracks. It's ID will be the index in the frame list
	 *
	 * @param tracks List of tracks
	 */
	public void addFrame( int width, int height, List<Track> tracks ) {
		Frame f = frames.grow();
		f.shape.setTo(width, height);
		f.pixels.reserve(tracks.size());
		f.id = "" + (frames.size - 1);
		frameIds.add(f.id);

		for (int pixelIdx = 0; pixelIdx < tracks.size(); pixelIdx++) {
			Track t = tracks.get(pixelIdx);
			long id = lookupID.getId(t);
			f.trackToIndex.put(id, pixelIdx);
			lookupPixel.getPixel(t, f.pixels.grow());
		}
	}

	/**
	 * Determines which frames/images have a relationship with each other. A relationship is determined by having
	 * at least minCommon features in common with each other. A brute force O(N^2) algorithm is used
	 *
	 * @param sequence If true it is assumed to be a sequence where the number of matches only decreases
	 * @param minCommon Minimum number of common features between two similar images.
	 */
	public void computeSimilarRelationships( boolean sequence, int minCommon ) {
		// Make sure everything is in the initial state
		for (int idxA = 0; idxA < frames.size; idxA++) {
			frames.get(idxA).similar.clear();
		}
		// Brute force O(N^2) checks to see if they are related
		for (int idxA = 0; idxA < frames.size; idxA++) {
			Frame frameA = frames.get(idxA);
			for (int idxB = idxA + 1; idxB < frames.size; idxB++) {
				Frame frameB = frames.get(idxB);
				int common = countCommon(frameA, frameB);

				// See if there are two few
				if (common < minCommon) {
					// if a sequence we assume the number of matches only goes down
					if (sequence)
						break;
					continue;
				}

				// Add the relationship between these two frames
				frameA.similar.add(frameB);
				frameB.similar.add(frameA);
			}
		}
	}

	/**
	 * Counts the number of common features between the two frames
	 */
	int countCommon( Frame frameA, Frame frameB ) {
		// Avoid creating/destroying memory when getting a set of keys in this frame
		keys.resize(frameA.trackToIndex.size());
		frameA.trackToIndex.keys(keys.data);

		// Count the number of keys/track Ids
		int total = 0;
		for (int i = 0; i < keys.size; i++) {
			long key = keys.get(i);
			if (frameB.trackToIndex.containsKey(key))
				total++;
		}

		return total;
	}

	/** Give the frame ID find the frame. Throw an exception of the target isn't known */
	Frame getFrame( String target ) {
		int index = Integer.parseInt(target);
		if (index < 0 || index >= frames.size)
			throw new IllegalArgumentException("Invalid or out of range target. " + target);
		Frame f = frames.get(index);
		return f;
	}

	@Override public List<String> getImageIDs() {
		return frameIds;
	}

	@Override public void findSimilar( String target, List<String> similar ) {
		Frame f = getFrame(target);

		similar.clear();
		for (int i = 0; i < f.similar.size(); i++) {
			similar.add(f.similar.get(i).id);
		}
	}

	@Override public void lookupPixelFeats( String target, DogArray<Point2D_F64> features ) {
		Frame f = getFrame(target);

		features.reset();
		features.resize(f.pixels.size);

		for (int i = 0; i < f.pixels.size; i++) {
			features.get(i).setTo(f.pixels.get(i));
		}
	}

	@Override public boolean lookupMatches( String viewA, String viewB, DogArray<AssociatedIndex> pairs ) {
		pairs.reset();

		Frame frameA = getFrame(viewA);
		Frame frameB = getFrame(viewB);

		// See if the two views have a similar relationship
		if (!frameA.similar.contains(frameB))
			return false;

		// Look up all the feature IDs in frameA
		keys.resize(frameA.trackToIndex.size());
		frameA.trackToIndex.keys(keys.data);

		// Create index pairs of common features
		for (int i = 0; i < keys.size; i++) {
			long key = keys.get(i);
			int idxB = frameB.trackToIndex.get(key);
			if (idxB < 0)
				continue;
			int idxA = frameA.trackToIndex.get(key);
			pairs.grow().setTo(idxA, idxB);
		}

		return true;
	}

	@Override public void lookupShape( String target, ImageDimension shape ) {
		shape.setTo(getFrame(target).shape);
	}

	static class Frame {
		// shape of original image
		final ImageDimension shape = new ImageDimension();
		final DogArray<Point2D_F64> pixels = new DogArray<>(Point2D_F64::new);
		// Looks up track to index and uses a value of -1 when there's no entry
		final TLongIntMap trackToIndex = new TLongIntHashMap(
				Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
		final List<Frame> similar = new ArrayList<>();
		String id = "";

		void reset() {
			shape.setTo(-1, -1);
			pixels.reset();
			trackToIndex.clear();
			similar.clear();
			id = "";
		}
	}

	public interface TrackToID<Track> {
		long getId( Track track );
	}

	public interface TrackToPixel<Track> {
		void getPixel( Track track, Point2D_F64 pixel );
	}
}
