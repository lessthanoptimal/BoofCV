/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.distort.PointTransform_F32;
import georegression.struct.homo.Homography2D_F32;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.homo.UtilHomography;
import georegression.struct.point.Point2D_F32;
import georegression.transform.homo.HomographyPointOps_F32;
import org.ejml.data.DenseMatrix64F;


/**
 * {@link PointTransform_F32} using {@link georegression.struct.homo.Homography2D_F32}.
 *
 * @author Peter Abeles
 */
public class PointTransformHomography_F32 implements PointTransform_F32 {

	Homography2D_F32 homo = new Homography2D_F32();

	public PointTransformHomography_F32() {
	}

	public PointTransformHomography_F32(DenseMatrix64F homo) {
		UtilHomography.convert(homo, this.homo);
	}

	public PointTransformHomography_F32(Homography2D_F32 homo) {
		this.homo = homo;
	}
	public PointTransformHomography_F32(Homography2D_F64 homo) {
		set(homo);
	}

	public void set(Homography2D_F32 transform ) {
		this.homo.set(transform);
	}

	public void set( Homography2D_F64 transform ) {
		UtilHomography.convert(transform,homo);
	}

	@Override
	public void compute(float x, float y, Point2D_F32 out) {
		HomographyPointOps_F32.transform(homo, x, y, out);
	}

	public Homography2D_F32 getModel() {
		return homo;
	}
}
