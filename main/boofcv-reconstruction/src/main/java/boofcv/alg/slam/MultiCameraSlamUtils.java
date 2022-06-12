/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.slam;

import boofcv.abst.geo.Triangulate2PointingMetricH;
import boofcv.alg.distort.LensDistortionWideFOV;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.ConfigLength;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.io.PrintStream;
import java.util.Objects;

/**
 * Various geometric functions and sanity checks for {@link BatchSlamMultiCameras}. Each camera can have it's own
 * intrinsics and image size
 *
 *
 *
 * @author Peter Abeles
 */
public class MultiCameraSlamUtils {
	/** Reject triangulation of reprojection error exceeds this value. relative to (w+h)/2 */
	public final ConfigLength tolReprojection = ConfigLength.relative(5.0/800.0, 1.0);

	/** Reject triangulation of angle between observations and 3D point is larger than this amount. */
	public final double tolPointing = 0.5*Math.PI;

	/** Triangulation algorithm used */
	public Triangulate2PointingMetricH triangulator2 = FactoryMultiView.triangulate2PointingMetricH(null);

	// Shape of images
	final ImageDimension shapeA = new ImageDimension();
	final ImageDimension shapeB = new ImageDimension();

	// Transform to and from image pixels
	Point2Transform3_F64 pixelToPointingA;
	Point3Transform2_F64 pointingToPixelA;
	Point2Transform3_F64 pixelToPointingB;
	Point3Transform2_F64 pointingToPixelB;

	// pixel tolerance squared for the views
	private double tolReprojectionA, tolReprojectionB;

	// Observations converting into pointing vectors
	Point3D_F64 pointA = new Point3D_F64();
	Point3D_F64 pointB = new Point3D_F64();

	// location of homogenous landmark in view-b
	Point4D_F64 locationB = new Point4D_F64();

	Point3D_F64 temp3 = new Point3D_F64();
	Point2D_F64 reprojected = new Point2D_F64();

	// verbose output if not null.
	public PrintStream verbose;

	/**
	 * Configure camera for view A
	 *
	 * @param intrinsics Lens model
	 * @param imageShape size of image
	 */
	public void setCameraA( final LensDistortionWideFOV intrinsics, ImageDimension imageShape ) {
		this.shapeA.setTo(imageShape);

		tolReprojectionA = tolReprojection.compute((imageShape.width + imageShape.height)/2.0);
		tolReprojectionA *= tolReprojectionA;

		pixelToPointingA = intrinsics.undistortPtoS_F64();
		pointingToPixelA = intrinsics.distortStoP_F64();
	}

	/**
	 * Configure camera for view B
	 *
	 * @param intrinsics Lens model
	 * @param imageShape size of image
	 */
	public void setCameraB( final LensDistortionWideFOV intrinsics, ImageDimension imageShape ) {
		this.shapeB.setTo(imageShape);

		tolReprojectionB = tolReprojection.compute((imageShape.width + imageShape.height)/2.0);
		tolReprojectionB *= tolReprojectionB;

		pixelToPointingB = intrinsics.undistortPtoS_F64();
		pointingToPixelB = intrinsics.distortStoP_F64();
	}

	/**
	 * Triangulates a point given the observations.
	 *
	 * @param pixelA (Input) Pixel observation of landmark in view A
	 * @param pixelB (Input) Pixel observation of landmark in view B
	 * @param viewA_to_viewB (Input) Known extronsic relationship. Transform from view-a to view-b
	 * @param locationA (Output) Found location of landmark in 3D space as homogenous coordinate
	 * @return true is triangulation did not fail
	 */
	public boolean triangulate( Point2D_F64 pixelA, Point2D_F64 pixelB, Se3_F64 viewA_to_viewB, Point4D_F64 locationA ) {
		Objects.requireNonNull(pixelToPointingA, "Must configure view-a first");
		Objects.requireNonNull(pixelToPointingB, "Must configure view-b first");

		// Convert to pointing vectors
		pixelToPointingA.compute(pixelA.x, pixelA.y, pointA);
		pixelToPointingB.compute(pixelB.x, pixelB.y, pointB);

		// Estimate 3D location in homogeneous coordinates
		if (!triangulator2.triangulate(pointA, pointB, viewA_to_viewB, locationA)) {
			if (verbose != null) verbose.println("failed triangulate");
			return false;
		}

		return true;
	}

	/**
	 * <p>Checks to see if reprojection error is within tolerance for the triangulated point in view-a</p>
	 *
	 * NOTE: Assumes {@link #triangulate} was called first.
	 *
	 * @return true if successful
	 */
	public boolean checkReprojection( Point2D_F64 pixelA, Point2D_F64 pixelB, Se3_F64 viewA_to_viewB, Point4D_F64 locationA ) {
		// Reproject point in view A.
		// NOTE: w just changes the scale and has no influence on apparent location, even if at infinity w=0
		temp3.setTo(locationA.x, locationA.y, locationA.z);
		temp3.divideIP(temp3.norm());
		pointingToPixelA.compute(temp3.x, temp3.y, temp3.z, reprojected);

		// See if the error is too large
		if (reprojected.distance2(pixelA) > tolReprojectionA) {
			if (verbose != null) verbose.println("failed reprojection in view A");
			return false;
		}

		// Transfer the location to view B reference frame
		SePointOps_F64.transform(viewA_to_viewB, locationA, locationB);

		temp3.setTo(locationB.x, locationB.y, locationB.z);
		temp3.divideIP(temp3.norm());
		pointingToPixelB.compute(temp3.x, temp3.y, temp3.z, reprojected);

		// See if error is too large
		if (reprojected.distance2(pixelB) > tolReprojectionA) {
			if (verbose != null) verbose.println("failed reprojection in view B");
			return false;
		}

		return true;
	}

	/**
	 * <p>If the projected 3D point has a very large difference in angle from the observation then reject. Classic
	 * example is if the triangulation point is behind the camera.</p>
	 *
	 * NOTE: Assumes {@link #triangulate} was called first.
	 *
	 * @return true if successful
	 */
	public boolean checkObservationAngle( Se3_F64 viewA_to_viewB, Point4D_F64 locationA ) {
		if (acuteAngle(pointA, locationA) > tolPointing) {
			if (verbose != null) verbose.println("failed angle test in view A");
			return false;
		}

		// Transfer landmark location to view B
		SePointOps_F64.transform(viewA_to_viewB, locationA, locationB);

		if (acuteAngle(pointB, locationB) > tolPointing) {
			if (verbose != null) verbose.println("failed angle test in view B");
			return false;
		}
		return true;
	}

	/**
	 * Computes the acute angle between the two points. Ignores 'w' in b since that's for scale only and we care
	 * about direction. Checks for invalid inputs, such as all zeros is done.
	 *
	 * @param a Observation as a pointing direction
	 * @param b triangulated homogenous point
	 * @return acute angle. 0 to pi
	 */
	public double acuteAngle( Point3D_F64 a, Point4D_F64 b ) {
		double na = a.norm();
		double nb = Math.sqrt(b.x*b.x + b.y*b.y + b.z*b.z);

		if (na == 0.0 || nb == 0.0) {
			if (verbose != null) verbose.println("observation of point full of zeros");
			// Return a value which will cause this point to be rejected
			return Math.PI*2.0;
		}

		// compute project from unit vectors to avoid numerical issues
		double dot = (a.x/na)*(b.x/nb) + (a.y/na)*(b.y/nb) + (a.z/na)*(b.z/nb);

		return Math.acos(dot);
	}
}