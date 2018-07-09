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

import boofcv.alg.nn.KdTreePoint3D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class PointCloudUtils {

	/**
	 * Automatically rescales the point cloud based so that it has a standard deviation of 'target'
	 * @param cloud The point cloud
	 * @param target The desired standard deviation of the cloud. Try 100
	 * @return The selected scale factor
	 */
	public static double autoScale(  List<Point3D_F64> cloud , double target ) {
		Point3D_F64 mean = new Point3D_F64();
		Point3D_F64 stdev = new Point3D_F64();

		statistics(cloud, mean, stdev);

		double scale = target/(Math.max(Math.max(stdev.x,stdev.y),stdev.z));


		int N = cloud.size();
		for (int i = 0; i < N ; i++) {
			cloud.get(i).scale(scale);
		}

		return scale;
	}

	/**
	 * Computes the mean and standard deviation of each axis in the point cloud computed in dependently
	 * @param cloud (Input) Cloud
	 * @param mean (Output) mean of each axis
	 * @param stdev (Output) standard deviation of each axis
	 */
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
		stdev.x = Math.sqrt(stdev.x);
		stdev.y = Math.sqrt(stdev.y);
		stdev.z = Math.sqrt(stdev.z);
	}

	/**
	 * Prunes points from the point cloud if they have very few neighbors
	 *
	 * @param cloud Point cloud
	 * @param minNeighbors Minimum number of neighbors for it to not be pruned
	 * @param radius search distance for neighbors
	 */
	public static void prune(List<Point3D_F64> cloud , int minNeighbors , double radius ) {
		if( minNeighbors < 0 )
			throw new IllegalArgumentException("minNeighbors must be >= 0");
		NearestNeighbor<Point3D_F64> nn = FactoryNearestNeighbor.kdtree(new KdTreePoint3D_F64() );

		nn.setPoints(cloud,false);
		FastQueue<NnData<Point3D_F64>> results = new FastQueue(NnData.class,true);

		// It will always find itself
		minNeighbors += 1;

		// distance is Euclidean squared
		radius *= radius;

		for( int i = cloud.size()-1; i >= 0; i-- ) {
			nn.findNearest(cloud.get(i),radius,minNeighbors,results);

			if( results.size < minNeighbors ) {
				cloud.remove(i);
			}
		}
	}

	/**
	 * Prunes points from the point cloud if they have very few neighbors
	 *
	 * @param cloud Point cloud
	 * @param colors Color of each point.
	 * @param minNeighbors Minimum number of neighbors for it to not be pruned
	 * @param radius search distance for neighbors
	 */
	public static void prune(List<Point3D_F64> cloud , GrowQueue_I32 colors, int minNeighbors , double radius ) {
		if( minNeighbors < 0 )
			throw new IllegalArgumentException("minNeighbors must be >= 0");
		NearestNeighbor<Point3D_F64> nn = FactoryNearestNeighbor.kdtree(new KdTreePoint3D_F64());

		nn.setPoints(cloud,false);
		FastQueue<NnData<Point3D_F64>> results = new FastQueue(NnData.class,true);

		// It will always find itself
		minNeighbors += 1;

		// distance is Euclidean squared
		radius *= radius;

		for( int i = cloud.size()-1; i >= 0; i-- ) {
			nn.findNearest(cloud.get(i),radius,minNeighbors,results);

			if( results.size < minNeighbors ) {
				cloud.remove(i);
				colors.remove(i);
			}
		}
	}
}
