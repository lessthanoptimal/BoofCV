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

package boofcv.factory.filter.binary;

import boofcv.struct.ConfigLength;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for {@link boofcv.alg.filter.binary.ThresholdBlockMinMax}
 *
 * @author Peter Abeles
 */
@Getter @Setter
public class ConfigThresholdBlockMinMax extends ConfigThreshold {
	/**
	 * If the lower and upper histogram values are different by less than or equal to this amount it is considered
	 * a textureless region. Set to a value <= -1 to disable.
	 */
	public double minimumSpread = 10;

	{
		scale = 0.85;
	}

	public ConfigThresholdBlockMinMax( int width, double minimumSpread, boolean down ) {
		this(ConfigLength.fixed(width), minimumSpread, down);
	}

	public ConfigThresholdBlockMinMax( ConfigLength width, double minimumSpread, boolean down ) {
		this.type = ThresholdType.BLOCK_MIN_MAX;
		this.width = width;
		this.minimumSpread = minimumSpread;
		this.down = down;
	}

	public ConfigThresholdBlockMinMax() {
	}

	public ConfigThresholdBlockMinMax setTo( ConfigThresholdBlockMinMax src ) {
		super.setTo(src);
		this.minimumSpread = src.minimumSpread;
		return this;
	}

	@Override
	public void checkValidity() {
		super.checkValidity();
	}
}
