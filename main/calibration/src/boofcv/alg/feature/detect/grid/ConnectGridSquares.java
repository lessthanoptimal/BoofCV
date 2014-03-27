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

package boofcv.alg.feature.detect.grid;

import boofcv.alg.feature.detect.quadblob.QuadBlob;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Connects calibration grid squares together.
 *
 * @author Peter Abeles
 */
public class ConnectGridSquares {

	/**
	 * Finds the largest island in the graph and returns that
	 */
	public static List<QuadBlob> pruneSmallIslands(List<QuadBlob> blobs) {
		blobs = new ArrayList<QuadBlob>(blobs);
		List<QuadBlob> largest = new ArrayList<QuadBlob>();
		
		while( blobs.size() > 0 ) {
			List<QuadBlob> c = findIsland(blobs.remove(0),blobs);
			if( c.size() > largest.size() )
				largest = c;
		}

		return largest;
	}

	/**
	 * Given an initial node, it searches for every node which is connect to it.  Seed
	 * is assumed to have already been removed from 'all'
	 *
	 */
	public static List<QuadBlob> findIsland( QuadBlob seed , List<QuadBlob> all )
	{
		List<QuadBlob> ret = new ArrayList<QuadBlob>();
		Stack<QuadBlob> open = new Stack<QuadBlob>();

		ret.add(seed);
		open.push(seed);
		
		while( open.size() > 0 ) {
			QuadBlob s = open.pop();
			
			for( QuadBlob c : s.conn ) {
				if( !ret.contains(c) ) {
					all.remove(c);
					ret.add(c);
					open.add(c);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * For each blob it finds the blobs which are directly next to it.  Up to 4 blobs can be next to
	 * any blob and two connected blobs should have the closes side approximately parallel.
	 */
	public static void connect( List<QuadBlob> blobs , double spaceToSquareRatio ) {

		for( int i = 0; i < blobs.size(); i++ ) {
			QuadBlob b = blobs.get(i);

			// examine each side in the blob
			for( int j = 0; j < 4; j++ ) {
				Point2D_I32 c0 = b.corners.get(j);
				Point2D_I32 c1 = b.corners.get((j+1)%4);
				Point2D_I32 c2 = b.corners.get((j+2)%4);
				Point2D_I32 c3 = b.corners.get((j+3)%4);

				double bestDistance = Math.max(2,b.largestSide*0.2);
				QuadBlob bestQuad = null;
				for( int k = i+1; k < blobs.size(); k++ ) {
					QuadBlob candidate = blobs.get(k);

					double d = predictedDistance(c0,c1,c2,c3,candidate,spaceToSquareRatio);
					if( !Double.isNaN(d) && d < bestDistance ) {
						bestDistance = d;
						bestQuad = candidate;
					}
				}

				// make the connection
				if( bestQuad != null ) {
					bestQuad.conn.add(b);
					b.conn.add(bestQuad);
				}
			}
		}
	}

	/**
	 * Distance metric between the two quads for a particular side.  Allows the distance between the two sides
	 * to be arbitrary since it can very.  Better way would be to use information in target description
	 */
	private static double predictedDistance( Point2D_I32 a0 , Point2D_I32 a1,
											 Point2D_I32 a2 , Point2D_I32 a3 ,
											 QuadBlob b , double spaceToSquareRatio ) {

		double total = 0;

		// find equivalent side on b
		Point2D_I32 b0 = findClosest(a0,a1,b);
		Point2D_I32 b1 = findClosest(a1,a0,b);

		total += Math.abs(a0.distance(a1) - b0.distance(b1));

		// crudely take on account perspective distortion by scaling it by the difference in the two faces
		double predictedSpace = spaceToSquareRatio*a0.distance(a1);
		double actualSpace = (a0.distance(b0) + a1.distance(b1))/2;

		// perspective distortion requires a very large margin of error here.  This could be shrunk
		// if it was accurately estimated
		if( Math.abs(predictedSpace-actualSpace)/predictedSpace > 0.6 )
			return Double.NaN;

		total +=  Math.abs(predictedSpace-actualSpace);

		// continue computing the difference between side lengths
		Point2D_I32 predB0 = new Point2D_I32(b0.x + a0.x-a3.x , b0.y + a0.y-a3.y);
		Point2D_I32 predB1 = new Point2D_I32(b1.x + a1.x-a2.x , b1.y + a1.y-a2.y);

		Point2D_I32 b2 = findClosest(predB1,predB0,b);
		Point2D_I32 b3 = findClosest(predB0,predB1,b);

		total += Math.abs(a2.distance(a3) - b2.distance(b3));
		total += Math.abs(a0.distance(a3) - b0.distance(b3));
		total += Math.abs(a1.distance(a2) - b1.distance(b2));

		return total/(4+1);
	}

	private static Point2D_I32 findClosest(Point2D_I32 a0 , Point2D_I32 a1, QuadBlob blob )  {
		double best = Double.MAX_VALUE;
		Point2D_I32 bestPoint = null;

		for( int i = 0; i < 4; i++ ) {
			// encourage it to be close to the first corner
			double d = a0.distance(blob.corners.get(i));
			// penalize it for being close to the other corner
			double d1 = a1.distance(blob.corners.get(i));

			d -= d1;

			if( d < best ) {
				best = d;
				bestPoint = blob.corners.get(i);
			}
		}

		return bestPoint;
	}

	/**
	 * Creates a proper copy of the sub-graph removing any references to nodes not
	 * in this sub-graph.
	 */
	public static List<QuadBlob> copy( List<QuadBlob> input ) {
		List<QuadBlob> output = new ArrayList<QuadBlob>();

		for( QuadBlob i : input ) {
			QuadBlob o = new QuadBlob();
			o.contour = i.contour;
			o.corners = i.corners;
			o.center = i.center;

			o.largestSide = i.largestSide;
			o.smallestSide = i.smallestSide;

			output.add(o);
		}

		// only add connections if they are in the sub-graph
		for( int index = 0; index < input.size(); index++ ) {
			QuadBlob in = input.get(index);
			QuadBlob out = output.get(index);

			for( QuadBlob c : in.conn ) {
				int i = input.indexOf(c);
				if( i >= 0) {
					out.conn.add(output.get(i));
				}
			}
		}

		return output;
	}
}
