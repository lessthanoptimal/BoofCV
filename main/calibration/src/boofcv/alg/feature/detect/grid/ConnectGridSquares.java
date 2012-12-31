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

package boofcv.alg.feature.detect.grid;

import boofcv.alg.feature.detect.quadblob.QuadBlob;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineSegment2D_F64;
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
	public static void connect( List<QuadBlob> blobs ) {

		LineSegment2D_F64 centerLine = new LineSegment2D_F64();
		LineSegment2D_F64 cornerLine = new LineSegment2D_F64();

		for( int i = 0; i < blobs.size(); i++ ) {
			QuadBlob b = blobs.get(i);
			// for each pair of corners, find the closest blob
			// center line between blobs must intersect line segment between two corners

			centerLine.a.set(b.center.x,b.center.y);

			// examine each side in the blob
			for( int j = 0; j < 4; j++ ) {
				Point2D_I32 c0 = b.corners.get(j);
				Point2D_I32 c1 = b.corners.get((j+1)%4);

				cornerLine.a.set(c0.x,c0.y);
				cornerLine.b.set(c1.x,c1.y);

				// see if another node already placed a connection to this side
				if( checkConnection(b.conn,centerLine,cornerLine)) {
					continue;
				}

				double best = Double.MAX_VALUE;
				QuadBlob bestBlob = null;

				// find the blob which is closest to it and "approximately parallel"
				for( int k = i+1; k < blobs.size(); k++ ) {
					QuadBlob c = blobs.get(k);

					centerLine.b.set(c.center.x,c.center.y);

					// two sides are declared approximately parallel if the center line intersects
					// the sides of blob 'b'
					if(Intersection2D_F64.intersection(cornerLine, centerLine, null) != null ) {
						double d = c.center.distance2(b.center);
						if( d < best  ) {
							// the physical distance between centers is 2 time a side's length
							// and perspective distortion would only make it shorter,
							// 2.5 = 2 + fudge factor
							// NOTE: Could be improved by having it be the length of perpendicular sides
							double max = Math.min(c.largestSide, b.largestSide)*2.5;
							if( d < max*max ) {
								best = d;
								bestBlob = c;
							}
						}
					}
				}

				if( bestBlob != null ) {
					bestBlob.conn.add(b);
					b.conn.add(bestBlob);
				}
			}
		}
	}

	/**
	 * Checks to see if a connection has already been added to this side
	 */
	private static boolean checkConnection( List<QuadBlob> connections ,
											LineSegment2D_F64 centerLine,
											LineSegment2D_F64 cornerLine ) {

		for( QuadBlob b : connections ) {
			centerLine.b.set(b.center.x,b.center.y);
			if(Intersection2D_F64.intersection(cornerLine,centerLine,null) != null ) {
				return true;
			}
		}

		return false;
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
