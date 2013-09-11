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

package boofcv.alg.feature.associate;

import boofcv.struct.feature.AssociatedIndex;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Given a list of associated features, find all the unassociated features.
 *
 * @author Peter Abeles
 */
public class FindUnassociated {

	// list of indexes in source which are unassociated
	GrowQueue_I32 unassociatedSrc = new GrowQueue_I32();
	GrowQueue_I32 unassociatedDst = new GrowQueue_I32();
	// list that indicates what was associated in the source list
	GrowQueue_I32 matched = new GrowQueue_I32();

	public GrowQueue_I32 checkSource( FastQueue<AssociatedIndex> matches , int num ) {
		matched.resize(num);
		for( int i = 0; i < matched.size; i++ ) {
			matched.data[i] = 0;
		}
		for( int i = 0; i < matches.size; i++ ) {
			matched.data[matches.get(i).src] = 1;
		}

		unassociatedSrc.reset();
		for( int i = 0; i < matched.size; i++ ) {
			if( matched.data[i] == 0 ) {
				unassociatedSrc.add(i);
			}
		}
		return unassociatedSrc;
	}

	public GrowQueue_I32 checkDestination( FastQueue<AssociatedIndex> matches , int num ) {
		matched.resize(num);
		for( int i = 0; i < matched.size; i++ ) {
			matched.data[i] = 0;
		}
		for( int i = 0; i < matches.size; i++ ) {
			matched.data[matches.get(i).dst] = 1;
		}

		unassociatedDst.reset();
		for( int i = 0; i < matched.size; i++ ) {
			if( matched.data[i] == 0 ) {
				unassociatedDst.add(i);
			}
		}
		return unassociatedDst;
	}
}
