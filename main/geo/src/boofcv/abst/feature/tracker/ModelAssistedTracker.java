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

package boofcv.abst.feature.tracker;

import boofcv.struct.image.ImageBase;

import java.util.List;

/**
 * Interface for trackers which uses a geometric model to aid in features tracking.  By using a geometric
 * model stronger assumptions can be used during association, allowing the number and quality of to be improved.
 *
 * TODO Fill out comments
 *
 * @author Peter Abeles
 */
public interface ModelAssistedTracker<T extends ImageBase,Model,Info> extends ImagePointTracker<T>
{

	/**
	 * TODO Fill out comments
	 *
	 * @param manager
	 */
	public void setTrackGeometry( TrackGeometryManager<Model,Info> manager );

	/**
	 * If true then a valid model was found and tracking was successful.
	 *
	 * @return indicates success or failure
	 */
	public boolean foundModel();

	/**
	 * The found motion model computed using the match set.  Only returns a valid
	 * solution is {@link #foundModel()} returns true.
	 *
	 * @return model.
	 */
	public Model getModel();

	/**
	 * Returns a list of tracks that were used to estimate the model. These tracks will be members
	 * of the active list.
	 *
	 * @return List of points in the match (inlier) set.
	 */
	public List<Info> getMatchSet();

	/**
	 * Active list index of an item in the match set.
	 *
	 * @param matchIndex Match set index.
	 * @return Index of the same element in the active track list.
	 */
	public int convertMatchToActiveIndex(int matchIndex);
}
