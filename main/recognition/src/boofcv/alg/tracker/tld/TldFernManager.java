/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.tld;

import java.util.Stack;

/**
 * Lookup table for ferns. An array is used to look up each fern by value.  The recommend descriptor size in the
 * paper is 10 bits, which results in a reasonably sized table.
 *
 * @author Peter Abeles
 */
public class TldFernManager {

	// lookup table for ferns.  The value of the fern is an element in the table
	TldFernFeature table[];

	// storage for unused ferns which can be recycled
	Stack<TldFernFeature> unusedFern = new Stack<>();

	/**
	 * Configures the manager
	 *
	 * @param descriptorSize Size of the fern's descriptor
	 */
	public TldFernManager(int descriptorSize) {
		int N = 1 << descriptorSize;

		table = new TldFernFeature[N];
	}

	/**
	 * Looks up the fern with the specified value.  If non exist a new one is created and returned.
	 * @param value The fern's value
	 * @return The fern associated with that value
	 */
	public TldFernFeature lookupFern( int value ) {
		TldFernFeature found = table[value];
		if( found == null ) {
			found = createFern();
			found.init(value);
			table[value] = found;
		}
		return found;
	}

	/**
	 * Looks up the posterior probability of the specified fern.  If a fern is found its posterior is returned
	 * otherwise -1 is returned.
	 *
	 * NOTE: How unknown values are handled is a deviation from the paper.
	 *
	 * @param value The fern's value
	 * @return Fern's posterior probability.  If the value is known then return -1
	 */
	public double lookupPosterior( int value ) {
		TldFernFeature found = table[value];
		if( found == null ) {
			return 0;
		}
		return found.posterior;
	}

	public void reset() {
		for( int i = 0; i < table.length; i++ ) {
			TldFernFeature f = table[i];
			if( f != null ) {
				unusedFern.add(f);
				table[i] = null;
			}
		}
	}

	protected TldFernFeature createFern() {
		if( unusedFern.size() > 0 ) {
			return unusedFern.pop();
		}
		return new TldFernFeature();
	}

}
