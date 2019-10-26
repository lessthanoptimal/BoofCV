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
 * A point cloud colorizer where the color pattern repeats periodically. The pattern can be offset and scaled
 *
 * @author Peter Abeles
 */
public abstract class PeriodicColorizer implements PointCloudViewer.Colorizer {
	// the period. This will be in the same units as the point
	double period;
	// offset scale. Scale relative to the period.
	double offset;

	public PeriodicColorizer(double period, double offset) {
		this.period = period;
		this.offset = offset;
	}

	public PeriodicColorizer() {
	}

	final double triangleWave(double value ) {
		return Math.abs(offset+value/period - Math.floor(offset+value/period + 0.5));
	}
	final double triangleWave(double value , double period) {
		return Math.abs(offset+value/period - Math.floor(offset+value/period + 0.5));
	}

	public double getPeriod() {
		return period;
	}

	public void setPeriod(double period) {
		this.period = period;
	}

	public double getOffset() {
		return offset;
	}

	public void setOffset(double offset) {
		this.offset = offset;
	}
}
