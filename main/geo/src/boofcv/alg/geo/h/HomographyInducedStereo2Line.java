/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.geo.PairLineNorm;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilPlane3D_F64;
import georegression.metric.Intersection3D_F64;
import georegression.struct.line.LineParametric3D_F64;
import georegression.struct.plane.PlaneGeneral3D_F64;
import georegression.struct.plane.PlaneNormal3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 *
 * <p>
 * Computes the homography induced by a plane from 2 line correspondences.  Works with both calibrated and
 * uncalibrated cameras.  The Fundamental/Essential matrix must be known.  The found homography will be from view 1
 * to view 2.  The passed in Fundamental matrix must have the following properties for each set of
 * point correspondences: x2*F*x1 = 0, where x1 and x2 are views of the point in image 1 and image 2 respectively.
 * </p>
 *
 * <p>
 * Algorithm: For each line correspondence it finds the intersection between the two planes which define the observed
 * lines.  These planes are created by line in the image and the camera origin.  From the two found lines, the
 * equations of the plane are extracted in 3D space.  This equation is then combined with information from
 * the fundamental matrix to compute the induced homography.
 * </p>
 *
 * <p>
 * NOTE: Any line which is parallel to camera baseline can't be used.  The lines in both cameras will have the same
 * slope, causing their intersection to be a plane instead of a line.  This can be a significant issue since for
 * many stereo rigs it would mean no perfectly horizontal lines can be used.
 * </p>
 *
 * @author Peter Abeles
 */
public class HomographyInducedStereo2Line {

	// Epipole in camera 2
	private Point3D_F64 e2 = new Point3D_F64();
	// A = cross(e2)*F
	private DenseMatrix64F A = new DenseMatrix64F(3,3);

	// The found homography from view 1 to view 2
	private DenseMatrix64F H = new DenseMatrix64F(3,3);

	// pick a reasonable scale and sign
	private AdjustHomographyMatrix adjust = new AdjustHomographyMatrix();

	// storage for intermediate results
	private Point3D_F64 Al0 = new Point3D_F64();
	private Point3D_F64 Al1 = new Point3D_F64();

	private Point3D_F64 v = new Point3D_F64();
	private DenseMatrix64F av = new DenseMatrix64F(3,3);

	private PlaneGeneral3D_F64 planeA = new PlaneGeneral3D_F64();
	private PlaneGeneral3D_F64 planeB = new PlaneGeneral3D_F64();

	private LineParametric3D_F64 intersect0 = new LineParametric3D_F64();
	private LineParametric3D_F64 intersect1 = new LineParametric3D_F64();

	private PlaneNormal3D_F64 pi = new PlaneNormal3D_F64();
	private Vector3D_F64 from0to1 = new Vector3D_F64();

	private PlaneGeneral3D_F64 pi_gen = new PlaneGeneral3D_F64();

	/**
	 * Specify the fundamental matrix and the camera 2 epipole.
	 *
	 * @param F Fundamental matrix.
	 * @param e2 Epipole for camera 2.  If null it will be computed internally.
	 */
	public void setFundamental( DenseMatrix64F F , Point3D_F64 e2 ) {
		if( e2 != null )
			this.e2.set(e2);
		else {
			MultiViewOps.extractEpipoles(F,new Point3D_F64(),this.e2);
		}
		GeometryMath_F64.multCrossA(this.e2,F,A);
	}

	/**
	 * Computes the homography based on two unique lines on the plane
	 *
	 * @param line0 Line on the plane
	 * @param line1 Line on the plane
	 */
	public boolean process(PairLineNorm line0, PairLineNorm line1) {

		// Find plane equations of second lines in the first view
		double a0 = GeometryMath_F64.dot(e2,line0.l2);
		double a1 = GeometryMath_F64.dot(e2,line1.l2);

		GeometryMath_F64.multTran(A,line0.l2,Al0);
		GeometryMath_F64.multTran(A,line1.l2,Al1);

		// find the intersection of the planes created by each view of each line
		// first line
		planeA.set( line0.l1.x , line0.l1.y , line0.l1.z , 0 );
		planeB.set( Al0.x , Al0.y , Al0.z , a0 );

		if( !Intersection3D_F64.intersect(planeA,planeB,intersect0) )
			return false;
		intersect0.slope.normalize(); // maybe this will reduce overflow problems?

		// second line
		planeA.set( line1.l1.x , line1.l1.y , line1.l1.z , 0 );
		planeB.set( Al1.x , Al1.y , Al1.z , a1 );

		if( !Intersection3D_F64.intersect(planeA,planeB,intersect1) )
			return false;

		intersect1.slope.normalize();

		// compute the plane defined by these two lines
		from0to1.x = intersect1.p.x - intersect0.p.x;
		from0to1.y = intersect1.p.y - intersect0.p.y;
		from0to1.z = intersect1.p.z - intersect0.p.z;

		// the plane's normal will be the cross product of one of the slopes and a line connecting the two lines
		GeometryMath_F64.cross(intersect0.slope,from0to1,pi.n);
		pi.p.set(intersect0.p);

		// convert this plane description into general format
		UtilPlane3D_F64.convert(pi,pi_gen);

		v.set(pi_gen.A/pi_gen.D,pi_gen.B/pi_gen.D,pi_gen.C/pi_gen.D);

		// H = A - e2*v^T
		GeometryMath_F64.outerProd(e2,v,av);
		CommonOps.subtract(A, av, H);

		// pick a good scale and sign for H
		adjust.adjust(H, line0);

		return true;
	}

	public DenseMatrix64F getHomography() {
		return H;
	}
}
