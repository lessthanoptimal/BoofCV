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

import boofcv.struct.distort.PixelTransform2_F32;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.transform.affine.AffinePointOps_F32;


/**
 * Distorts pixels using {@link Affine2D_F32}.
 *
 * @author Peter Abeles
 */
public class PixelTransformAffine_F32 extends PixelTransform2_F32 {

	Affine2D_F32 affine = new Affine2D_F32();
	Point2D_F32 tran = new Point2D_F32();

	public PixelTransformAffine_F32() {
	}

	public PixelTransformAffine_F32(Affine2D_F32 affine) {
		
		this.affine = affine;
	}

	public void set( Affine2D_F32 affine ) {
		this.affine.set(affine);
	}

	public void set( Affine2D_F64 affine ) {
		this.affine.a11 = (float)affine.a11;
		this.affine.a12 = (float)affine.a12;
		this.affine.a21 = (float)affine.a21;
		this.affine.a22 = (float)affine.a22;

		this.affine.tx = (float)affine.tx;
		this.affine.ty = (float)affine.ty;
	}

	@Override
	public void compute(int x, int y) {
		AffinePointOps_F32.transform(affine, x, y, tran);
		distX = tran.x;
		distY = tran.y;
	}

	public Affine2D_F32 getModel() {
		return affine;
	}
}
