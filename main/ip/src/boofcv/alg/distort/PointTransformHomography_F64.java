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

import boofcv.struct.distort.PointTransform_F64;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.homo.UtilHomography;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homo.HomographyPointOps_F64;
import org.ejml.data.DenseMatrix64F;


/**
 * {@link boofcv.struct.distort.PointTransform_F64} using {@link georegression.struct.homo.Homography2D_F64}.
 *
 * @author Peter Abeles
 */
public class PointTransformHomography_F64 implements PointTransform_F64 {

	Homography2D_F64 homo = new Homography2D_F64();

	public PointTransformHomography_F64() {
	}

	public PointTransformHomography_F64(DenseMatrix64F homo) {
		UtilHomography.convert(homo, this.homo);
	}

	public PointTransformHomography_F64(Homography2D_F64 homo) {
		set(homo);
	}

	public void set(Homography2D_F64 transform ) {
		this.homo.set(transform);
	}

	@Override
	public void compute(double x, double y, Point2D_F64 out) {
		HomographyPointOps_F64.transform(homo, x, y, out);
	}

	public Homography2D_F64 getModel() {
		return homo;
	}
}
