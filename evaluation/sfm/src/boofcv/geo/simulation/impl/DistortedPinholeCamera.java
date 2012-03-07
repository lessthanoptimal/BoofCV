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

package boofcv.geo.simulation.impl;

import boofcv.geo.simulation.CameraModel;
import boofcv.struct.distort.PointTransform_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.Random;

/**
 * Simulated pinhole camera with distortion.
 *
 * @author Peter Abeles
 */
public class DistortedPinholeCamera implements CameraModel {

	// used when adding pixel noise
	Random rand;
	
	// camera location
	Se3_F64 cameraToWorld = new Se3_F64();
	
	// camera calibration matrix
	DenseMatrix64F K;

	// adds distortion to pixels in normalized image coordinates
	PointTransform_F64 distortNorm;
	
	// image size
	int widthPixels;
	int heightPixels;

	// is the y-axis pointed up or down
	boolean yAxisDown;

	// noise applied to each pixel
	double sigmaPixel;

	// temporary storage
	Point3D_F64 cameraPt = new Point3D_F64();

	/**
	 * Configures the camera simulation
	 *
	 * @param rand Noise random number generator
	 * @param K Camera calibration matrix
	 * @param distortNorm Adds distortion to pixels in normalized image coordinates  If null, no distortion.
	 * @param widthPixels width of camera image in pixels
	 * @param heightPixels height of camera image in pixels
	 * @param yAxisDown Is the positive y-axis pointed down? Most of the time this is true
	 * @param sigmaPixel Noise added to each pixel
	 */
	public DistortedPinholeCamera(Random rand,  DenseMatrix64F K, PointTransform_F64 distortNorm,
								  int widthPixels, int heightPixels,
								  boolean yAxisDown, double sigmaPixel)
	{
		this.rand = rand;
		this.K = K;
		this.distortNorm = distortNorm;
		this.widthPixels = widthPixels;
		this.heightPixels = heightPixels;
		this.yAxisDown = yAxisDown;
		this.sigmaPixel = sigmaPixel;
	}

	@Override
	public boolean projectPoint(Point3D_F64 world, Point2D_F64 pixel) {

		// from world to camera coordinate system
		SePointOps_F64.transformReverse(cameraToWorld, world, cameraPt);

		// discard points behind the camera
		if( cameraPt.z <= 0 )
			return false;

		// camera 3D to normalized pixel 2D
		pixel.x = cameraPt.x/cameraPt.z;
		pixel.y = cameraPt.y/cameraPt.z;

		// add distortion to normalized pixel coordinates
		if( distortNorm != null )
			distortNorm.compute(pixel.x,pixel.y,pixel);

		// normalized to to pixel
		GeometryMath_F64.mult(K,pixel,pixel);

		// adjust coordinate system
		if(yAxisDown) {
			pixel.y = heightPixels-pixel.y;
		}

		// add noise
		pixel.x += rand.nextGaussian()*sigmaPixel;
		pixel.y += rand.nextGaussian()*sigmaPixel;

		// make sure the pixel is inside the field of view
		return !(pixel.x < 0 || pixel.y < 0 || pixel.x >= widthPixels || pixel.y >= heightPixels);
	}

	@Override
	public void setCameraToWorld(Se3_F64 pose) {
		cameraToWorld.set(pose);
	}

	@Override
	public Se3_F64 getWorldToCamera() {
		return cameraToWorld;
	}

	public double getSigmaPixel() {
		return sigmaPixel;
	}

	public void setSigmaPixel(double sigmaPixel) {
		this.sigmaPixel = sigmaPixel;
	}

	public boolean isyAxisDown() {
		return yAxisDown;
	}

	public void setyAxisDown(boolean yAxisDown) {
		this.yAxisDown = yAxisDown;
	}
}
