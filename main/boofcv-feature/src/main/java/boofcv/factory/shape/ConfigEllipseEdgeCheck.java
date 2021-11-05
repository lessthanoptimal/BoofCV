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

package boofcv.factory.shape;

import boofcv.struct.Configuration;

/**
 * Parameters for {@link boofcv.alg.shapes.ellipse.EdgeIntensityEllipse}
 *
 * @author Peter Abeles
 */
public class ConfigEllipseEdgeCheck implements Configuration {
	/**
	 * Check:<br>
	 * Threshold for minimum edge intensity. This should be a value which is 0 to (max-min pixel value)
	 * Set to &le; 0 to disable check.
	 */
	public double minimumEdgeIntensity = 20;

	/**
	 * Refinement: how many points along the contour it will sample. Set to &le; 0 to disable refinement
	 */
	public int numSampleContour = 20;
	/**
	 * Check:<br>
	 * Tangential distance away from contour the image is sampled when performing edge intensity check.
	 */
	public double checkRadialDistance = 1.5;

	public ConfigEllipseEdgeCheck setTo( ConfigEllipseEdgeCheck src ) {
		this.minimumEdgeIntensity = src.minimumEdgeIntensity;
		this.numSampleContour = src.numSampleContour;
		this.checkRadialDistance = src.checkRadialDistance;
		return this;
	}

	@Override public void checkValidity() {}
}
