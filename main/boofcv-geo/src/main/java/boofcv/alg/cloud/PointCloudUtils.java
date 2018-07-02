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

import boofcv.struct.Point3dRgbI;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class PointCloudUtils {
	public static void prune(List<Point3dRgbI> cloud , int minNeighbors , double radius ) {
		NearestNeighbor nn = FactoryNearestNeighbor.kdtree();
		nn.init(3);

		List<double[]> points = new ArrayList<double[]>();

		for( int i = 0; i < cloud.size(); i++ ) {
			Point3dRgbI p = cloud.get(i);
			points.add( new double[]{p.x,p.y,p.z});
		}
		nn.setPoints(points,null);
		FastQueue<NnData> results = new FastQueue<>(NnData.class,true);

		System.out.println("  before="+cloud.size());
		for( int i = cloud.size()-1; i >= 0; i-- ) {
			nn.findNearest(points.get(i),radius,minNeighbors,results);

			if( results.size < minNeighbors ) {
				cloud.remove(i);
			}
		}
		System.out.println("  after="+cloud.size());
	}
}
