/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.line;


import boofcv.struct.feature.MatrixOfList;
import georegression.geometry.UtilLine2D_F32;
import georegression.metric.ClosestPoint2D_F32;
import georegression.metric.Distance2D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_F32;

import java.util.Iterator;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class LineImageOps {

	public static void pruneClutteredGrids( MatrixOfList<LineSegment2D_F32> lines , int threshold )
	{
		int N = lines.width*lines.height;
		for( int i = 0; i < N; i++ ) {
			List<LineSegment2D_F32> l = lines.grid[i];
			if( l.size() > threshold )
				l.clear();
		}
	}

	public static void pruneSmall( List<LineSegment2D_F32> lines , float threshold )
	{
		threshold *= threshold;

		Iterator<LineSegment2D_F32> iter = lines.iterator();

		while( iter.hasNext() ) {
			LineSegment2D_F32 l = iter.next();
			if( l.getLength2() <= threshold ) {
				iter.remove();
			}
		}
	}

	public static void mergeSimilar( List<LineSegment2D_F32> lines , float thresholdAngle , float thresholdDist )
	{
		Point2D_F32 p = new Point2D_F32();
		for( int i = 0; i < lines.size(); i++ ) {
			LineSegment2D_F32 a = lines.get(i);
			double thetaA = UtilAngle.atanSafe(a.slopeY(),a.slopeX());

			for( int j = i+1; j < lines.size(); j++ ) {
				LineSegment2D_F32 b = lines.get(j);
				double thetaB = UtilAngle.atanSafe(b.slopeY(),b.slopeX());

				// see if they are nearly parallel
				if( UtilAngle.distHalf(thetaA,thetaB) > thresholdAngle )
					continue;

				if( !(Distance2D_F32.distance(a,b.a) < thresholdDist || Distance2D_F32.distance(a,b.b) < thresholdDist) )
					continue;

				// if they don't intersect let it be
//				if( Intersection2D_F32.intersection(a, b, p) == null )
//					continue;

				mergeIntoA(a,b);
				lines.remove(j);
				j--;// counteract the ++
			}
		}
	}

	private static void mergeIntoA( LineSegment2D_F32 a , LineSegment2D_F32 b )
	{
		LineParametric2D_F32 paraA = UtilLine2D_F32.convert(a,null);

		Point2D_F32 pts[] = new Point2D_F32[4];
		float t[] = new float[4];

		pts[0] = a.a;
		pts[1] = a.b;
		pts[2] = b.a;
		pts[3] = b.b;

		for( int i = 0; i < 4; i++ )
			t[i] = ClosestPoint2D_F32.closestPointT(paraA,pts[i]);

		float min = t[0];
		float max = min;
		int indexMin = 0;
		int indexMax = 0;

		for( int i = 1; i < 4; i++ ) {
			float v = t[i];
			if( v < min ) {
				min = v;
				indexMin = i;
			}
			if( v > max ) {
				max = v;
				indexMax = i;
			}
		}

		// set the first line to the extreme points on each line
		a.a.set(pts[indexMin]);
		a.b.set(pts[indexMax]);
	}
}
