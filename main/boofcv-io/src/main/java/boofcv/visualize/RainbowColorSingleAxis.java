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
public abstract class RainbowColorSingleAxis extends PeriodicColorizer {

	public static class X extends RainbowColorSingleAxis {
		@Override
		public int color(int index, double x, double y, double z) {
			double a = triangleWave(x);
			int red = (int)(235*a)+20;
			int blue = (int)(235*(1.0-a))+20;

			return (red << 16) | blue;
		}
	}

	public static class Y extends RainbowColorSingleAxis {
		@Override
		public int color(int index, double x, double y, double z) {
			double a = triangleWave(y);
			int red = (int)(235*a)+20;
			int blue = (int)(235*(1.0-a))+20;

			return (red << 16) | blue;
		}
	}

	public static class Z extends RainbowColorSingleAxis {
		@Override
		public int color(int index, double x, double y, double z) {
			double a = triangleWave(z);

			int red = (int)(235*a)+20;
			int blue = (int)(235*(1.0-a))+20;

			return (red << 16) | blue;
		}
	}
}
