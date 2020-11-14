/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
 * Generates colors based on value along one axis. A triangle wave is used.
 *
 * @author Peter Abeles
 */
public abstract class SingleAxisRgb extends PeriodicColorizer {

	private static final int HV = 255;
	private static final int LV = 0;

	final int color( double val ) {
		double f = triangleWave(val);
		double a=(1-f)/0.25;	//invert and group
		int X=(int)Math.floor(a);	//this is the integer part
		int Y=(int)Math.floor(HV*(a-X)); //fractional part from 0 to 255

		int r,g,b;
		switch (X) {
			case 0 -> { r = HV; g = Y; b = LV; }
			case 1 -> { r = HV - Y; g = HV; b = LV; }
			case 2 -> { r = LV; g = HV; b = Y; }
			case 3 -> { r = LV; g = HV - Y; b = HV; }
			case 4 -> { r = LV; g = LV; b = HV; }
			default -> throw new RuntimeException("BUG! X=" + X + "  f=" + f);
		}
		return (r << 16) | (g <<8) | b;
	}

	public static class X extends SingleAxisRgb {
		@Override
		public int color(int index, double x, double y, double z) {
			return color(x);
		}
	}

	public static class Y extends SingleAxisRgb {
		@Override
		public int color(int index, double x, double y, double z) {
			return color(y);
		}
	}

	public static class Z extends SingleAxisRgb {
		@Override
		public int color(int index, double x, double y, double z) {
			return color(z);
		}
	}
}
