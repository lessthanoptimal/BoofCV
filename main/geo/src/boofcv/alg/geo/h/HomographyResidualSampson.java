/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.h;

import boofcv.alg.geo.ModelObservationResidualN;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.ops.CommonOps;

/**
 * <p>
 * Computes the Sampson distance residual for a set of observations given a homography matrix.  For use
 * in least-squares non-linear optimization algorithms. The full 9 elements of the 3x3 matrix are used
 * to parameterize.  This has an extra redundant parameter, but is much simpler and should not affect
 * the final result.
 * </p>
 *
 * <p>
 * R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </p>
 *
 * @author Peter Abeles
 */
public class HomographyResidualSampson
		implements ModelObservationResidualN<DenseMatrix64F,AssociatedPair>
{

	DenseMatrix64F H;
	Point2D_F64 temp = new Point2D_F64();

	DenseMatrix64F J = new DenseMatrix64F(2,4);
	DenseMatrix64F JJ = new DenseMatrix64F(2,2);
	DenseMatrix64F e = new DenseMatrix64F(2,1);
	DenseMatrix64F x = new DenseMatrix64F(2,1);
	DenseMatrix64F error = new DenseMatrix64F(4,1);

	LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.linear(2);

	@Override
	public void setModel(DenseMatrix64F H) {
		this.H = H;
	}

	@Override
	public int computeResiduals(AssociatedPair p, double[] residuals, int index) {

		GeometryMath_F64.mult(H, p.p1, temp);

		double top1 = error1(p.p1.x,p.p1.y,p.p2.x,p.p2.y);
		double top2 = error2(p.p1.x,p.p1.y,p.p2.x,p.p2.y);

		computeJacobian(p.p1,p.p2);
		// JJ = J*J'
		CommonOps.multTransB(J, J, JJ);

		// solve JJ'*x = -e
		e.data[0] = -top1;
		e.data[1] = -top2;

		if( solver.setA(JJ) ) {
			solver.solve(e,x);
			// -J'(J*J')^-1*e
			CommonOps.multTransA(J,x,error);
			residuals[index++] = error.data[0];
			residuals[index++] = error.data[1];
			residuals[index++] = error.data[2];
			residuals[index++] = error.data[3];
		} else {
			residuals[index++] = 0;
			residuals[index++] = 0;
			residuals[index++] = 0;
			residuals[index++] = 0;
		}

		return index;
	}

	/**
	 *
	 * x2 = H*x1
	 *
	 */
	public double error1( double x1 , double y1 ,
						  double x2 , double y2 )
	{
		double ret;

		ret = -(x1*H.get(1,0)+y1*H.get(1,1)+H.get(1,2));
		ret += y2*(x1*H.get(2,0)+y1*H.get(2,1)+H.get(2,2));

		return ret;
	}

	public double error2( double x1 , double y1 ,
						  double x2 , double y2 )
	{
		double ret;

		ret = (x1*H.get(0,0)+y1*H.get(0,1)+H.get(0,2));
		ret -= x2*(x1*H.get(2,0)+y1*H.get(2,1)+H.get(2,2));

		return ret;
	}

	public void computeJacobian( Point2D_F64 x1 , Point2D_F64 x2 ) {
		J.data[0] = -H.get(1,0) + x2.y*H.get(2,0);
		J.data[1] = -H.get(1,1) + x2.y*H.get(2,1);
		J.data[2] = 0;
		J.data[3] = x1.x*H.get(2,0) + x1.y*H.get(2,1) + H.get(2,2);

		J.data[4] = H.get(0,0) - x2.x*H.get(2,0);
		J.data[5] = H.get(0,1) - x2.x*H.get(2,1);
		J.data[6] = -J.data[3];
		J.data[7] = 0;
	}

	@Override
	public int getN() {
		return 4;
	}
}
