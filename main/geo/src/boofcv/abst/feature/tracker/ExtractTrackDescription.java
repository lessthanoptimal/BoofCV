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

/**
 * Interface which allows the description of a track to be extracted.  The description can then be used to
 * register objects and images together.
 *
 * @author Peter Abeles
 */
public interface ExtractTrackDescription<Desc extends TupleDesc> extends DescriptorInfo<Desc>
{
	/**
	 * Extracts the feature description from the point track.  The returned description will belong to the track
	 * and documentation on the tracker should be consulted to see if it's modified.
	 *
	 * @param track A track from this tracker.
	 * @return Description of the feature being tracked.
	 */
	public Desc extractDescription(PointTrack track);
}
