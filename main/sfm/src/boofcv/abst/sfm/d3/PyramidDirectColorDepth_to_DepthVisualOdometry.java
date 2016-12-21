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

import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.sfm.d3.direct.PyramidDirectColorDepth;
import boofcv.core.image.border.BorderType;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.se.Se3_F64;

/**
 * @author Peter Abeles
 */
public class PyramidDirectColorDepth_to_DepthVisualOdometry<T extends ImageGray<T>, Depth extends ImageGray<Depth>>
	implements DepthVisualOdometry<Planar<T>,Depth>
{
	ImageType<Planar<T>> visType;
	Class<Depth> depthType;
	DepthSparse3D<Depth> sparse3D;

	PyramidDirectColorDepth<T> alg;

	ImageDistort<Planar<T>,Planar<T>> adjustImage;
	Planar<T> undistorted;

	CameraPinholeRadial paramAdjusted;

	public PyramidDirectColorDepth_to_DepthVisualOdometry(DepthSparse3D<Depth> sparse3D,
														  PyramidDirectColorDepth<T> alg,
														  Class<Depth> depthType ) {
		this.sparse3D = sparse3D;
		this.alg = alg;
		this.visType = alg.getInputType();
		this.depthType = depthType;

		undistorted = visType.createImage(1,1);
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
	public void setCalibration(CameraPinholeRadial paramVisual, Point2Transform2_F32 visToDepth) {
		if( paramVisual.skew != 0 )
			throw new RuntimeException("Removing skew is not yet supported");

		// todo undistortedToDepth transform

		paramAdjusted = paramVisual.createLike();

		adjustImage = LensDistortionOps.imageRemoveDistortion(
				AdjustmentType.EXPAND, BorderType.ZERO, paramVisual,paramAdjusted,visType);

		undistorted.reshape(paramVisual.width, paramVisual.height);

		alg.setCameraParameters(
				(float)paramAdjusted.fx, (float)paramAdjusted.fy,
				(float)paramAdjusted.cy, (float)paramAdjusted.cy,
				paramAdjusted.width, paramAdjusted.height);
	}

	@Override
	public boolean process(Planar<T> visual, Depth depth) {
		sparse3D.setDepthImage(depth);
		adjustImage.apply(visual, undistorted);
		return alg.process(visual, null);
	}

	@Override
	public ImageType<Planar<T>> getVisualType() {
		return visType;
	}

	@Override
	public Class<Depth> getDepthType() {
		return depthType;
	}
}
