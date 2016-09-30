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

package boofcv.struct.calib;

/**
 * <p>
 * Calibration parameters for depth sensors (e.g. Kinect) which provide depth information for pixels inside an RGB
 * image via a depth image.  The depth and visual images are assumed to be already aligned.  As such, both images
 * will have the same size.
 * </p>
 *
 * @author Peter Abeles
 */
public class VisualDepthParameters {

	/**
	 * The maximum depth which the depth sensor can sense
	 */
	public Number maxDepth;

	/**
	 * The value to a pixel in the depth image if there is no information there
	 */
	public Number pixelNoDepth;

	/**
	 * Intrinsic camera parameters for the visual sensor.
	 */
	public CameraPinholeRadial visualParam;

	public VisualDepthParameters() {
	}

	public VisualDepthParameters(VisualDepthParameters param) {
		set(param);
	}

	public void set( VisualDepthParameters param ) {
		maxDepth = param.maxDepth;
		visualParam.set( param.visualParam );
	}

	public CameraPinholeRadial getVisualParam() {
		return visualParam;
	}

	public void setVisualParam(CameraPinholeRadial visualParam) {
		this.visualParam = visualParam;
	}

	public Number getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(Number maxDepth) {
		this.maxDepth = maxDepth;
	}

	public Number getPixelNoDepth() {
		return pixelNoDepth;
	}

	public void setPixelNoDepth(Number pixelNoDepth) {
		this.pixelNoDepth = pixelNoDepth;
	}
}
