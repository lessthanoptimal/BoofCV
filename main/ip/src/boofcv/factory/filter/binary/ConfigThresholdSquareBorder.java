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
 * Configuration for {@link boofcv.abst.filter.binary.LocalSquareBorderBinaryFilter}
 *
 * @author Peter Abeles
 */
public class ConfigThresholdSquareBorder extends ConfigThreshold {

	/**
	 * Lower histogram value's fractional value.  Used to compute spread.
	 */
	public double lowerFraction = 0.02;

	/**
	 * Upper histogram value's fractional value.  Used to compute spread.
	 */
	public double upperFraction = 0.98;

	/**
	 * Number of elements in the histogram.
	 */
	public int histogramLength = 100;

	/**
	 * If the lower and upper histogram values are different by less than or equal to this ammount it is considered
	 * a textureless region.
	 */
	public int minimumSpread = 10;

	public ConfigThresholdSquareBorder(int radius , int minimumSpread, boolean down ) {
		this.radius = radius;
		this.minimumSpread = minimumSpread;
		this.down = down;
	}

	public ConfigThresholdSquareBorder() {
	}

	@Override
	public void checkValidity() {
		super.checkValidity();
	}
}
