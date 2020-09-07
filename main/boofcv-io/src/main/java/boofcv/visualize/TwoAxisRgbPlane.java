/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
 * Generates colors using a primary axis and the sum of the other two axises. This is intended to provide
 * more visual variety than {@link SingleAxisMagentaBlue}
 *
 * @author Peter Abeles
 */
public abstract class TwoAxisRgbPlane extends PeriodicColorizer {

	double planeScale;

	protected TwoAxisRgbPlane(double planeScale) {
		this.planeScale = planeScale;
	}

	public static class X_YZ extends TwoAxisRgbPlane {
		public X_YZ(double planeScale) {
			super(planeScale);
		}

		@Override
		public int color(int index, double x, double y, double z) {
			double a = triangleWave(x);
			double b = triangleWave(y+z,period*planeScale);
			return color(a,b);
		}
	}

	public static class Y_XZ extends TwoAxisRgbPlane {
		public Y_XZ(double planeScale) {
			super(planeScale);
		}

		@Override
		public int color(int index, double x, double y, double z) {
			double a = triangleWave(y);
			double b = triangleWave(x+z,period*planeScale);
			return color(a,b);
		}
	}

	public static class Z_XY extends TwoAxisRgbPlane {
		public Z_XY(double planeScale) {
			super(planeScale);
		}

		@Override
		public int color(int index, double x, double y, double z) {
			double a = triangleWave(z);
			double b = triangleWave(x+y,period*planeScale);
			return color(a,b);
		}
	}

	protected int color( double axis , double plane ) {
		int red = (int)(255*axis);
		int other = (int)(255*plane);

		return (red << 16) | (other << 8) | (0xFF-other);
	}
}
