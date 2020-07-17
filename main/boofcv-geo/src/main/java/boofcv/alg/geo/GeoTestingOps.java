/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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


import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Operations useful for unit tests
 *
 * @author Peter Abeles
 */
public class GeoTestingOps {

	public static List<Point3D_F64> createList3_F64( int N ) {
		List<Point3D_F64> ret = new ArrayList<>();
		for( int i = 0; i < N; i++ ) {
			ret.add( new Point3D_F64());
		}
		return ret;
	}

	public static List<Point4D_F64> randomPointsH_F64(double min, double max,
													  int num, Random rand)
	{
		List<Point4D_F64> ret = new ArrayList<>();

		for( int i = 0; i < num; i++ ) {
			double x = rand.nextDouble()*(max-min)+min;
			double y = rand.nextDouble()*(max-min)+min;
			double z = rand.nextDouble()*(max-min)+min;
			double w = rand.nextDouble()*0.01 + 1.0;

			ret.add(new Point4D_F64(x,y,z,w));
		}

		return ret;
	}

	public static List<Point4D_F64> randomPointsH_F64(double minX, double maxX,
													  double minY, double maxY,
													  double minZ, double maxZ,
													  int num, Random rand)
	{
		List<Point4D_F64> ret = new ArrayList<>();

		for( int i = 0; i < num; i++ ) {
			double x = rand.nextDouble()*(maxX-minX)+minX;
			double y = rand.nextDouble()*(maxY-minY)+minY;
			double z = rand.nextDouble()*(maxZ-minZ)+minZ;
			double w = rand.nextDouble()*0.01 + 1.0;

			ret.add(new Point4D_F64(x,y,z,w));
		}

		return ret;
	}

	public static List<Point3D_F64> randomPoints_F64(double minX, double maxX,
													 double minY, double maxY,
													 double minZ, double maxZ,
													 int num, Random rand)
	{
		List<Point3D_F64> ret = new ArrayList<>();

		for( int i = 0; i < num; i++ ) {
			double x = rand.nextDouble()*(maxX-minX)+minX;
			double y = rand.nextDouble()*(maxY-minY)+minY;
			double z = rand.nextDouble()*(maxZ-minZ)+minZ;

			ret.add(new Point3D_F64(x,y,z));
		}

		return ret;
	}

	public static List<Point2D_F64> randomPoints_F64(double minX, double maxX,
													 double minY, double maxY,
													 int num, Random rand)
	{
		List<Point2D_F64> ret = new ArrayList<>();

		for( int i = 0; i < num; i++ ) {
			double x = rand.nextDouble()*(maxX-minX)+minX;
			double y = rand.nextDouble()*(maxY-minY)+minY;

			ret.add(new Point2D_F64(x,y));
		}

		return ret;
	}

	public static double residualError( double[] residuals ) {
		double total = 0;
		for( double d : residuals ) {
			total += d*d;
		}
		return total*0.5;
	}

	/**
	 * Checks to see if the two SE3 are equal up to a scale factor
	 * @param tolT Translational tolerance
	 * @param tolRad Rotational tolerance in radians
	 * @return true if within tolerance
	 */
	public static boolean isEqualsScale(Se3_F64 a , Se3_F64 b , double tolT , double tolRad ) {
		double scaleA = a.T.norm();
		double scale = scaleA / b.T.norm();

		if(UtilEjml.isUncountable(scale))
			scale = 1;


		double dx = a.T.x - scale*b.T.x;
		double dy = a.T.y - scale*b.T.y;
		double dz = a.T.z - scale*b.T.z;
		if( Math.sqrt(dx*dx + dy*dy + dz*dz) > scaleA*tolT )
			return false;

		var R = new DMatrixRMaj(3,3);
		CommonOps_DDRM.multTransB(a.R,b.R,R);
		double errorRad = ConvertRotation3D_F64.matrixToRodrigues(R,null).theta;
		return Math.abs(errorRad) <= tolRad;
	}
}
