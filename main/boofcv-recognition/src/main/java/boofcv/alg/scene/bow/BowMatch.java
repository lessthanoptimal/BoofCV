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

import org.jetbrains.annotations.NotNull;

/**
 * Common image match data type for Bag-of-Words methods
 *
 * @author Peter Abeles
 */
public class BowMatch implements Comparable<BowMatch> {
	/** Initially stores the image index, but is then converted into the image ID for output */
	public int identification;
	/** The error between this image's descriptor and the query image. 0.0 = perfect match */
	public float error;

	public void reset() {
		identification = -1;
		error = 2.0f; // See MapDistanceNorm for why the error initially has to be this value
	}

	@Override public int compareTo( @NotNull BowMatch o ) {
		return Float.compare(error, o.error);
	}
}
