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

package boofcv.alg.tracker;

import boofcv.abst.tracker.PointTrack;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Detects if tracks are too close together and discards some of the close ones.  Tracks are projected into
 * a smaller grid (specified by scale) and if more than one lands on the same grid element it is pruned. An ambiguity
 * resolver is used to decide which track to keep if there's a conflict. If the resolver says they are equivalent
 * then featureID is used. Results should be consistent independent of order in tracks list.
 *
 * This is designed to work with different types of track data structures.
 *
 * @author Peter Abeles
 */
public class PruneCloseTracks<T> {
	/** Number of pixels away that two objects can be before they are considered to be in conflict */
	@Setter @Getter	private int radius;

	// size of the grid
	private int gridWidth,gridHeight;

	private T[] trackImage = (T[])new Object[0];
	// indicates if the occupant of a cell has been dropped or not. This is needed to avoid dropping
	// a track twice. Looking up the track in a list could be an expensive operation
	private boolean[] dropImage = new boolean[0];

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
	@Setter Comparator<T> ambiguityResolver = (a, b) -> Long.compare( trackInfo.getID(b),trackInfo.getID(a));

	// workspace variables
	private Point2D_F64 candidatePt = new Point2D_F64();
	private Point2D_F64 currentPt = new Point2D_F64();

	public PruneCloseTracks(int radius, TrackInfo<T> trackInfo ) {
		this.radius = radius;
		this.trackInfo = trackInfo;
	}

	/**
	 * Initializes data structures for the specified input image size
	 */
	public void init(int imgWidth , int imgHeight ) {
		// +1 to avoid out of bounds error along image border
		gridWidth = imgWidth / radius +1;
		gridHeight = imgHeight / radius +1;
		final int N = gridWidth*gridHeight;
		
		if( trackImage.length < N ) {
			trackImage = (T[])new Object[ N ];
			dropImage = new boolean[ N ];
		}
	}

	/**
	 * Processes existing tracks and adds tracks to drop list if they are too close to other tracks and
	 * considered less desirable
	 * @param tracks (Input) List of tracks
	 * @param dropTracks (Output)
	 */
	public void process( List<T> tracks , List<T> dropTracks ) {

		dropTracks.clear();

		double tol = radius*radius;
		final int w = gridWidth;
		final int h = gridHeight;

		Arrays.fill(dropImage,0,w*h, false);

		for( int i = 0; i < tracks.size(); i++ ) {
			final T candidate = tracks.get(i);
			trackInfo.getLocation(candidate,candidatePt);
			long candidateID = trackInfo.getID(candidate);

			final int cx = (int)(candidatePt.x/radius);
			final int cy = (int)(candidatePt.y/radius);

			// Find the local neighborhood taking in account the image border
			int x0 = cx-1;
			int x1 = cx+2;
			int y0 = cy-1;
			int y1 = cy+2;
			if( x0 < 0 ) x0 = 0;
			if( x1 > w ) x1 = w;
			if( y0 < 0 ) y0 = 0;
			if( y1 > h ) y1 = h;

			int centerIndex = cy*w+cx;

			// Search through the local neighborhood for conflicts
			boolean candidateDropped = false;
			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					int index = y*w+x;

					final T current = trackImage[index];
					if( current == null )
						continue;

					// See if they are in conflict
					trackInfo.getLocation(current,currentPt);
					if( currentPt.distance2(candidatePt) > tol )
						continue;

					// Use the ambiguity resolver to select one of them to keep
					int result = ambiguityResolver.compare(candidate,current);
					if( result < 0 ) {
						// The current track is preferred
						candidateDropped = true;
					} else if( result == 0 && trackInfo.getID(current) < candidateID ) {
						// Results are ambiguous, but the tie breaker is done using featureID so the candidate
						// is still dropped
						candidateDropped = true;
					} else {
						// the candidate track is preferred so drop the current track.
						// If it has not already been dropped
						if( !dropImage[index] ) {
							dropTracks.add(trackImage[index]);
							dropImage[index] = true;
						}
					}
				}
			}

			// add candidate to the dropped list if it was dropped
			if( candidateDropped ) {
				dropTracks.add(candidate);
			}
			// if the cell was empty add the candidate to it and mark it's drop status
			if((trackImage[centerIndex] == null) || !candidateDropped) {
				trackImage[centerIndex] = candidate;
				dropImage[centerIndex] = candidateDropped;
			}
		}

		// clear the track image so that it will be null on the next call and so that it doesn't
		// save references to objects when it's not going to use them again
		Arrays.fill(trackImage,0,w*h,null);
	}

	/**
	 * Interface which allows multiple track data structures be passed in
	 */
	public interface TrackInfo<T> {
		/**
		 * Location of the track inside the image. It's assumed the track is inside the image.
		 */
		void getLocation(T track , Point2D_F64 location );

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
			public void getLocation(PointTrack track, Point2D_F64 location) {
				location.set(track);
			}

			@Override
			public long getID(PointTrack track) {
				return track.featureId;
			}
		});
	}
}
