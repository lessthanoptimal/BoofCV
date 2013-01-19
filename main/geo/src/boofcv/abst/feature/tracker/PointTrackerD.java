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

import boofcv.abst.feature.describe.DescriptorInfo;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;

/**
 * Extension of {@link PointTracker} which allows feature descriptions to be extracted from the tracks.
 *
 * @author Peter Abeles
 *
 * @param <T>  Type of input image.
 * @param <Desc> Type of feature descriptor.
 */
public interface PointTrackerD<T extends ImageBase, Desc extends TupleDesc>
		extends PointTracker<T> , DescriptorInfo<Desc>
{
// TODO other functions in the tracker need to be done by set also
// e.g. get all new by set
// perhaps, instead of getBlah( int set ) do setActiveSet( int set ) then getBlah() will be for that set
//	/**
//	 * The number of feature sets.
//	 *
//	 * @return number of feature sets
//	 */
//	public int getNumberOfSets();
//
//	/**
//	 * Returns a list of tracks as segmented by the method they were detected and tracked by.  When associated,
//	 * only features which are from the same set should be associated together.
//	 *
//	 * @param set Which set should it return.
//	 * @return List of point tracks belonging to the specified set.
//	 */
//	public FastQueue<PointTrack> getFeatureSet( int set );

	/**
	 * Extracts the feature description from the point track.  The returned description will belong to the track
	 * and documentation on the tracker should be consulted to see if it's modified.
	 *
	 * @param track A track from this tracker.
	 * @return Description of the feature being tracked.
	 */
	public Desc extractDescription( PointTrack track );
}
