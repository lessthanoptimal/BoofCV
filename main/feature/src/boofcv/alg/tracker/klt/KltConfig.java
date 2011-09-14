/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
	 */
	public int forbiddenBorder;

	public float maxPerPixelError;
	public int maxIterations;
	public float minDeterminant;
	public float minPositionDelta;

	/**
	 * Creates a config with reasonable values for most applications.  These should be tuned
	 * for better performance.
	 *
	 */
	public static KltConfig createDefault() {
		KltConfig ret = new KltConfig();
		ret.maxPerPixelError = 20;
		ret.maxIterations = 15;
		ret.minDeterminant = 0.001f;
		ret.minPositionDelta = 0.01f;

		return ret;
	}
}
