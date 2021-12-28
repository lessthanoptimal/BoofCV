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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity;

/**
 * <p>
 * Implementation of {@link boofcv.alg.feature.detect.intensity.ShiTomasiCornerIntensity}.
 * </p>
 *
 * @author Peter Abeles
 */
public class ShiTomasiCorner_S32 implements ImplSsdCornerBase.CornerIntensity_S32, ShiTomasiCornerIntensity {
	@Override
	public float compute( int totalXX, int totalXY, int totalYY ) {
		// compute the smallest eigenvalue
		double left = (totalXX + totalYY)*0.5;
		double b = (totalXX - totalYY)*0.5;
		double right = Math.sqrt(b*b + ((double)totalXY)*totalXY);

		// the smallest eigenvalue will be minus the right side
		return (float)(left - right);
	}
}
