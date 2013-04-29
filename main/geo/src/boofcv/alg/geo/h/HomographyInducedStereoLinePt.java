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

package boofcv.alg.geo.h;

import boofcv.alg.geo.MultiViewOps;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.PairLineNorm;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 *
 * <p>
 * Computes the homography induced by a plane from correspondences of a line and a point.  Works with both
 * calibrated and uncalibrated cameras.  The Fundamental/Essential matrix must be known.  The found homography will be
 * from view 1 to view 2.  The passed in Fundamental matrix must have the following properties for each set of
 * point correspondences: x2*F*x1 = 0, where x1 and x2 are views of the point in image 1 and image 2 respectively.
 * For more information see [1].
 * </p>
 *
 * <p>
 * NOTE: Any line which is parallel to camera baseline can't be used.  The lines in both cameras will have the same
 * slope, causing their intersection to be a plane instead of a line.  This can be a significant issue since for
 * many stereo rigs it would mean no perfectly horizontal lines can be used.
 * </p>
 *
 * <p>
 * [1] R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003
 * </p>
 *
 * @author Peter Abeles
 */
public class HomographyInducedStereoLinePt {

	// Fundamental matrix
	private DenseMatrix64F F;
	// Epipole in camera 2
	private Point3D_F64 e2 = new Point3D_F64();

	// The found homography from view 1 to view 2
	private DenseMatrix64F H = new DenseMatrix64F(3,3);

	// pick a reasonable scale and sign
	private AdjustHomographyMatrix adjust = new AdjustHomographyMatrix();

	// storage for intermediate results
	private DenseMatrix64F el = new DenseMatrix64F(3,3);
	private DenseMatrix64F lf = new DenseMatrix64F(3,3);

	private Point3D_F64 Fx = new Point3D_F64();

	private Point3D_F64 t0 = new Point3D_F64();
	private Point3D_F64 t1 = new Point3D_F64();

	/**
	 * Specify the fundamental matrix and the camera 2 epipole.
	 *
	 * @param F Fundamental matrix.
	 * @param e2 Epipole for camera 2.  If null it will be computed internally.
	 */
	public void setFundamental( DenseMatrix64F F , Point3D_F64 e2 ) {
		this.F = F;
		if( e2 != null )
			this.e2.set(e2);
		else {
			MultiViewOps.extractEpipoles(F,new Point3D_F64(),this.e2);
		}
	}

	/**
	 * Computes the homography based on a line and point on the plane
	 * @param line Line on the plane
	 * @param point Point on the plane
	 */
	public void process(PairLineNorm line, AssociatedPair point) {

		// t0 = (F*x) cross l'
		GeometryMath_F64.mult(F,point.p1,Fx);
		GeometryMath_F64.cross(Fx,line.getL2(),t0);
		// t1 = x' cross ((f*x) cross l')
		GeometryMath_F64.cross(point.p2, t0, t1);
		// t0 = x' cross e'
		GeometryMath_F64.cross(point.p2,e2,t0);

		double top = GeometryMath_F64.dot(t0,t1);
		double bottom = t0.normSq()*(line.l1.x*point.p1.x + line.l1.y*point.p1.y + line.l1.z);

		// e' * l^T
		GeometryMath_F64.outerProd(e2, line.l1, el);
		// cross(l')*F
		GeometryMath_F64.multCrossA(line.l2, F, lf);

		CommonOps.add(lf,top/bottom,el,H);

		// pick a good scale and sign for H
		adjust.adjust(H, point);
	}

	public DenseMatrix64F getHomography() {
		return H;
	}
}
