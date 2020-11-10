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

import boofcv.struct.distort.PixelTransform;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.affine.AffinePointOps_F64;
import lombok.Getter;

/**
 * Distorts pixels using {@link Affine2D_F64}.
 *
 * @author Peter Abeles
 */
public class PixelTransformAffine_F64 implements PixelTransform<Point2D_F64> {

	@Getter protected final Affine2D_F64 model = new Affine2D_F64();

	public PixelTransformAffine_F64() {}

	public PixelTransformAffine_F64( Affine2D_F64 affine ) {
		this.model.setTo(affine);
	}

	public void setTo( Affine2D_F64 affine ) {
		this.model.setTo(affine);
	}

	@Override
	public void compute( int x, int y, Point2D_F64 output ) {
		AffinePointOps_F64.transform(model, x, y, output);
	}

	@Override
	public PixelTransformAffine_F64 copyConcurrent() {
		return new PixelTransformAffine_F64(model);
	}
}
