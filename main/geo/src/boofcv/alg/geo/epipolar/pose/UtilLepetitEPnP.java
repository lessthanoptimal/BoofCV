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

package boofcv.alg.geo.epipolar.pose;

import georegression.struct.point.Point3D_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Various utility functions for {@link PnPLepetitEPnP}.
 * 
 * @author Peter Abeles
 */
// TODO merge back into main class?
public class UtilLepetitEPnP {

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
	 * Linear constraint matrix used in case 4
	 *
	 * @param L Constraint matrix
	 * @param y Vector containing distance between world control points
	 * @param controlWorldPts List of world control points
	 * @param nullPts Null points
	 */
	public static void constraintMatrix6x10( DenseMatrix64F L , DenseMatrix64F y ,
											 List<Point3D_F64> controlWorldPts ,
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

}
