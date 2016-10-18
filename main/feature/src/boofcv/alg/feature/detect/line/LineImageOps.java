/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.line;


import boofcv.struct.feature.MatrixOfList;
import georegression.geometry.UtilLine2D_F32;
import georegression.metric.ClosestPoint2D_F32;
import georegression.metric.Distance2D_F32;
import georegression.metric.Intersection2D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_F32;
import org.ddogleg.sorting.QuickSort_F32;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class LineImageOps {

	static double foo = 1e-4;

	public static List<LineParametric2D_F32>
	pruneRelativeIntensity( List<LineParametric2D_F32> lines ,
							float intensity[] ,
							float fraction )
	{
		int indexSort[] = new int[ intensity.length ];
		QuickSort_F32 sort = new QuickSort_F32();
		sort.sort(intensity, lines.size(), indexSort);

		float threshold = intensity[ indexSort[ lines.size()-1] ]*fraction;

		List<LineParametric2D_F32> ret = new ArrayList<>();

		for( int i = 0; i < lines.size(); i++ ) {
			if( intensity[i] >= threshold ) {
				ret.add( lines.get(i));
			}
		}
		return ret;
	}

	/**
	 * Prunes similar looking lines, but keeps the lines with the most intensity.
	 *
	 * @param lines
	 * @param intensity
	 * @param toleranceAngle
	 * @return
	 */
	public static List<LineParametric2D_F32>
	pruneSimilarLines( List<LineParametric2D_F32> lines ,
					   float intensity[] ,
					   float toleranceAngle ,
					   float toleranceDist ,
					   int imgWidth ,
					   int imgHeight )
	{

		int indexSort[] = new int[ intensity.length ];
		QuickSort_F32 sort = new QuickSort_F32();
		sort.sort(intensity, lines.size(), indexSort);

		float theta[] = new float[ lines.size() ];
		List<LineSegment2D_F32> segments = new ArrayList<>(lines.size());

		for( int i = 0; i < lines.size(); i++ ) {
			LineParametric2D_F32 l = lines.get(i);
			theta[i] = UtilAngle.atanSafe(l.getSlopeY(),l.getSlopeX());
			segments.add( convert(l,imgWidth,imgHeight));
		}

		for( int i = segments.size()-1; i >= 0; i-- ) {
			LineSegment2D_F32 a = segments.get(indexSort[i]);
			if( a == null ) continue;

			for( int j = i-1; j >= 0; j-- ) {
				LineSegment2D_F32 b = segments.get(indexSort[j]);

				if( b == null )
					continue;

				if( UtilAngle.distHalf(theta[indexSort[i]],theta[indexSort[j]]) > toleranceAngle )
					continue;

				Point2D_F32 p = Intersection2D_F32.intersection(a,b,null);
				if( p != null && p.x >= 0 && p.y >= 0 && p.x < imgWidth && p.y < imgHeight ) {
					segments.set(indexSort[j],null);
				} else {
					float distA = Distance2D_F32.distance(a,b.a);
					float distB = Distance2D_F32.distance(a,b.b);

					if( distA <= toleranceDist || distB < toleranceDist ) {
						segments.set(indexSort[j],null);
					}
				}
			}
		}

		List<LineParametric2D_F32> ret = new ArrayList<>();
		for( int i = 0; i < segments.size(); i++ ) {
			if( segments.get(i) != null ) {
				ret.add( lines.get(i));
			}
		}

		return ret;
	}

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
		for( int i = 0; i < lines.size(); i++ ) {
			LineSegment2D_F32 a = lines.get(i);
			double thetaA = UtilAngle.atanSafe(a.slopeY(),a.slopeX());

			// finds the best match and merges
			// could speed up by just picking the first match, but results would depend on input order
			while( true ) {
				int indexBest = -1;
				double distanceBest = thresholdDist;
				for( int j = i+1; j < lines.size(); j++ ) {
					LineSegment2D_F32 b = lines.get(j);
					double thetaB = UtilAngle.atanSafe(b.slopeY(),b.slopeX());

					// see if they are nearly parallel
					if( UtilAngle.distHalf(thetaA,thetaB) > thresholdAngle )
						continue;

					float distA = Distance2D_F32.distance(a,b.a);
					float distB = Distance2D_F32.distance(a,b.b);
					float dist = Math.min(distA,distB);

					if( dist < distanceBest ) {
						distanceBest = dist;
						indexBest = j;
					}
				}
				if( indexBest != -1 ) {
					mergeIntoA(a,lines.remove(indexBest));
					thetaA = UtilAngle.atanSafe(a.slopeY(),a.slopeX());
				} else {
					break;
				}
			}
		}
	}

	private static void mergeIntoA( LineSegment2D_F32 a , LineSegment2D_F32 b )
	{
		LineParametric2D_F32 paraA = UtilLine2D_F32.convert(a,(LineParametric2D_F32)null);

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

	/**
	 * Find the point in which the line intersects the image border and create a line segment at those points
	 */
	public static LineSegment2D_F32 convert(LineParametric2D_F32 l,
									 int width, int height) {
		double t0 = (0-l.p.x)/l.getSlopeX();
		double t1 = (0-l.p.y)/l.getSlopeY();
		double t2 = (width-l.p.x)/l.getSlopeX();
		double t3 = (height-l.p.y)/l.getSlopeY();

		Point2D_F32 a = computePoint(l, t0);
		Point2D_F32 b = computePoint(l, t1);
		Point2D_F32 c = computePoint(l, t2);
		Point2D_F32 d = computePoint(l, t3);

		List<Point2D_F32> inside = new ArrayList<>();
		checkAddInside(width , height , a, inside);
		checkAddInside(width , height , b, inside);
		checkAddInside(width , height , c, inside);
		checkAddInside(width , height , d, inside);

		if( inside.size() != 2 ) {
			return null;
//			System.out.println("interesting");
		}
		return new LineSegment2D_F32(inside.get(0),inside.get(1));
	}

	public static void checkAddInside(int width, int height, Point2D_F32 a, List<Point2D_F32> inside) {
		if( a.x >= -foo && a.x <= width+foo && a.y >= -foo && a.y <= height+foo ) {

			for( Point2D_F32 p : inside ) {
				if( p.distance(a) < foo )
					return;
			}
			inside.add(a);
		}
	}

	public static Point2D_F32 computePoint(LineParametric2D_F32 l, double t) {
		return new Point2D_F32((float)(t*l.slope.x+l.p.x) , (float)(t*l.slope.y + l.p.y));
	}
}
