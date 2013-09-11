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

package boofcv.alg.geo.pose;

import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Various utility functions for {@link PnPLepetitEPnP}.
 *
 * @author Peter Abeles
 */
public class UtilLepetitEPnP {

	/**
	 * Computes the camera control points as weighted sum of null points.
	 *
	 * @param beta Beta values which describe the weights of null points
	 * @param nullPts Null points that the camera point is a weighted sum of
	 * @param cameraPts The output
	 */
	public static void computeCameraControl(double beta[],
											List<Point3D_F64> nullPts[],
											FastQueue<Point3D_F64> cameraPts ,
											int numControl )
	{
		cameraPts.reset();
		for( int i = 0; i < numControl; i++ ) {
			cameraPts.grow().set(0,0,0);
		}

		for( int i = 0; i < numControl; i++ ) {
			double b = beta[i];
//			System.out.printf("%7.3f ", b);

			for( int j = 0; j < numControl; j++ ) {
				Point3D_F64 s = cameraPts.get(j);
				Point3D_F64 p = nullPts[i].get(j);
				s.x += b*p.x;
				s.y += b*p.y;
				s.z += b*p.z;
			}
		}
	}

	/**
	 * Extracts the linear constraint matrix for case 1 from the full 6x10 constraint matrix.
	 */
	public static void constraintMatrix6x4( DenseMatrix64F L_6x10 , DenseMatrix64F L_6x4 ) {

		int index = 0;
		for( int i = 0; i < 6; i++ ) {
			L_6x4.data[index++] = L_6x10.get(i,0);
			L_6x4.data[index++] = L_6x10.get(i,1);
			L_6x4.data[index++] = L_6x10.get(i,3);
			L_6x4.data[index++] = L_6x10.get(i,6);
		}
	}

	/**
	 * Extracts the linear constraint matrix for case 2 from the full 6x10 constraint matrix.
	 */
	public static void constraintMatrix6x3( DenseMatrix64F L_6x10 , DenseMatrix64F L_6x3 ) {

		int index = 0;
		for( int i = 0; i < 6; i++ ) {
			L_6x3.data[index++] = L_6x10.get(i,0);
			L_6x3.data[index++] = L_6x10.get(i,1);
			L_6x3.data[index++] = L_6x10.get(i,4);
		}
	}

	/**
	 * Extracts the linear constraint matrix for case 3 from the full 6x10 constraint matrix.
	 */
	public static void constraintMatrix6x6( DenseMatrix64F L_6x10 , DenseMatrix64F L_6x6 ) {

		int index = 0;
		for( int i = 0; i < 6; i++ ) {
			L_6x6.data[index++] = L_6x10.get(i,0);
			L_6x6.data[index++] = L_6x10.get(i,1);
			L_6x6.data[index++] = L_6x10.get(i,2);
			L_6x6.data[index++] = L_6x10.get(i,4);
			L_6x6.data[index++] = L_6x10.get(i,5);
			L_6x6.data[index++] = L_6x10.get(i,7);
		}
	}

	/**
	 * Linear constraint matrix used in case 4 in the general case
	 *
	 * @param L Constraint matrix
	 * @param y Vector containing distance between world control points
	 * @param controlWorldPts List of world control points
	 * @param nullPts Null points
	 */
	public static void constraintMatrix6x10( DenseMatrix64F L , DenseMatrix64F y ,
											 FastQueue<Point3D_F64> controlWorldPts ,
											 List<Point3D_F64> nullPts[] ) {
		int row = 0;
		for( int i = 0; i < 4; i++ ) {
			Point3D_F64 ci = controlWorldPts.get(i);
			Point3D_F64 vai = nullPts[0].get(i);
			Point3D_F64 vbi = nullPts[1].get(i);
			Point3D_F64 vci = nullPts[2].get(i);
			Point3D_F64 vdi = nullPts[3].get(i);

			for( int j = i+1; j < 4; j++ , row++) {
				Point3D_F64 cj = controlWorldPts.get(j);
				Point3D_F64 vaj = nullPts[0].get(j);
				Point3D_F64 vbj = nullPts[1].get(j);
				Point3D_F64 vcj = nullPts[2].get(j);
				Point3D_F64 vdj = nullPts[3].get(j);

				y.set(row,0,ci.distance2(cj));

				double xa = vai.x-vaj.x;
				double ya = vai.y-vaj.y;
				double za = vai.z-vaj.z;

				double xb = vbi.x-vbj.x;
				double yb = vbi.y-vbj.y;
				double zb = vbi.z-vbj.z;

				double xc = vci.x-vcj.x;
				double yc = vci.y-vcj.y;
				double zc = vci.z-vcj.z;

				double xd = vdi.x-vdj.x;
				double yd = vdi.y-vdj.y;
				double zd = vdi.z-vdj.z;

				double da = xa*xa + ya*ya + za*za;
				double db = xb*xb + yb*yb + zb*zb;
				double dc = xc*xc + yc*yc + zc*zc;
				double dd = xd*xd + yd*yd + zd*zd;

				double dab = xa*xb + ya*yb + za*zb;
				double dac = xa*xc + ya*yc + za*zc;
				double dad = xa*xd + ya*yd + za*zd;
				double dbc = xb*xc + yb*yc + zb*zc;
				double dbd = xb*xd + yb*yd + zb*zd;
				double dcd = xc*xd + yc*yd + zc*zd;

				L.set(row,0,da);
				L.set(row,1,2*dab);
				L.set(row,2,2*dac);
				L.set(row,3,2*dad);
				L.set(row,4,db);
				L.set(row,5,2*dbc);
				L.set(row,6,2*dbd);
				L.set(row,7,dc);
				L.set(row,8,2*dcd);
				L.set(row,9,dd);
			}
		}
	}

	/**
	 * Extracts the linear constraint matrix for case 1 from the full 6x10 constraint matrix.
	 */
	public static void constraintMatrix3x3a( DenseMatrix64F L_3x6 , DenseMatrix64F L_3x3 ) {

		int index = 0;
		for( int i = 0; i < 3; i++ ) {
			L_3x3.data[index++] = L_3x6.get(i,0);
			L_3x3.data[index++] = L_3x6.get(i,1);
			L_3x3.data[index++] = L_3x6.get(i,2);
		}
	}

	/**
	 * Extracts the linear constraint matrix for planar case 2 from the full 4x6 constraint matrix.
	 */
	public static void constraintMatrix3x3( DenseMatrix64F L_3x6 ,
											DenseMatrix64F L_6x3 ) {

		int index = 0;
		for( int i = 0; i < 3; i++ ) {
			L_6x3.data[index++] = L_3x6.get(i,0);
			L_6x3.data[index++] = L_3x6.get(i,1);
			L_6x3.data[index++] = L_3x6.get(i,3);
		}
	}

	/**
	 * Linear constraint matrix for case 4 in the planar case
	 *
	 * @param L Constraint matrix
	 * @param y Vector containing distance between world control points
	 * @param controlWorldPts List of world control points
	 * @param nullPts Null points
	 */
	public static void constraintMatrix3x6( DenseMatrix64F L , DenseMatrix64F y ,
											FastQueue<Point3D_F64> controlWorldPts ,
											List<Point3D_F64> nullPts[] ) {
		int row = 0;
		for( int i = 0; i < 3; i++ ) {
			Point3D_F64 ci = controlWorldPts.get(i);
			Point3D_F64 vai = nullPts[0].get(i);
			Point3D_F64 vbi = nullPts[1].get(i);
			Point3D_F64 vci = nullPts[2].get(i);

			for( int j = i+1; j < 3; j++ , row++) {
				Point3D_F64 cj = controlWorldPts.get(j);
				Point3D_F64 vaj = nullPts[0].get(j);
				Point3D_F64 vbj = nullPts[1].get(j);
				Point3D_F64 vcj = nullPts[2].get(j);

				y.set(row,0,ci.distance2(cj));

				double xa = vai.x-vaj.x;
				double ya = vai.y-vaj.y;
				double za = vai.z-vaj.z;

				double xb = vbi.x-vbj.x;
				double yb = vbi.y-vbj.y;
				double zb = vbi.z-vbj.z;

				double xc = vci.x-vcj.x;
				double yc = vci.y-vcj.y;
				double zc = vci.z-vcj.z;

				double da = xa*xa + ya*ya + za*za;
				double db = xb*xb + yb*yb + zb*zb;
				double dc = xc*xc + yc*yc + zc*zc;

				double dab = xa*xb + ya*yb + za*zb;
				double dac = xa*xc + ya*yc + za*zc;
				double dbc = xb*xc + yb*yc + zb*zc;

				L.set(row,0,da);
				L.set(row,1,2*dab);
				L.set(row,2,2*dac);
				L.set(row,3,db);
				L.set(row,4,2*dbc);
				L.set(row,5,dc);
			}
		}
	}

	/**
	 * Computes the residuals (difference between observed and predicted) given 4 control points.
	 */
	public static void residuals_Control4( DenseMatrix64F L_full , DenseMatrix64F y ,
										   double beta[] , double r[] )
	{
		double b0 = beta[0]; double b1 = beta[1]; double b2 = beta[2]; double b3 = beta[3];

		final double ld[] = L_full.data;

		for( int i = 0; i < 6; i++ ) {
			int li = L_full.numCols*i;
			double residual = -y.data[i];
			residual += ld[li++]*b0*b0;
			residual += ld[li++]*b0*b1;
			residual += ld[li++]*b0*b2;
			residual += ld[li++]*b0*b3;
			residual += ld[li++]*b1*b1;
			residual += ld[li++]*b1*b2;
			residual += ld[li++]*b1*b3;
			residual += ld[li++]*b2*b2;
			residual += ld[li++]*b2*b3;
			residual += ld[li  ]*b3*b3;

			r[i] = residual;
		}
	}

	/**
	 * Computes the residuals (difference between observed and predicted given 3 control points.
	 */
	public static void residuals_Control3( DenseMatrix64F L_full , DenseMatrix64F y ,
										   double beta[] , double r[] )
	{
		double b0 = beta[0]; double b1 = beta[1]; double b2 = beta[2];

		final double ld[] = L_full.data;

		for( int i = 0; i < 3; i++ ) {
			double residual = -y.data[i];
			int li = L_full.numCols*i;
			residual += ld[li++]*b0*b0;
			residual += ld[li++]*b0*b1;
			residual += ld[li++]*b0*b2;
			residual += ld[li++]*b1*b1;
			residual += ld[li++]*b1*b2;
			residual += ld[li  ]*b2*b2;

			r[i] = residual;
		}
	}

	/**
	 * Computes the Jacobian given 4 control points.
	 */
	public static void jacobian_Control4( DenseMatrix64F L_full ,
										  double beta[] , DenseMatrix64F A )
	{
		int indexA = 0;

		double b0 = beta[0]; double b1 = beta[1]; double b2 = beta[2]; double b3 = beta[3];

		final double ld[] = L_full.data;

		for( int i = 0; i < 6; i++ ) {
			int li = L_full.numCols*i;
			A.data[indexA++] = 2*ld[li+0]*b0 +   ld[li+1]*b1 +   ld[li+2]*b2 +   ld[li+3]*b3;
			A.data[indexA++] =   ld[li+1]*b0 + 2*ld[li+4]*b1 +   ld[li+5]*b2 +   ld[li+6]*b3;
			A.data[indexA++] =   ld[li+2]*b0 +   ld[li+5]*b1 + 2*ld[li+7]*b2 +   ld[li+8]*b3;
			A.data[indexA++] =   ld[li+3]*b0 +   ld[li+6]*b1 +   ld[li+8]*b2 + 2*ld[li+9]*b3;
		}
	}

	/**
	 * Computes the Jacobian given 3 control points.
	 */
	public static void jacobian_Control3( DenseMatrix64F L_full ,
										  double beta[] ,  DenseMatrix64F A)
	{
		int indexA = 0;

		double b0 = beta[0]; double b1 = beta[1]; double b2 = beta[2];

		final double ld[] = L_full.data;

		for( int i = 0; i < 3; i++ ) {
			int li = L_full.numCols*i;
			A.data[indexA++] = 2*ld[li+0]*b0 +   ld[li+1]*b1 +   ld[li+2]*b2;
			A.data[indexA++] =   ld[li+1]*b0 + 2*ld[li+3]*b1 +   ld[li+4]*b2;
			A.data[indexA++] =   ld[li+2]*b0 +   ld[li+4]*b1 + 2*ld[li+5]*b2;
		}
	}
}
