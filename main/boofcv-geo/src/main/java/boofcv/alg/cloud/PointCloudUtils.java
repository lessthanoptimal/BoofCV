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

package boofcv.alg.cloud;

import georegression.struct.point.Point3D_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class PointCloudUtils {

	public static double autoScale(  List<Point3D_F64> cloud ) {
		Point3D_F64 mean = new Point3D_F64();
		Point3D_F64 stdev = new Point3D_F64();

		statistics(cloud, mean, stdev);

		double scale = Math.sqrt(Math.max(Math.max(stdev.x,stdev.y),stdev.z));

		scale /= 100;

		int N = cloud.size();
		for (int i = 0; i < N ; i++) {
			cloud.get(i).scale(1.0/scale);
		}

		return scale;
	}

	public static void statistics( List<Point3D_F64> cloud , Point3D_F64 mean , Point3D_F64 stdev ) {
		final int N = cloud.size();
		for (int i = 0; i < N; i++) {
			Point3D_F64 p = cloud.get(i);
			mean.x += p.x / N;
			mean.y += p.y / N;
			mean.z += p.z / N;
		}

		for (int i = 0; i < N; i++) {
			Point3D_F64 p = cloud.get(i);
			double dx = p.x-mean.x;
			double dy = p.y-mean.y;
			double dz = p.z-mean.z;

			stdev.x += dx*dx/N;
			stdev.y += dy*dy/N;
			stdev.z += dz*dz/N;
		}
	}

	public static void prune(List<Point3D_F64> cloud , int minNeighbors , double radius ) {
		NearestNeighbor nn = FactoryNearestNeighbor.kdtree();
		nn.init(3);

		List<double[]> points = new ArrayList<double[]>();

		for( int i = 0; i < cloud.size(); i++ ) {
			Point3D_F64 p = cloud.get(i);
			points.add( new double[]{p.x,p.y,p.z});
		}
		nn.setPoints(points,null);
		FastQueue<NnData> results = new FastQueue<>(NnData.class,true);

		for( int i = cloud.size()-1; i >= 0; i-- ) {
			nn.findNearest(points.get(i),radius,minNeighbors,results);

			if( results.size < minNeighbors ) {
				cloud.remove(i);
			}
		}
	}

	public static void prune(List<Point3D_F64> cloud , GrowQueue_I32 colors, int minNeighbors , double radius ) {
		NearestNeighbor nn = FactoryNearestNeighbor.kdtree();
		nn.init(3);

		List<double[]> points = new ArrayList<double[]>();

		for( int i = 0; i < cloud.size(); i++ ) {
			Point3D_F64 p = cloud.get(i);
			points.add( new double[]{p.x,p.y,p.z});
		}
		nn.setPoints(points,null);
		FastQueue<NnData> results = new FastQueue<>(NnData.class,true);

		for( int i = cloud.size()-1; i >= 0; i-- ) {
			nn.findNearest(points.get(i),radius,minNeighbors,results);

			if( results.size < minNeighbors ) {
				cloud.remove(i);
				colors.remove(i);
			}
		}
	}
}
