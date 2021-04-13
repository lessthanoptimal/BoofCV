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

package boofcv.alg.scene.bow;

import boofcv.misc.BoofLambdas;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.FastAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * Utility functions related to Bag-of-Words methods
 *
 * @author Peter Abeles
 */
public class BowUtils {
	/**
	 * When dealing with 1 million images using quick select first makes a significant improvement
	 * Sorting everything was taking 300 to 500 ms before and after it was taking 15 to 45 ms.
	 */
	public static void filterAndSortMatches(
			FastAccess<BowMatch> matches, @Nullable BoofLambdas.FilterInt filter, int limit ) {
		if (filter !=null) {
			// Iterate until the limit the best 'limit' matches pass the filter
			int startIdx = 0;
			while (startIdx < matches.size) {
				// Select first set of possible matches
				if (limit < matches.size)
					QuickSelect.select(matches.data, limit-startIdx, startIdx, matches.size);

				// Filter these. This will potentially mess up the order
				int localLimit = Math.min(limit, matches.size);
				for (int i = startIdx; i < localLimit;) {
					if (filter.keep(matches.get(i).identification)) {
						i++;
						continue;
					}
					// Swap an element at the end of the list the quick sort region in to the current location
					matches.swap(i, localLimit-1);
					// Take the mach which got filtered out and remove it from the match list entirely
					matches.removeSwap(localLimit-1);
					// quick sort region is now smaller
					localLimit--;
				}

				// If the local limit is the same as the hard limit then stop
				if (localLimit==Math.min(limit, matches.size))
					break;
				startIdx += localLimit - startIdx;
			}
			matches.size = Math.min(matches.size, limit);
		} else {
			// Very simple logic if there's no filter
			if (matches.size > limit) {
				// Quick select ensures that the first N elements have the smallest N values, but they are not ordered
				QuickSelect.select(matches.data, limit, matches.size);
				matches.size = limit;
			}
		}
		Collections.sort(matches.toList());
	}
}
