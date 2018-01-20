/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.filter.binary;

import boofcv.alg.filter.binary.ThresholdBlockOtsu;
import boofcv.struct.ConfigLength;

/**
 * Configuration for all threshold types.
 *
 * @author Peter Abeles
 */
public class ConfigThresholdLocalOtsu extends ConfigThreshold {

	/**
	 * Tuning parameter that will make it harder for pixels to be marked in low texture regions.
	 * 0 = regular Otsu. Try 15 when tuning.
	 *
	 * @see ThresholdBlockOtsu
	 */
	public double tuning=0;

	/**
	 * If true it will use Otsu 2. Produce much better results in binary or nearly binary images.
	 *
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeOtsu2(int[], int, int)
	 */
	public boolean useOtsu2 = true;

	public ConfigThresholdLocalOtsu(int regionWidth , double tuning) {
		this(ConfigLength.fixed(regionWidth),tuning);
	}

	public ConfigThresholdLocalOtsu(ConfigLength regionWidth , double tuning) {
		this();
		this.width =regionWidth;
		this.tuning = tuning;
	}

	public ConfigThresholdLocalOtsu() {
		this.type = ThresholdType.BLOCK_OTSU;
	}
}
