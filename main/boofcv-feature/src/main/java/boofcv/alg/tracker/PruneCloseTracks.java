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

package boofcv.alg.tracker;

import boofcv.abst.tracker.PointTrack;
import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Detects if tracks are too close together and discards some of the close ones. Tracks are projected into
 * a smaller grid (specified by scale) and if more than one lands on the same grid element it is pruned. An ambiguity
 * resolver is used to decide which track to keep if there's a conflict. If the resolver says they are equivalent
 * then featureID is used. Results should be consistent independent of order in tracks list.
 *
 * This is designed to work with different types of track data structures.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class PruneCloseTracks<T> {
	/** Number of pixels away that two objects can be before they are considered to be in conflict */
	@Setter @Getter private int radius;

	// size of the grid
	private int gridWidth, gridHeight;

	// Way to look up all the tracks inside a grid. Each element is null if there are no tracks inside
	private List<TrackDrop<T>>[] gridToCell = new List[0];

	DogArray<List<TrackDrop<T>>> storageLists = new DogArray<>(ArrayList::new, List::clear);
	DogArray<TrackDrop<T>> storageDrops = new DogArray<>(TrackDrop::new, TrackDrop::reset);

	/**
	 * Used to extract information about a track. Enables this class to work with different formats.
	 */
	private TrackInfo<T> trackInfo;

	/**
	 * Specifies how it's decided which track remains when there's a conflict. By default lower featureID's
	 * are preferred. The idea is that they are older tracks and unlike spawn time there is no ambiguity
	 *
	 * <pre>
	 *  1 = 'a' is preferred over 'b'
	 *  0 = both are equivalent
	 * -1 =  'b' is preferred over 'a'
	 * </pre>
	 */
	@Setter Comparator<T> ambiguityResolver = ( a, b ) -> Long.compare(trackInfo.getID(b), trackInfo.getID(a));

	// workspace variables
	private final Point2D_F64 candidatePt = new Point2D_F64();
	private final Point2D_F64 currentPt = new Point2D_F64();

	public PruneCloseTracks( int radius, TrackInfo<T> trackInfo ) {
		this.radius = radius;
		this.trackInfo = trackInfo;
	}

	/**
	 * Initializes data structures for the specified input image size
	 */
	public void init( int imgWidth, int imgHeight ) {
		// +1 to avoid out of bounds error along image border
		gridWidth = imgWidth/radius + 1;
		gridHeight = imgHeight/radius + 1;
		final int N = gridWidth*gridHeight;

		if (gridToCell.length < N) {
			gridToCell = (List<TrackDrop<T>>[])new List[N];
		}
	}

	/**
	 * Processes existing tracks and adds tracks to drop list if they are too close to other tracks and
	 * considered less desirable
	 *
	 * @param tracks (Input) List of tracks. Not modified.
	 * @param dropTracks (Output) List of tracks that need to be dropped by the tracker
	 */
	public void process( List<T> tracks, List<T> dropTracks ) {

		// Initialize data structures and reset into their initial state
		final int w = gridWidth;
		final int h = gridHeight;
		dropTracks.clear();
		storageDrops.reset();
		storageLists.reset();
		Arrays.fill(gridToCell, 0, w*h, null);

		// Go through all the tracks and use the local grid neighborhood to quickly find potential neighbors
		for (int i = 0; i < tracks.size(); i++) {
			final T candidate = tracks.get(i);
			trackInfo.getLocation(candidate, candidatePt);
			long candidateID = trackInfo.getID(candidate);

			final int cx = (int)(candidatePt.x/radius);
			final int cy = (int)(candidatePt.y/radius);

			// Find the local neighborhood taking in account the image border
			int x0 = cx - 1;
			int x1 = cx + 2;
			int y0 = cy - 1;
			int y1 = cy + 2;
			if (x0 < 0) x0 = 0;
			if (x1 > w) x1 = w;
			if (y0 < 0) y0 = 0;
			if (y1 > h) y1 = h;

			int centerIndex = cy*w + cx;

			// Search through the local neighborhood for conflicts
			boolean candidateDropped = false;
			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					int index = y*w + x;

					final List<TrackDrop<T>> cell = gridToCell[index];
					if (cell == null)
						continue;
					candidateDropped = isCandidateDropped(dropTracks, candidate, candidateID, candidateDropped, cell);
				}
			}

			// add candidate to the dropped list if it was dropped
			if (candidateDropped) {
				dropTracks.add(candidate);
			}

			// Add the track to a cell
			List<TrackDrop<T>> cell = gridToCell[centerIndex];
			if (cell == null) {
				gridToCell[centerIndex] = cell = storageLists.grow();
				BoofMiscOps.checkTrue(cell.size() == 0);
			}

			TrackDrop<T> td = storageDrops.grow();
			td.track = candidate;
			td.dropped = candidateDropped;
			cell.add(td);
		}
	}

	private boolean isCandidateDropped( List<T> dropTracks, T candidate, long candidateID, boolean candidateDropped,
										List<TrackDrop<T>> cell ) {
		// Note the use of manhattan distance and not cartesian? This ensures that items in the same cell
		// are in conflict AND that all neighbors are within 1 block
		for (int trackIdx = 0; trackIdx < cell.size(); trackIdx++) {
			TrackDrop<T> td = cell.get(trackIdx);

			trackInfo.getLocation(td.track, currentPt);
			if (Math.abs(currentPt.x - candidatePt.x) >= radius || Math.abs(currentPt.y - candidatePt.y) >= radius) {
				continue;
			}

			// Use the ambiguity resolver to select one of them to keep
			int result = ambiguityResolver.compare(candidate, td.track);
			if (result < 0) {
				// The current track is preferred
				candidateDropped = true;
			} else if (result == 0 && trackInfo.getID(td.track) < candidateID) {
				// Results are ambiguous, but the tie breaker is done using featureID so the candidate
				// is still dropped
				candidateDropped = true;
			} else {
				// the candidate track is preferred so drop the current track.
				// If it has not already been dropped
				if (!td.dropped) {
					dropTracks.add(td.track);
					td.dropped = true;
				}
			}
		}
		return candidateDropped;
	}

	@SuppressWarnings({"NullAway.Init"})
	protected static class TrackDrop<TD> {
		public TD track;
		public boolean dropped;

		@SuppressWarnings({"NullAway"})
		public void reset() {
			track = null;
			dropped = false;
		}
	}

	/**
	 * Interface which allows multiple track data structures be passed in
	 */
	public interface TrackInfo<T> {
		/**
		 * Location of the track inside the image. It's assumed the track is inside the image.
		 */
		void getLocation( T track, Point2D_F64 location );

		/**
		 * Unique ID. It is assumed the older tracks have a lower ID
		 */
		long getID( T track );
	}

	/**
	 * Convenience function for creating a variant for PointTrack
	 */
	public static PruneCloseTracks<PointTrack> prunePointTrack( int radius ) {
		return new PruneCloseTracks<>(radius, new TrackInfo<>() {
			@Override
			public void getLocation( PointTrack track, Point2D_F64 location ) {
				location.setTo(track.pixel);
			}

			@Override
			public long getID( PointTrack track ) {
				return track.featureId;
			}
		});
	}
}
