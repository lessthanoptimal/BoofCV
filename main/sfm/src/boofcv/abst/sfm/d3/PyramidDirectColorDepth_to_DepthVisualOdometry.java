/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.sfm.DepthSparse3D_to_PixelTo3D;
import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.distort.PixelTransformCached_F32;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.sfm.d3.direct.PyramidDirectColorDepth;
import boofcv.core.image.ConvertImageFilter;
import boofcv.core.image.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.SequencePoint2Transform2_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.ConvertFloatType;
import georegression.struct.se.Se3_F32;
import georegression.struct.se.Se3_F64;

/**
 * TODO write
 *
 * @author Peter Abeles
 */
public class PyramidDirectColorDepth_to_DepthVisualOdometry<T extends ImageBase<T>, Depth extends ImageGray<Depth>>
	implements DepthVisualOdometry<T,Depth>
{
	ImageType<T> inputType;
	Class<Depth> depthType;
	ImageType<Planar> algType;
	DepthSparse3D<Depth> sparse3D;
	DepthSparse3D_to_PixelTo3D<Depth> wrapSparse3D;

	ConvertImageFilter convertInput;
	Planar inputConverted;

	PyramidDirectColorDepth alg;

	ImageDistort<Planar,Planar> adjustImage;
	Planar undistorted;

	CameraPinhole paramAdjusted = new CameraPinhole();

	Se3_F32 worldToCurrent = new Se3_F32();
	Se3_F64 w2c_64 = new Se3_F64();

	public PyramidDirectColorDepth_to_DepthVisualOdometry(DepthSparse3D<Depth> sparse3D,
														  PyramidDirectColorDepth alg,
														  Class<Depth> depthType ) {
		this.sparse3D = sparse3D;
		this.alg = alg;
		this.inputType = alg.getInputType();
		this.depthType = depthType;
		this.algType = alg.getInputType();

		undistorted = algType.createImage(1,1);
		wrapSparse3D = new DepthSparse3D_to_PixelTo3D<>(sparse3D);
	}

	public PyramidDirectColorDepth_to_DepthVisualOdometry(DepthSparse3D<Depth> sparse3D,
														  ConvertImageFilter<T,?> convertImage,
														  PyramidDirectColorDepth alg,
														  Class<Depth> depthType ) {
		this.sparse3D = sparse3D;
		this.alg = alg;
		this.inputType = convertImage.getInputType();
		this.algType = (ImageType<Planar>) convertImage.getOutputType();
		this.depthType = depthType;
		this.convertInput = convertImage;

		inputConverted = algType.createImage(1,1);
		undistorted = algType.createImage(1,1);
		wrapSparse3D = new DepthSparse3D_to_PixelTo3D<>(sparse3D);
	}

	@Override
	public void reset() {
		alg.reset();
	}

	@Override
	public boolean isFault() {
		return alg.isFatalError();
	}

	@Override
	public Se3_F64 getCameraToWorld() {
		alg.worldToCurrent().invert(worldToCurrent);
		ConvertFloatType.convert(worldToCurrent, w2c_64);
		return w2c_64;
	}

	@Override
	public void setCalibration(CameraPinholeRadial paramVisual, Point2Transform2_F32 visToDepth) {

		// the algorithms camera model assumes no lens distortion and that skew = 0
		CameraPinhole desired = new CameraPinhole(paramVisual);
		desired.skew = 0;

		adjustImage = LensDistortionOps.changeCameraModel(
				AdjustmentType.EXPAND, BorderType.ZERO, paramVisual,desired,paramAdjusted, algType);

		Point2Transform2_F32 desiredToOriginal = LensDistortionOps.transformChangeModel_F32(
				AdjustmentType.EXPAND, paramVisual, desired, false, null);

		// the adjusted undistorted image pixel to the depth image transform
		Point2Transform2_F32 adjustedToDepth = new SequencePoint2Transform2_F32(desiredToOriginal,visToDepth);

		// Create a lookup table to make the math much faster
		PixelTransform2_F32 pixelAdjToDepth = new PixelTransformCached_F32(
				paramAdjusted.width, paramAdjusted.height,adjustedToDepth);

		// adjusted pixels to normalized image coordinates in RGB frame
		sparse3D.configure(LensDistortionOps.narrow(paramAdjusted), pixelAdjToDepth);

		undistorted.reshape(paramAdjusted.width, paramAdjusted.height);
		if( convertInput != null ) {
			inputConverted.reshape(paramAdjusted.width, paramAdjusted.height);
		}

		alg.setCameraParameters(
				(float)paramAdjusted.fx, (float)paramAdjusted.fy,
				(float)paramAdjusted.cx, (float)paramAdjusted.cy,
				paramAdjusted.width, paramAdjusted.height);
	}

	@Override
	public boolean process(T visual, Depth depth) {
		if( convertInput != null ) {
			convertInput.process(visual,inputConverted);
			adjustImage.apply(inputConverted, undistorted);

		} else {
			adjustImage.apply((Planar)visual, undistorted);
		}

		sparse3D.setDepthImage(depth);
		return alg.process(undistorted, wrapSparse3D);
	}

	public double getFractionInBounds() {
		return alg.getFractionInBounds();
	}

	@Override
	public ImageType<T> getVisualType() {
		return inputType;
	}

	@Override
	public Class<Depth> getDepthType() {
		return depthType;
	}

	public Planar getUndistorted() {
		return undistorted;
	}
}
