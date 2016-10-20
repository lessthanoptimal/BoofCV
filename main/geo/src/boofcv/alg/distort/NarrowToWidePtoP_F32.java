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

package boofcv.alg.distort;

import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point3Transform2_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Projects a synthetic view of a narrow FOV camera from a wide FOV camera.  The synthetic camera
 * can be rotated.
 *
 * @author Peter Abeles
 */
public class NarrowToWidePtoP_F32 implements Point2Transform2_F32 {

	// rotation matrix
	DenseMatrix64F rotateWideToNarrow = CommonOps.identity(3,3);
	Point2Transform2_F32 narrowToNorm;
	Point3Transform2_F32 unitToWide;

	// normalized pixel coordinate storage
	Point2D_F32 norm = new Point2D_F32();
	// unit circle coordinate storage
	Point3D_F32 unit = new Point3D_F32();

	public NarrowToWidePtoP_F32() {
	}

	public NarrowToWidePtoP_F32(LensDistortionNarrowFOV narrow, LensDistortionWideFOV wide) {
		configure(narrow, wide);
	}

	public void configure(LensDistortionNarrowFOV narrow, LensDistortionWideFOV wide) {
		narrowToNorm = narrow.undistort_F32(true,false);
		unitToWide = wide.distortStoP_F32();
	}

	/**
	 * Specifies rotation matrix which determines the pointing direction of the camera
	 * @param R rotation matrix
	 */
	public void setRotationWideToNarrow(DenseMatrix64F R ) {
		this.rotateWideToNarrow.set(R);
	}

	/**
	 * Apply the transformation
	 *
	 * @param x x-coordinate of point in pixels.  Synthetic narrow FOV camera
	 * @param y y-coordinate of point in pixels.  Synthetic narrow FOV camera
	 * @param out Pixel location of point in wide FOV camera.
	 */
	@Override
	public void compute(float x, float y, Point2D_F32 out) {
		narrowToNorm.compute(x,y, norm);

		// Convert from 2D homogenous to 3D
		unit.set( norm.x , norm.y , 1.0f);

		// Rotate then make it a unit vector
		GeometryMath_F32.mult(rotateWideToNarrow,unit,unit);
		float n = unit.norm();
		unit.x /= n;
		unit.y /= n;
		unit.z /= n;

		unitToWide.compute(unit.x,unit.y,unit.z,out);
	}

}
