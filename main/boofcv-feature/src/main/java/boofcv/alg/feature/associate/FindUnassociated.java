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

package boofcv.alg.feature.associate;

import boofcv.struct.feature.AssociatedIndex;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;

/**
 * Given a list of associated features, find all the unassociated features.
 *
 * @author Peter Abeles
 */
public class FindUnassociated {

	// list of indexes in source which are unassociated
	DogArray_I32 unassociatedSrc = new DogArray_I32();
	DogArray_I32 unassociatedDst = new DogArray_I32();
	// list that indicates what was associated in the source list
	DogArray_B matched = new DogArray_B();

	/**
	 * Finds unassociated features in source
	 *
	 * @param matches List of matched features
	 * @param featureCount Number of source features
	 * @return indexes of unassociated features from source
	 */
	public DogArray_I32 checkSource( FastAccess<AssociatedIndex> matches, int featureCount ) {
		matched.resize(featureCount);
		matched.fill(false);

		for (int i = 0; i < matches.size; i++) {
			matched.data[matches.get(i).src] = true;
		}

		unassociatedSrc.reset();
		for (int i = 0; i < featureCount; i++) {
			if (!matched.data[i]) {
				unassociatedSrc.add(i);
			}
		}
		return unassociatedSrc;
	}

	/**
	 * Finds unassociated features in destination
	 *
	 * @param matches List of matched features
	 * @param featureCount Number of destination features
	 * @return indexes of unassociated features from destination
	 */
	public DogArray_I32 checkDestination( FastAccess<AssociatedIndex> matches, final int featureCount ) {
		matched.resize(featureCount);
		matched.fill(false);

		for (int i = 0; i < matches.size; i++) {
			matched.data[matches.get(i).dst] = true;
		}

		unassociatedDst.reset();
		for (int i = 0; i < featureCount; i++) {
			if (!matched.data[i]) {
				unassociatedDst.add(i);
			}
		}
		return unassociatedDst;
	}
}
