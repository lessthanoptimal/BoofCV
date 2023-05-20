/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.disparity;

import boofcv.alg.disparity.DisparityBlockMatchBestFive;
import boofcv.struct.KernelRadius2D;

/**
 * A block matching algorithm which improved performance along edges by finding the score for 9 regions but only
 * selecting the 5 best.
 *
 * @author Peter Abeles
 * @see DisparityBlockMatchBestFive
 */
public class ConfigDisparityBMBest5 extends ConfigDisparityBM {

	public ConfigDisparityBMBest5() {
		// Empirically determined that this needed to be set to a lower value
		catastrophicReset = 20;
	}

	@Override public KernelRadius2D getBlockSize() {
		int radiusX = (2*regionRadiusX + 1)*3/2;
		int radiusY = (2*regionRadiusY + 1)*3/2;

		return new KernelRadius2D(radiusX, radiusY);
	}

	public ConfigDisparityBMBest5 setTo( ConfigDisparityBMBest5 src ) {
		super.setTo(src);
		return this;
	}
}
