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

package boofcv.alg.geo.pose;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrix2x2;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF2;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.ops.ConvertDMatrixStruct;

import java.util.List;

/**
 * <p>A plane based pose estimation algorithm [1]. Works by first finding the homography between two sets of 2D points. Then
 * solve for the pose using 3D pose information of the points. Since this is plane based the 3D points must all
 * lie on a plane (z=0). A minimum of 4 points is required to estimate the pose. The planar assumption enables
 * it to run very fast and accurately.</p>
 *
 * <p>[1] Collins, Toby, and Adrien Bartoli. "Infinitesimal plane-based pose estimation." International journal
 * of computer vision 109.3 (2014): 252-286.</p>
 *
 * @author Peter Abeles
 */
public class PnPInfinitesimalPlanePoseEstimation {

	Estimate1ofEpipolar estimateHomography;

	// Offset required to center points in world
	Point2D_F64 center = new Point2D_F64();

	// Homography matrix
	DMatrixRMaj H = new DMatrixRMaj(3,3);

	// J is a 2x2 matrix
	// v is a 2x1 vector
	DMatrix2x2 J = new DMatrix2x2();
	double v1,v2;

	// final pose estimates
	Se3_F64 pose0 = new Se3_F64();
	Se3_F64 pose1 = new Se3_F64();

	DMatrixRMaj K_x = new DMatrixRMaj(3,3);
	DMatrixRMaj R_v = new DMatrixRMaj(3,3);
	DMatrixRMaj tmp = new DMatrixRMaj(3,3);

	DMatrix2x2 A = new DMatrix2x2();
	DMatrix2x2 AA = new DMatrix2x2();
	DMatrix2x2 B = new DMatrix2x2();

	DMatrix2x2 R22 = new DMatrix2x2();

	Vector3D_F64 l0 = new Vector3D_F64();
	Vector3D_F64 l1 = new Vector3D_F64();
	Vector3D_F64 ca = new Vector3D_F64();

	// Used to solve for translation
	DMatrixRMaj W = new DMatrixRMaj(1,3);
	DMatrixRMaj WW = new DMatrixRMaj(3,3);
	DMatrixRMaj y = new DMatrixRMaj(1,1);
	DMatrixRMaj Wty = new DMatrixRMaj(1,1);


	public PnPInfinitesimalPlanePoseEstimation(Estimate1ofEpipolar estimateHomography) {
		this.estimateHomography = estimateHomography;
	}

	public PnPInfinitesimalPlanePoseEstimation() {
		this(FactoryMultiView.computeHomographyTLS());
	}

	/**
	 * Estimates the transform from world coordinate system to camera given known points and observations.
	 * For each observation p1=World 3D location. z=0 is implicit. p2=Observed location of points in image in
	 * normalized image coordinates
	 *
	 * @param points List of world coordinates in 2D (p1) and normalized image coordinates (p2)
	 * @return true if successful or false if it fails to estimate
	 */
	public boolean process( List<AssociatedPair> points )
	{
		if( points.size() < estimateHomography.getMinimumPoints())
			throw new IllegalArgumentException("At least "+estimateHomography.getMinimumPoints()+" must be provided");

		// TODO center location of points in model

		if( !estimateHomography.process(points,H) )
			return false;

		// make sure H[2,2] == 1
		CommonOps_DDRM.divide(H.get(2,2),H);

		// Jacobian of pi(H[u_0 1]^T)
		J.a11 = H.unsafe_get(0,0) - H.unsafe_get(2,0)*H.unsafe_get(0,2);
		J.a12 = H.unsafe_get(0,1) - H.unsafe_get(2,1)*H.unsafe_get(0,2);
		J.a21 = H.unsafe_get(1,0) - H.unsafe_get(2,0)*H.unsafe_get(1,2);
		J.a22 = H.unsafe_get(1,1) - H.unsafe_get(2,1)*H.unsafe_get(1,2);

		// v = (H[0,1],H[1,2])
		v1 = H.unsafe_get(0,2);
		v2 = H.unsafe_get(1,2);

		// Solve for rotations
		IPPE(pose0.R,pose1.R);

		// Solve for translations
		estimateTranslation(pose0.R,points,pose0.T);
		estimateTranslation(pose1.R,points,pose1.T);

		// compute the reprojection error for each pose TODO

		// Make sure the best pose is the first one TODO

		// TODO Undo centering adjustment

		return true;
	}

	/**
	 * Estimate's the translation given the previously found rotation
	 * @param R Rotation matrix
	 * @param T (Output) estimated translation
	 */
	void estimateTranslation( DMatrixRMaj R , List<AssociatedPair> points , Vector3D_F64 T )
	{
		final int N = points.size();
		W.reshape(N,3);
		y.reshape(N,1);
		Wty.reshape(3,1);

		DMatrix3x3 Rtmp = new DMatrix3x3();
		ConvertDMatrixStruct.convert(R,Rtmp);

		int indexY = 0,indexW = 0;
		for (int i = 0; i < N; i++) {
			AssociatedPair p = points.get(i);

			// rotate into camera frame
			double u1 = Rtmp.a11*p.p1.x + Rtmp.a12*p.p1.y;
			double u2 = Rtmp.a21*p.p1.x + Rtmp.a22*p.p1.y;
			double u3 = Rtmp.a31*p.p1.x + Rtmp.a32*p.p1.y;

			W.data[indexW++] = 1;
			W.data[indexW++] = 0;
			W.data[indexW++] = -p.p2.x;
			W.data[indexW++] = 0;
			W.data[indexW++] = 1;
			W.data[indexW++] = -p.p2.y;

			y.data[indexY++] = p.p2.x*u3 - u1;
			y.data[indexY++] = p.p2.y*u3 - u2;
		}

		//======= Compute Pseudo Inverse
		// WW = inv(W^T*W)
		CommonOps_DDRM.multTransA(W,W,WW);
		CommonOps_DDRM.invert(WW);

		// W^T*y
		CommonOps_DDRM.multTransA(W,y,Wty);

		// translation = inv(W^T*W)*W^T*y
		W.reshape(N,1);
		CommonOps_DDRM.mult(WW,Wty,W);

		T.x = W.data[0];
		T.y = W.data[1];
		T.z = W.data[2];
	}

	/**
	 * Solves the IPPE problem
	 */
	protected void IPPE( DMatrixRMaj R1 , DMatrixRMaj R2 ) {
		// Equation 23 - Compute R_v from v
		compute_Kv();

		// [B|0] = [I2|-v]*R_v
		compute_B(B,R_v,v1,v2);

		CommonOps_DDF2.invert(B,B);

		// A = inv(B)*J
		CommonOps_DDF2.mult(B,J,A);

		// Find the largest singular value of A
		double gamma = largestSingularValue(A);

		// Compute R22 from A
		CommonOps_DDF2.scale(1.0/gamma,A,R22);

		// B = I2 - R22^T * Rss
		CommonOps_DDF2.setIdentity(B);
		CommonOps_DDF2.multAddTransA(-1,R22,R22,B);

		double b1 = Math.sqrt(B.a11);
		double b2 = Math.signum(B.a12)*Math.sqrt(B.a22);

		// [c;a] = [R22;b^T]*[1;0] cross [R22;b^T]*[0;1]
		l0.set(R22.a11,R22.a12,b1);
		l1.set(R22.a21,R22.a22,b2);

		l0.cross(l1,ca);

		// R1 = R_v*[R22, +c; b^T , a ]
		constructR(R1,R_v,R22,b1,b2,ca,1,tmp);
		constructR(R2,R_v,R22,b1,b2,ca,1,tmp);
	}

	/**
	 * R = R_v*[R22, sgn*c; sgn*b^T , a ]
	 */
	static void constructR( DMatrixRMaj R, DMatrixRMaj R_v , DMatrix2x2 R22 ,
							double b1 , double b2 , Vector3D_F64 ca ,
							double sign , DMatrixRMaj tmp )
	{
		tmp.data[0]= R22.a11;
		tmp.data[1]= R22.a12;
		tmp.data[2]= sign*ca.x;

		tmp.data[3]= R22.a21;
		tmp.data[4]= R22.a22;
		tmp.data[5]= sign*ca.y;

		tmp.data[6]= sign*b1;
		tmp.data[7]= sign*b2;
		tmp.data[8]= ca.z;

		CommonOps_DDRM.mult(R_v,tmp,R);
	}

	/**
	 * [B|0] = [I2|-v]*R_v
	 */
	static void compute_B(DMatrix2x2 B , DMatrixRMaj R_v , double v1 , double v2 ) {
		B.a11 = R_v.data[0]+R_v.data[6]*-v1;
		B.a12 = R_v.data[1]+R_v.data[7]*-v1;
		B.a21 = R_v.data[3]+R_v.data[6]*-v2;
		B.a22 = R_v.data[4]+R_v.data[7]*-v2;
	}

	double largestSingularValue( DMatrix2x2 A ) {
		CommonOps_DDF2.mult(A,A,AA);

		double a11_m_a22 = AA.a11-AA.a22;
		return 0.5*(AA.a11 + AA.a22 + Math.sqrt(a11_m_a22*a11_m_a22 - 4.0*AA.a12*AA.a12));
	}

	/**
	 *  R_v is a 3x3 matrix
	 *  R_v = I + sin(theta)*[k]_x + (1-cos(theta))[k]_x^2
	 */
	void compute_Kv() {
		double t = Math.sqrt(v1*v1 + v2*v2);
		double s = Math.sqrt(t*t + 1);
		double cosT = 1.0/s;
		double sinT = Math.sqrt(1-1.0/(s*s));

		K_x.data[0] = 0;   K_x.data[1] = 0;   K_x.data[2] = v1;
		K_x.data[3] = 0;   K_x.data[4] = 0;   K_x.data[5] = v2;
		K_x.data[6] = -v1; K_x.data[7] = -v2; K_x.data[8] = 0;
		CommonOps_DDRM.divide(t,K_x);
		CommonOps_DDRM.setIdentity(R_v);
		CommonOps_DDRM.addEquals(R_v,sinT,K_x);
		CommonOps_DDRM.multAdd(1.0-cosT,K_x,K_x,R_v);
	}

	public int getMinimumPoints() {
		return estimateHomography.getMinimumPoints();
	}
}
