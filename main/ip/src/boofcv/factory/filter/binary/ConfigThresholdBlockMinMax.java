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

package boofcv.factory.filter.binary;

/**
 * Configuration for {@link boofcv.abst.filter.binary.LocalSquareBlockMinMaxBinaryFilter}
 *
 * @author Peter Abeles
 */
public class ConfigThresholdBlockMinMax extends ConfigThreshold {
	/**
	 * If the lower and upper histogram values are different by less than or equal to this amount it is considered
	 * a textureless region.  Set to a value <= -1 to disable.
	 */
	public double minimumSpread = 10;

	public ConfigThresholdBlockMinMax(int radius , double minimumSpread, boolean down ) {
		this.type = ThresholdType.LOCAL_SQUARE_BLOCK_MIN_MAX;
		this.radius = radius;
		this.minimumSpread = minimumSpread;
		this.down = down;
	}

	public ConfigThresholdBlockMinMax() {
	}

	{
		scale = 0.85;
	}

	@Override
	public void checkValidity() {
		super.checkValidity();
	}
}
