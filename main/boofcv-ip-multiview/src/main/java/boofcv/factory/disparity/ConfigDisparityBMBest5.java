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

package boofcv.factory.disparity;

import boofcv.alg.disparity.DisparityBlockMatchBestFive;

/**
 * A block matching algorithm which improved performance along edges by finding the score for 9 regions but only
 * selecting the 5 best.
 *
 * @see DisparityBlockMatchBestFive
 *
 * @author Peter Abeles
 */
public class ConfigDisparityBMBest5 extends ConfigDisparityBM {
	public ConfigDisparityBMBest5 setTo( ConfigDisparityBMBest5 src ) {
		super.setTo(src);
		return this;
	}
}
