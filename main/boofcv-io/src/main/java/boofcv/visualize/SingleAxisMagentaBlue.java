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
 * Generates colors based on value along one axis. A triangle wave is used.
 *
 * @author Peter Abeles
 */
public abstract class SingleAxisMagentaBlue extends PeriodicColorizer {
	private static final int HV = 225;
	private static final int LV = 30;

	final int color( double val ) {
		double a = triangleWave(val);
		int red = (int)(HV*a)+LV;
		int blue = (int)(HV*(1.0-a))+LV;

		return (red << 16) | blue;
	}

	public static class X extends SingleAxisMagentaBlue {
		@Override
		public int color(int index, double x, double y, double z) {
			return color(x);
		}
	}

	public static class Y extends SingleAxisMagentaBlue {
		@Override
		public int color(int index, double x, double y, double z) {
			return color(y);
		}
	}

	public static class Z extends SingleAxisMagentaBlue {
		@Override
		public int color(int index, double x, double y, double z) {
			return color(z);
		}
	}
}
