/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.distort.Point2Transform2Model_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.homography.UtilHomography_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;

/**
 * {@link Point2Transform2Model_F64} using {@link Homography2D_F64}.
 *
 * @author Peter Abeles
 */
public class PointTransformHomography_F64 implements Point2Transform2Model_F64<Homography2D_F64> {

	final Homography2D_F64 model = new Homography2D_F64();

	public PointTransformHomography_F64() {}

	public PointTransformHomography_F64( DMatrixRMaj model ) {
		UtilHomography_F64.convert(model, this.model);
	}

	public PointTransformHomography_F64( Homography2D_F64 model ) {
		set(model);
	}

	public void set( DMatrix transform ) {
		this.model.setTo(transform);
	}

	@Override public void compute( double x, double y, Point2D_F64 out ) {
		HomographyPointOps_F64.transform(model, x, y, out);
	}

	@Override public void setModel( Homography2D_F64 o ) {
		model.setTo(o);
	}

	@Override public Homography2D_F64 getModel() {
		return model;
	}

	@Override public Homography2D_F64 newInstanceModel() {
		return new Homography2D_F64();
	}

	@Override public PointTransformHomography_F64 copyConcurrent() {
		return new PointTransformHomography_F64(model.copy());
	}
}
