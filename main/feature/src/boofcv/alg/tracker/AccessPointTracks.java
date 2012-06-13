/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * Provides access to the location of point tracks.  Location of tracks are provided in terms
 * of pixel coordinates.
 *
 * @author Peter Abeles
 */
public interface AccessPointTracks {

	/**
	 * Used to get the track ID of an active Track
	 *
	 * @param index which track
	 * @return The track's ID
	 */
	public long getTrackId( int index );

	/**
	 * All the points being actively tracked
	 *
	 * @return all active tracks
	 */
	public List<Point2D_F64> getAllTracks();

	/**
	 * Tracks which are inliers to some algorithm.  For example, inlier set when estimating
	 * the camera's motion
	 *
	 * @return inlier features
	 */
	public List<Point2D_F64> getInlierTracks();

	/**
	 * Tracks that were just recently spawned
	 *
	 * @return newly spawned tracks
	 */
	public List<Point2D_F64> getNewTracks();
}
