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

package boofcv.abst.sfm.d3;

import boofcv.alg.sfm.d3.direct.PyramidDirectRgbDepth;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.se.Se3_F64;

/**
 * @author Peter Abeles
 */
public class PyramidDirectRgbDepth_to_DepthVisualOdometry<T extends ImageGray<T>, Depth extends ImageGray<Depth>>
	implements DepthVisualOdometry<Planar<T>,Depth>
{
	PyramidDirectRgbDepth<T> alg;

	public PyramidDirectRgbDepth_to_DepthVisualOdometry(PyramidDirectRgbDepth<T> alg) {
		this.alg = alg;
	}

	@Override
	public void reset() {

	}

	@Override
	public boolean isFault() {
		return false;
	}

	@Override
	public Se3_F64 getCameraToWorld() {
		return null;
	}

	@Override
	public void setCalibration(CameraPinholeRadial paramVisual, PixelTransform2_F32 visToDepth) {
		// todo if distorted or skewed undistort
	}

	@Override
	public boolean process(Planar<T> visual, Depth depth) {
		return false;
	}

	@Override
	public ImageType<Planar<T>> getVisualType() {
		return null;
	}

	@Override
	public Class<Depth> getDepthType() {
		return null;
	}
}
