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

package boofcv.alg.sfm;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;

/**
 * Computes stereo disparity on a per pixel basis as requested.
 *
 * @author Peter Abeles
 */
public class StereoSparse3D<T extends ImageGray>
		extends StereoProcessingBase<T> implements ImagePixelTo3D {

	// computes spare disparity
	private StereoDisparitySparse<T> disparity;

	// convert from left camera pixel coordinates into rectified coordinates
	private Point2Transform2_F64 leftPixelToRect;

	// storage for rectified pixel coordinate
	private Point2D_F64 pixelRect = new Point2D_F64();

	// 3D coordinate in the left camera: in homogeneous coordinates.  w = disparity
	private Point3D_F64 pointLeft = new Point3D_F64();
	// Found disparity or the 4th-axis in homogeneous coordinates
	private double w;

	/**
	 * Configures and declares internal data
	 *
	 * @param imageType   Input image type
	 */
	public StereoSparse3D(StereoDisparitySparse<T> disparity, Class<T> imageType) {
		super(imageType);
		this.disparity = disparity;
	}

	@Override
	public void setCalibration(StereoParameters stereoParam) {
		super.setCalibration(stereoParam);

		leftPixelToRect = RectifyImageOps.transformPixelToRect_F64(stereoParam.left,rect1);
	}

	@Override
	public void setImages( T leftImage , T rightImage ) {
		super.setImages(leftImage,rightImage);
		disparity.setImages(imageLeftRect,imageRightRect);
	}

	/**
	 * Takes in pixel coordinates from the left camera in the original image coordinate system
	 * @param x x-coordinate of the pixel
	 * @param y y-coordinate of the pixel
	 * @return true if successful
	 */
	@Override
	public boolean process(double x, double y) {

		leftPixelToRect.compute(x,y,pixelRect);

		// round to the nearest pixel
		if( !disparity.process((int)(pixelRect.x+0.5),(int)(pixelRect.y+0.5)) )
			return false;

		// Compute coordinate in camera frame
		this.w = disparity.getDisparity();
		computeHomo3D(pixelRect.x, pixelRect.y, pointLeft);

		return true;
	}

	@Override
	public double getX() {
		return pointLeft.x;
	}

	@Override
	public double getY() {
		return pointLeft.y;
	}

	@Override
	public double getZ() {
		return pointLeft.z;
	}

	@Override
	public double getW() {
		return w;
	}
}
