/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.tracker;

import boofcv.struct.image.ImageBase;

/**
 * Tracker that provides access to auxiliary feature location information.  The auxiliary information can specify
 * the features orientation, scale, or affine structure.  The user can also manually spawn points by providing a
 * feature's location and the auxiliary information.
 *
 * @author Peter Abeles
 */
public interface PointTrackerAux<T extends ImageBase, Aux>
		extends PointTracker<T>
{
	/**
	 * Returns auxiliary location information about the track, e.g scale, orientation, or affine.
	 *
	 * @param track Requested track.
	 * @return auxiliary location
	 */
	public Aux getAuxiliary( PointTrack track );

	/**
	 * Creates a new track at the specified pixel coordinate. Newly created tracks here are not added to the spawn list.
	 *
	 * @param x x-coordinate of new track
	 * @param y y-coordinate of new track
	 * @param auxiliary Auxiliary coordinate information.
	 * @return The newly created track.  null if a track could not be created there
	 */
	PointTrack addTrack( double x , double y , Aux auxiliary );
}
