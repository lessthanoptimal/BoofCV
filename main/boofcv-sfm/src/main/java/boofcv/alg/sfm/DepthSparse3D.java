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

package boofcv.alg.sfm;

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayI;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;

/**
 * Computes the 3D coordinate a point in a visual camera given a depth image. The visual camera is a standard camera
 * while the depth camera contains the depth (value along z-axis) of objects inside its field of view. The
 * Kinect (structured light) and flash ladar (time of flight) are examples of sensors which could use this class.
 * The z-axis is defined to be pointing straight out of the visual camera and both depth and visual cameras are
 * assumed to be parallel with identical pointing vectors for the z-axis.
 *
 * A mapping is provided for converting between pixels in the visual camera and the depth camera. This mapping
 * is assumed to be fixed with time.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class DepthSparse3D<T extends ImageGray<T>> {
	// Storage for the depth image
	protected T depthImage;

	// transform from visual camera pixels to normalized image coordinates
	private Point2Transform2_F64 p2n;

	// location of point in visual camera coordinate system
	private final Point3D_F64 worldPt = new Point3D_F64();

	// pixel in normalized image coordinates
	private final Point2D_F64 norm = new Point2D_F64();

	// transform from visual image coordinate system to depth image coordinate system
	private PixelTransform<Point2D_F32> visualToDepth;
	private final Point2D_F32 distorted = new Point2D_F32();

	// scales the values from the depth image
	private final double depthScale;

	/**
	 * Configures parameters
	 *
	 * @param depthScale Used to change units found in the depth image.
	 */
	protected DepthSparse3D( double depthScale ) {
		this.depthScale = depthScale;
	}

	/**
	 * Configures intrinsic camera parameters
	 *
	 * @param model Model for narrow FOV cameras
	 * @param visualToDepth Transform from visual to depth camera pixel coordinate systems.
	 */
	public void configure( LensDistortionNarrowFOV model, PixelTransform<Point2D_F32> visualToDepth ) {
		this.visualToDepth = visualToDepth;
		this.p2n = model.undistort_F64(true, false);
	}

	/**
	 * Sets the depth image. A reference is saved internally.
	 *
	 * @param depthImage Image containing depth information.
	 */
	public void setDepthImage( T depthImage ) {
		this.depthImage = depthImage;
	}

	/**
	 * Given a pixel coordinate in the visual camera, compute the 3D coordinate of that point.
	 *
	 * @param x x-coordinate of point in visual camera
	 * @param y y-coordinate of point in visual camera
	 * @return true if a 3D point could be computed and false if not
	 */
	public boolean process( int x, int y ) {
		visualToDepth.compute(x, y, distorted);

		int depthX = (int)distorted.x;
		int depthY = (int)distorted.y;

		if (depthImage.isInBounds(depthX, depthY)) {
			// get the depth at the specified location
			double value = lookupDepth(depthX, depthY);

			// see if its an invalid value
			if (value == 0)
				return false;

			// convert visual pixel into normalized image coordinate
			p2n.compute(x, y, norm);

			// project into 3D space
			worldPt.z = value*depthScale;
			worldPt.x = worldPt.z*norm.x;
			worldPt.y = worldPt.z*norm.y;

			return true;
		} else {
			return false;
		}
	}

	/**
	 * The found 3D coordinate of the point in the visual camera coordinate system. Is only valid when
	 * {@link #process(int, int)} returns true.
	 *
	 * @return 3D coordinate of point in visual camera coordinate system
	 */
	public Point3D_F64 getWorldPt() {
		return worldPt;
	}

	/**
	 * Internal function which looks up the pixel's depth. Depth is defined as the value of the z-coordinate which
	 * is pointing out of the camera. If there is no depth measurement at this location return 0.
	 *
	 * @param depthX x-coordinate of pixel in depth camera
	 * @param depthY y-coordinate of pixel in depth camera
	 * @return depth at the specified coordinate
	 */
	protected abstract double lookupDepth( int depthX, int depthY );

	/**
	 * Implementation for {@link GrayI}.
	 */
	public static class I<T extends GrayI<T>> extends DepthSparse3D<T> {

		public I( double depthScale ) {
			super(depthScale);
		}

		@Override
		protected double lookupDepth( int depthX, int depthY ) {
			return depthImage.unsafe_get(depthX, depthY);
		}
	}

	/**
	 * Implementation for {@link GrayF32}.
	 */
	public static class F32 extends DepthSparse3D<GrayF32> {

		public F32( double depthScale ) {
			super(depthScale);
		}

		@Override
		protected double lookupDepth( int depthX, int depthY ) {
			return depthImage.unsafe_get(depthX, depthY);
		}
	}
}
