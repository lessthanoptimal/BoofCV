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

package boofcv.alg.distort;

import boofcv.struct.distort.PixelTransform;
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.transform.homography.HomographyPointOps_F32;

/**
 * Distorts pixels using {@link Homography2D_F32}.
 *
 * @author Peter Abeles
 */
public class PixelTransformHomography_F32 implements PixelTransform<Point2D_F32> {

	final Homography2D_F32 homo = new Homography2D_F32();

	public PixelTransformHomography_F32() {}

	public PixelTransformHomography_F32( Homography2D_F32 homo ) {
		this.homo.setTo(homo);
	}

	public PixelTransformHomography_F32( Homography2D_F64 homo ) {
		setTo(homo);
	}

	public void setTo( Homography2D_F32 transform ) {
		this.homo.setTo(transform);
	}

	public void setTo( Homography2D_F64 transform ) {
		this.homo.a11 = (float)transform.a11;
		this.homo.a12 = (float)transform.a12;
		this.homo.a13 = (float)transform.a13;
		this.homo.a21 = (float)transform.a21;
		this.homo.a22 = (float)transform.a22;
		this.homo.a23 = (float)transform.a23;
		this.homo.a31 = (float)transform.a31;
		this.homo.a32 = (float)transform.a32;
		this.homo.a33 = (float)transform.a33;
	}

	public Homography2D_F32 getModel() {
		return homo;
	}

	@Override
	public void compute( int x, int y, Point2D_F32 output ) {
		HomographyPointOps_F32.transform(homo, x, y, output);
	}

	@Override
	public PixelTransformHomography_F32 copyConcurrent() {
		return new PixelTransformHomography_F32(homo.copy());
	}
}
