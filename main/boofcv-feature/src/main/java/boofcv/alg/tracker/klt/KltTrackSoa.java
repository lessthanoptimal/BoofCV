/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.klt;

/// [PyramidKltFeature] stored in a Structure-of-Array format for efficient memory access
/// and storage.
public class KltTrackSoa {
	/// radius of the descriptor template
	public final int radius;
	/// Number of levels in the pyramid
	public final int levels;
	/// Location of track in full resolution image
	public float[] x, y;
	/// Frame the track was spawned in
	public long[] frameSpawned;
	/// Unique ID assigned to the track
	public long[] id;
	/// Status enum, see [KltTrackFault].
	public byte[] status;
	/// location and description in each level
	public float[] levelX, levelY, levelDesc, levelDerivX, levelDerivY;
	/// Level specific hessian
	public float[] levelGxx, levelGxy, levelGyy;

	public KltTrackSoa( int radius, int levels ) {
		this.radius = radius;
		this.levels = levels;
	}

	/// Number of active tracks
	public int size() {
		return -1;
	}

	/// How many tracks have memory allocated for them before it needs to grow
	public int allocated() {
		return -1;
	}

	/// Allocate enough memory so that this many tracks can be stored
	public void allocate( int size ) {

	}

	public int add( KltFeature track, EasyPyramidKlt.TrackMeta meta ) {
		// See if all available storage has allocated already
		if (size() >= allocated()) {
			// Double the number of track storage and handle the case where
			// there are only a few tracks initially
			allocate(10 + size()*2);
		}
		return -1;
	}

	public void remove( int index ) {

	}

	public void get( int index,KltFeature track, EasyPyramidKlt.TrackMeta meta ) {

	}
}
