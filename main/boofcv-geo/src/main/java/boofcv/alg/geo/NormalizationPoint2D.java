/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo;

import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;

/**
 * Describes how to normalize a set of points such that they have zero mean and variance. This is equivalent
 * to applying the matrix below.  Normalization is often needed as a preprocessing step for solving linear equations.
 * Greatly reduces bias and numerical errors.
 *
 * <pre>
 * N = [ 1/&sigma;_x     0      -&mu;_x/&sigma;_x ]
 *     [    0   1/&sigma;_y 0   -&mu;_y/&sigma;_y ]
 *     [    0      0          1    ]
 * </pre>
 *
 * <p>
 * Y. Ma, S. Soatto, J. Kosecka, and S. S. Sastry, "An Invitation to 3-D Vision" Springer-Verlad, 2004
 * </p>
 *
 * @author Peter Abeles
 */
public class NormalizationPoint2D {
	public double meanX,stdX;
	public double meanY,stdY;

	public NormalizationPoint2D() {
	}

	public NormalizationPoint2D(double meanX, double stdX, double meanY, double stdY) {
		this.meanX = meanX;
		this.stdX = stdX;
		this.meanY = meanY;
		this.stdY = stdY;
	}

	public void apply(Point2D_F64 p) {
		p.x = (p.x - meanX)/ stdX;
		p.y = (p.y - meanY)/ stdY;
	}

	public void apply(Point2D_F64 input, Point2D_F64 output) {
		output.x = (input.x - meanX)/ stdX;
		output.y = (input.y - meanY)/ stdY;
	}

	public void remove(Point2D_F64 p) {
		p.x = p.x*stdX + meanX;
		p.y = p.y*stdY + meanY;
	}

	public void remove(Point2D_F64 input, Point2D_F64 output ) {
		output.x = input.x*stdX + meanX;
		output.y = input.y*stdY + meanY;
	}

	public DMatrixRMaj matrix() {
		DMatrixRMaj M = new DMatrixRMaj(3,3);
		M.set(0,0,1.0/stdX);
		M.set(1,1,1.0/stdY);
		M.set(0,2,-meanX/stdX);
		M.set(1,2,-meanY/stdY);
		M.set(2,2,1);
		return M;
	}

	public DMatrixRMaj matrixInv() {
		DMatrixRMaj M = new DMatrixRMaj(3,3);
		M.set(0,0,stdX);
		M.set(1,1,stdY);
		M.set(0,2,meanX);
		M.set(1,2,meanY);
		M.set(2,2,1);
		return M;
	}

	public boolean isEquals( NormalizationPoint2D a , double tol ) {
		if( Math.abs(a.meanX-meanX) > tol )
			return false;
		if( Math.abs(a.meanY-meanY) > tol )
			return false;
		if( Math.abs(a.stdX-stdX) > tol )
			return false;
		if( Math.abs(a.stdY-stdY) > tol )
			return false;
		return true;
	}

}
