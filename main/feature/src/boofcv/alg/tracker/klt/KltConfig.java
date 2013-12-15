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

package boofcv.alg.tracker.klt;

/**
 * @author Peter Abeles
 */
public class KltConfig {

	/**
	 * Due to how the image derivative and interpolation is performed outer most pixels. Features are
	 * not allowed to overlap this close to the image's edge.
	 *
	 * WARNING: currently not used.  will probably be removed.
	 */
	public int forbiddenBorder;

	/**
	 * Maximum allowed average per pixel error across the whole region.
	 */
	public float maxPerPixelError = 25;
	/**
	 * Maximum number of iterations KLT performs for each feature
	 */
	public int maxIterations = 15;  // TODO consider increasing this number? TLD seems to like it being 50
	/**
	 * Declare a feature as invalid if hte detemrinant is less than this value.
	 */
	public float minDeterminant = 0.001f;
	/**
	 * Stop iterating of the change in x and y is less than this number.
	 */
	public float minPositionDelta = 0.01f;
}
