/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.calibgrid;

import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * Connects calibration grid squares together.
 *
 * @author Peter Abeles
 */
// TODO require both nodes to have center line intersect side
// TODO require line connecting to be a reasonable length relative to max side length
// TODO modify DetectCalibrationTarget to not modify the grid!
public class ConnectGridSquares {

	/**
	 * For each blob it finds the blobs which are directly next to it.  Up to 4 blobs can be next to
	 * any blob and two connected blobs should have the closes side approximately parallel.
	 */
	public static void connect( List<SquareBlob> blobs ) throws InvalidTarget {

		LineSegment2D_F64 centerLine = new LineSegment2D_F64();
		LineSegment2D_F64 cornerLine = new LineSegment2D_F64();

		for( int i = 0; i < blobs.size(); i++ ) {
			SquareBlob b = blobs.get(i);
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
				SquareBlob bestBlob = null;

				// find the blob which is closest to it and "approximately parallel"
				for( int k = i+1; k < blobs.size(); k++ ) {
					SquareBlob c = blobs.get(k);

					centerLine.b.set(c.center.x,c.center.y);

					// two sides are declared approximately parallel if the center line intersects
					// the sides of blob 'b'
					if(Intersection2D_F64.intersection(cornerLine, centerLine, null) != null ) {
						double d = c.center.distance2(b.center);
						if( d < best ) {
							best = d;
							bestBlob = c;
						}
					}
				}

				if( bestBlob != null ) {
					bestBlob.conn.add(b);
					b.conn.add(bestBlob);
				}
			}

			// corners will have two connections, sides 3, and inner ones 4
			if( b.conn.size() < 2 && b.conn.size() > 4 )
				throw new InvalidTarget("Bad number of square connections. "+b.conn.size());
		}
	}

	/**
	 * Checks to see if a connection has already been added to this side
	 */
	private static boolean checkConnection( List<SquareBlob> connections ,
											LineSegment2D_F64 centerLine,
											LineSegment2D_F64 cornerLine ) {

		for( SquareBlob b : connections ) {
			centerLine.b.set(b.center.x,b.center.y);
			if(Intersection2D_F64.intersection(cornerLine,centerLine,null) != null ) {
				return true;
			}
		}

		return false;
	}
}
