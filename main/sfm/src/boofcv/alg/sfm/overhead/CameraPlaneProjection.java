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

package boofcv.alg.sfm.overhead;

import boofcv.alg.distort.LensDistortionOps;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

/**
 * Computes the location of a point on the plane from an observation in pixels and the reverse.
 *
 * See {@link CreateSyntheticOverheadView} for details about reference frames and how the plane is defined.
 *
 * @author Peter Abeles
 */
public class CameraPlaneProjection {

	// transform from plane to camera
	private Se3_F64 planeToCamera;
	// transform from camera to plane
	private Se3_F64 cameraToPlane = new Se3_F64();
	// converts from pixel to normalized image coordinates
	private Point2Transform2_F64 pixelToNorm;
	// convert from normalized image coordinates to pixel
	private Point2Transform2_F64 normToPixel;

	// location of point on plane in current ref frame in 3D
	private Point3D_F64 plain3D = new Point3D_F64();
	// location of point in 3D camera ref frame
	private Point3D_F64 camera3D = new Point3D_F64();
	// normalized image coordinate of pixel
	private Point2D_F64 norm = new Point2D_F64();

	// Pointing vector from pixel
	private Vector3D_F64 pointing = new Vector3D_F64();

	/**
	 * Configures the camera's intrinsic and extrinsic parameters
	 * @param planeToCamera Transform from the plane to the camera
	 * @param intrinsic Pixel to normalized image coordinates
	 */
	public void setConfiguration( Se3_F64 planeToCamera ,
								  CameraPinholeRadial intrinsic )
	{
		this.planeToCamera = planeToCamera;
		normToPixel = LensDistortionOps.transformPoint(intrinsic).distort_F64(false, true);
		pixelToNorm = LensDistortionOps.transformPoint(intrinsic).undistort_F64(true, false);

		planeToCamera.invert(cameraToPlane);
	}

	/**
	 * Configures the camera's intrinsic parameters
	 * @param intrinsic Intrinsic camera parameters
	 */
	public void setIntrinsic(CameraPinholeRadial intrinsic )
	{
		normToPixel = LensDistortionOps.transformPoint(intrinsic).distort_F64(false, true);
		pixelToNorm = LensDistortionOps.transformPoint(intrinsic).undistort_F64(true, false);
	}

	/**
	 * Specifies camera's extrinsic parameters.
	 *
	 * @param planeToCamera Transform from plane to camera reference frame
	 * @param computeInverse Set to true if pixelToPlane is going to be called.  performs extra calculation
	 */
	public void setPlaneToCamera(Se3_F64 planeToCamera, boolean computeInverse ) {
		this.planeToCamera = planeToCamera;

		if( computeInverse )
			planeToCamera.invert(cameraToPlane);
	}

	/**
	 * Given a point on the plane find the pixel in the image.
	 *
	 * @param pointX (input) Point on the plane, x-axis
	 * @param pointY (input) Point on the plane, y-axis
	 * @param pixel (output) Pixel in the image
	 * @return true if the point is in front of the camera.  False if not.
	 */
	public boolean planeToPixel( double pointX , double pointY , Point2D_F64 pixel ) {
		// convert it into a 3D coordinate and transform into camera reference frame
		plain3D.set(-pointY, 0, pointX);
		SePointOps_F64.transform(planeToCamera, plain3D, camera3D);

		// if it's behind the camera it can't be seen
		if( camera3D.z <= 0 )
			return false;

		// normalized image coordinates and convert into pixels
		double normX = camera3D.x / camera3D.z;
		double normY = camera3D.y / camera3D.z;
		normToPixel.compute(normX,normY,pixel);

		return true;
	}

	/**
	 * Given a point on the plane find the normalized image coordinate
	 *
	 * @param pointX (input) Point on the plane, x-axis
	 * @param pointY (input) Point on the plane, y-axis
	 * @param normalized (output) Normalized image coordinate of pixel
	 * @return true if the point is in front of the camera.  False if not.
	 */
	public boolean planeToNormalized( double pointX , double pointY , Point2D_F64 normalized ) {
		// convert it into a 3D coordinate and transform into camera reference frame
		plain3D.set(-pointY, 0, pointX);
		SePointOps_F64.transform(planeToCamera, plain3D, camera3D);

		// if it's behind the camera it can't be seen
		if( camera3D.z <= 0 )
			return false;

		// normalized image coordinates and convert into pixels
		normalized.x = camera3D.x / camera3D.z;
		normalized.y = camera3D.y / camera3D.z;

		return true;
	}

	/**
	 * Given a pixel, find the point on the plane.  Be sure computeInverse was set to true in
	 * {@link #setPlaneToCamera(georegression.struct.se.Se3_F64, boolean)}
	 *
	 * @param pixelX (input) Pixel in the image, x-axis
	 * @param pixelY (input) Pixel in the image, y-axis
	 * @param plane (output) Point on the plane.
	 * @return true if a point on the plane was found in front of the camera
	 */
	public boolean pixelToPlane( double pixelX , double pixelY , Point2D_F64 plane ) {
		// computer normalized image coordinates
		pixelToNorm.compute(pixelX,pixelY,norm);

		// Ray pointing from camera center through pixel to ground in ground reference frame
		pointing.set(norm.x,norm.y,1);
		GeometryMath_F64.mult(cameraToPlane.getR(), pointing, pointing);

		double height = cameraToPlane.getY();

		// If the point vector and the vector from the plane have the same sign then the ray will not intersect
		// the plane or the intersection is undefined
		if( pointing.y*height >= 0 )
			return false;

		// compute the location of the point on the plane in 2D
		double t = -height / pointing.y;

		plane.x = pointing.z*t;
		plane.y = -pointing.x*t;

		return true;
	}

	/**
	 * Given a pixel in normalized coordinates, find the point on the plane.  Make sure invert was set to true in
	 * {@link #setPlaneToCamera(georegression.struct.se.Se3_F64, boolean)}
	 *
	 * @param normX (input) Image pixel in normalized coordinates, x-axis
	 * @param normY (input) Image pixel in normalized coordinates, y-axis
	 * @param plane (output) Point on the plane.
	 * @return true if a point on the plane was found in front of the camera
	 */
	public boolean normalToPlane( double normX , double normY , Point2D_F64 plane ) {
		// Ray pointing from camera center through pixel to ground in ground reference frame
		pointing.set(normX,normY,1);
		GeometryMath_F64.mult(cameraToPlane.getR(), pointing, pointing);

		double height = cameraToPlane.getY();

		// If the point vector and the vector from the plane have the same sign then the ray will not intersect
		// the plane or the intersection is undefined
		if( pointing.y*height >= 0 )
			return false;

		// compute the location of the point on the plane in 2D
		double t = -height / pointing.y;

		plane.x = pointing.z*t;
		plane.y = -pointing.x*t;

		return true;
	}
}
