/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.visualize;

/**
 * Generates colors based on position in Y and X-Z plane
 *
 * @author Peter Abeles
 */
public class RainbowColor_Y implements PointCloudViewer.Colorizer {

	double periodY;

	public RainbowColor_Y( double periodY ) {
		this.periodY = periodY;
	}

	public RainbowColor_Y() {
	}

	@Override
	public int color(int index, double x, double y, double z) {

		// generate a triangle wave for alternative the colors
		double a = Math.abs(y/periodY - Math.floor(y/periodY + 0.5));

		int red = (int)(235*a)+20;
		int blue = (int)(235*(1.0-a))+20;

		return (red << 16) | blue;
	}

	public double getPeriodY() {
		return periodY;
	}

	public void setPeriodY(double periodY) {
		this.periodY = periodY;
	}
}
