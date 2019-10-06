/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.chess;

import boofcv.struct.image.GrayF32;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;

/**
 * TODO describe
 *
 * @author Peter Abeles
 */
public class SaddlePointXCorner {
	int radius;
	GrayF32 image;

	DMatrixRMaj A = new DMatrixRMaj(1,6);
	DMatrixRMaj X = new DMatrixRMaj(6,1);
	DMatrixRMaj B = new DMatrixRMaj(1,1);

	// TODO try faster pinv
	LinearSolverDense<DMatrixRMaj> pinv =  LinearSolverFactory_DDRM.pseudoInverse(false);

	double saddleX,saddleY;

	public void setImage(GrayF32 image) {
		this.image = image;
	}

	public boolean process(int cx , int cy ) {

		double oldX = cx;
		double oldY = cy;
		boolean success = false;
		boolean aborted = false;
		for (int i = 0; i < 10; i++) {
			// find the sample region
			int x0 = cx - radius;
			int x1 = cx + radius + 1;
			int y0 = cy - radius;
			int y1 = cy + radius + 1;

			// ensure the entire region is inside the image
			if( x0 < 0 )
				x0 = 0;
			if( y0 < 0 )
				y0 = 0;
			if( x1 > image.width )
				x1 = image.width;
			if( y1 > image.height )
				y1 = image.height;

			// number of pixels/equations to sample
			int N = (x1-x0)*(y1-y0);
			if( N < 6 ) {
				aborted = true;
				break;
			}

			if (!solveCoefficients(cx, cy, x0, x1, y0, y1, N)) {
				aborted = true;
				break;
			}

			if( !solveSaddlePoint(cx, cy) ) {
				aborted = true;
				break;
			}
			success = true;

			cx = (int)(saddleX+0.5);
			cy = (int)(saddleY+0.5);

			if( !image.isInBounds(cx,cy)) {
				aborted = true;
				break;
			}

			// if the center point didn't change then it has converged
			if( cx == (int)(oldX+0.5) && cy == (int)(oldY+0.5) ) {
				break;
			}
			oldX = saddleX;
			oldY = saddleY;
			break;
		}
		if( aborted ) {
			saddleX = oldX;
			saddleY = oldY;
		}
		return success;
	}

	private boolean solveSaddlePoint(int cx, int cy) {
		// At the saddle point the gradient will be zero
		// f_x = 0 = b + d*y + 2*e*x
		// f_y = 0 = c + d*x + 2*f*y
//		double a = X.data[0];
		double b = X.data[1];
		double c = X.data[2];
		double d = X.data[3];
		double e = X.data[4];
		double f = X.data[5];

		// Solving for x and y
		double sy = (-2*c*e + d*b)/(4*f*e-d*d);
		double sx = -(b+d*sy)/(2*e);

		// make sure it's a saddle point; fxx*fyy - fxy*fxy < 0
		// fxx = 2e, fyy = 2*f, fxy = d
		if( 4*e*f - d*d < 0 ) {
			// undo translation and scaling
			saddleX = sx*radius + cx;
			saddleY = sy*radius + cy;
			return true;
		} else {
			// fail hard if there's a bug and the return value is ignored
			saddleX = Double.NaN;
			saddleY = Double.NaN;
			return false;
		}
	}

	private boolean solveCoefficients(int cx, int cy, int x0, int x1, int y0, int y1, int n) {
		if( n > 500 )
			System.out.println("WTF "+n);
		A.reshape(n,6);
		B.reshape(n,1);

		double r = radius;

		int idxA = 0;
		for (int py = y0,i=0; py < y1; py++) {
			double yy = (py-cy)/r; // make coordinates around (0,0) and [-1,1] for numerics
			for (int px = x0; px < x1; px++,i++) {
				double xx = (px-cx)/r;

				B.data[i] = image.unsafe_get(px,py);

				// f(x,c) = a + b*x + c*y + d*x*y + e*x*x + f*y*y
				A.data[idxA++] = 1.0;
				A.data[idxA++] = xx;
				A.data[idxA++] = yy;
				A.data[idxA++] = xx*yy;
				A.data[idxA++] = xx*xx;
				A.data[idxA++] = yy*yy;
			}
		}

		// Solve for the coefficients using a pseudo inverse
		if( !pinv.setA(A))
			return false;
		pinv.solve(B, X);
		return true;
	}

	public double getSaddleX() {
		return saddleX;
	}

	public double getSaddleY() {
		return saddleY;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}
}
