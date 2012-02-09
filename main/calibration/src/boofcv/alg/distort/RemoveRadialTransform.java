/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.distort.PixelTransform_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.struct.point.Point2D_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Applies correction for radial distortion
 *
 * @author Peter Abeles
 */
public class RemoveRadialTransform extends PixelTransform_F32 {

	DenseMatrix64F K = new DenseMatrix64F(3,3);
	DenseMatrix64F K_inv = new DenseMatrix64F(3,3);
	// radial distortion
	float kappa[];

	Point2D_F32 temp0 = new Point2D_F32();
	Point2D_F32 temp1 = new Point2D_F32();
	/**
	 *
	 * @param x_c Camera center in pixels
	 * @param y_c Camera center in pixels
	 * @param kappa Radial distortion parameters
	 */
	public RemoveRadialTransform( double a , double b , double c , double x_c, double y_c, double[] kappa) {
		K.set(0,0,a);
		K.set(0,1,c);
		K.set(1,1,b);
		K.set(0,2,x_c);
		K.set(1,2,y_c);
		K.set(2,2,1);

		CommonOps.invert(K, K_inv);
		CommonOps.scale(1.0/K_inv.get(2,2),K_inv);

		this.kappa = new float[kappa.length];
		for( int i = 0; i < kappa.length; i++ ) {
			this.kappa[i] = (float)kappa[i];
		}
	}

	@Override
	public void compute(int x, int y) {
		float sum = 0;

		temp0.x = x;
		temp0.y = y;

		// distorted pixel to distorted normalized
		GeometryMath_F32.mult(K_inv, temp0, temp1);

		float r2 = temp1.x*temp1.x + temp1.y*temp1.y;

		for( int i = 0; i < kappa.length; i++ ) {
			sum += kappa[i]*r2;
		}

		// distorted normalized
		temp0.x = temp1.x + sum*temp1.x;
		temp0.y = temp1.x + sum*temp1.x;

		// distorted pixel
		GeometryMath_F32.mult(K,temp0,temp1);

		distX = temp1.x;
		distY = temp1.y;
	}
}
