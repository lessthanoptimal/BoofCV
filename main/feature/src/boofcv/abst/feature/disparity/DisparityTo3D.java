/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.disparity;

import boofcv.alg.distort.ImageDistort;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.feature.ComputePixelTo3D;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F32;
import org.ejml.data.DenseMatrix64F;

/**
 * Computes the disparity using a sparse algorithm for each point as requested, which is then
 * converted into a 3D coordinate in the camera's reference frame.
 *
 * @author Peter Abeles
 */
public class DisparityTo3D<T extends ImageSingleBand>
		implements ComputePixelTo3D<T>
{
	// computes spare disparity
	private StereoDisparitySparse<T> alg;

	// converts coordinates from unrectified pixels to rectified pixels
	private PointTransform_F32 pixelToRectified;
	private Point2D_F32 rectified = new Point2D_F32();
	// distortion for rectifying left and right images
	private ImageDistort<T> rectifyLeft;
	private ImageDistort<T> rectifyRight;

	// rectified images
	private T rectLeft;
	private T rectRight;

	// stereo camera system configuration
	private double baseline;
	private double focalLengthX;
	private double focalLengthY;
	private double centerX;
	private double centerY;

	// found 3D coordinate of the point
	private double x,y,z;

	public DisparityTo3D(StereoDisparitySparse<T> alg,
						 ImageDistort<T> rectifyLeft,
						 ImageDistort<T> rectifyRight,
						 PointTransform_F32 pixelToRectified,
						 Class<T> imageType) {
		this.alg = alg;
		this.pixelToRectified = pixelToRectified;
		this.rectifyLeft = rectifyLeft;
		this.rectifyRight = rectifyRight;

		rectLeft = GeneralizedImageOps.createSingleBand(imageType,1,1);
		rectRight = GeneralizedImageOps.createSingleBand(imageType,1,1);
	}

	/**
	 * Specifies stereo camera parameters
	 *
	 * @param baseline Baseline between the two stereo cameras
	 * @param K Camera calibration matrix for rectified images
	 */
	public void configure( double baseline, DenseMatrix64F K ) {
		this.baseline = baseline;
		this.focalLengthX = K.get(0,0);
		this.focalLengthY = K.get(1,1);
		this.centerX = K.get(0,2);
		this.centerY = K.get(1,2);
	}

	/**
	 * Specifies rectified input images.
	 *
	 * @param leftImage Unrectified left input image
	 * @param rightImage Unrectified right input image
	 */
	@Override
	public void setImages(T leftImage, T rightImage) {
		if( leftImage.width != rectLeft.width || leftImage.height != rectLeft.height ) {
			rectLeft.reshape(leftImage.width,leftImage.height);
			rectRight.reshape(leftImage.width,leftImage.height);
		}

		rectifyLeft.apply(leftImage,rectLeft);
		rectifyRight.apply(rightImage,rectRight);

		alg.setImages(rectLeft,rectRight);
	}

	@Override
	public boolean process(double x, double y) {
		pixelToRectified.compute((float)x,(float)y,rectified);

		if( !alg.process((int)rectified.x,(int)rectified.y))
			return false;

		double d = alg.getDisparity();

		this.z = baseline*focalLengthX/d;
		this.x = z*(x - centerX)/focalLengthX;
		this.y = z*(y - centerY)/focalLengthY;

		return true;
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public double getZ() {
		return z;
	}
}
