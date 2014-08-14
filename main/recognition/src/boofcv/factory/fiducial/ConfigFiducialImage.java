/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.fiducial;

import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.abst.fiducial.SquareBinary_to_FiducialDetector}.
 *
 * @see boofcv.alg.fiducial.DetectFiducialSquareBinary
 *
 * @author Peter Abeles
 */
public class ConfigFiducialImage implements Configuration {
	/**
	 * Length of a targets size in world units.
	 */
	public double targetWidth;

	/**
	 * Minimum number of pixels in a shape's contour specified as a fraction of the input
	 * image's width.  Used to prune shapes which are too small.
	 */
	public double minContourFraction = 0.23;
	/**
	 * Tolerance in pixels before a line is split when fitting a contour
	 */
	public int borderTolerance = 4;
	/**
	 * The maximum number of iterations the polygon fitting algorithm can run for
	 */
	public int borderMaxIterations = 20;

	/**
	 * If the difference between an candidate and a target is less than this amount it is considered
	 * a match.
	 */
	public double maxErrorFraction = 0.1;

	public ConfigFiducialImage() {
	}

	public ConfigFiducialImage(double targetWidth) {
		this.targetWidth = targetWidth;
	}

	@Override
	public void checkValidity() {

	}

	public double getTargetWidth() {
		return targetWidth;
	}

	public void setTargetWidth(double targetWidth) {
		this.targetWidth = targetWidth;
	}

	public int getBorderTolerance() {
		return borderTolerance;
	}

	public void setBorderTolerance(int borderTolerance) {
		this.borderTolerance = borderTolerance;
	}

	public int getBorderMaxIterations() {
		return borderMaxIterations;
	}

	public void setBorderMaxIterations(int borderMaxIterations) {
		this.borderMaxIterations = borderMaxIterations;
	}

	public double getMinContourFraction() {
		return minContourFraction;
	}

	public void setMinContourFraction(double minContourFraction) {
		this.minContourFraction = minContourFraction;
	}
}
