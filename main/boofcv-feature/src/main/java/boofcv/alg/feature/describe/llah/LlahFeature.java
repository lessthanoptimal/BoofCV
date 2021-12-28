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

package boofcv.alg.feature.describe.llah;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Describes a LLAH feature. Says which document it appeared in, which landmark it belongs to and it's invariants.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class LlahFeature {
	/**
	 * If there's a hash collision
	 */
	public @Nullable LlahFeature next;
	public int documentID;
	public int landmarkID;
	public final int[] invariants;
	/**
	 * Hash code for this feature
	 */
	public int hashCode;

	public LlahFeature( int numInvariants ) {
		invariants = new int[numInvariants];
	}

	public void reset() {
		this.next = null;
		this.documentID = -1;
		this.landmarkID = -1;
		Arrays.fill(invariants, -1);
	}

	/**
	 * Returns true if the invariants are identical
	 */
	public boolean doInvariantsMatch( LlahFeature f ) {
		for (int i = 0; i < invariants.length; i++) {
			if (f.invariants[i] != this.invariants[i])
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}
}
