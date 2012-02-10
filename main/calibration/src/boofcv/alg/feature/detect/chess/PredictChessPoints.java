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

package boofcv.alg.feature.detect.chess;

import boofcv.alg.feature.detect.quadblob.QuadBlob;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class PredictChessPoints {
	/**
	 * Predict the location of calibration points using the graph. Calibration points should appear
	 * at intersecting corners of squares.
	 *
	 * @return list of ordered approximate calibration points
	 */
	public static List<Point2D_I32> predictPoints( QuadBlob corner , int numRows , int numCols ) {

		List<Point2D_I32> points = new ArrayList<Point2D_I32>();

		// add all the columns in this row
		QuadBlob a = corner.conn.get(0);
		addRow(corner,a,points,true);
		
		int foundCols = points.size();

		// step down through the rows
		corner = next(corner,a,true);
		while( corner.conn.size() != 1  ) {
			addRow(corner,a,points,false);
			a = corner.conn.get(0) == a ? corner.conn.get(1) : corner.conn.get(0);
			addRow(corner,a,points,true);
			corner = next(corner,a,true);
		}
		addRow(corner,a,points,false);

		// must have the appropriate number of rows and columns
		if( foundCols != numCols ) {
			// transpose the results
			List<Point2D_I32> tran = new ArrayList<Point2D_I32>();
			for( int i = 0; i < numRows; i++ ) {
				for( int j = 0; j < numCols; j++ ) {
					tran.add(points.get(j*numRows+i));
				}
			}
			points = tran;
		}
		
		return points;
	}

	public static void addRow( QuadBlob a , QuadBlob b , List<Point2D_I32> points , boolean bottom ) {
		while( b != null ) {
			addPoint(points,a,b);
			a = next(a,b,!bottom);
			addPoint(points,a,b);
			b = next(b,a,bottom);
		}
	}

	public static void addPoint(List<Point2D_I32> points , QuadBlob a , QuadBlob b ) {

		int x = (a.center.x+b.center.x)/2;
		int y = (a.center.y+b.center.y)/2;

		points.add(new Point2D_I32(x,y));
	}

	public static QuadBlob next( QuadBlob a , QuadBlob b , boolean cw ) {

		// angle from a to b
		double thetaAB = Math.atan2(b.center.y-a.center.y,b.center.x-a.center.x);

		double bestAngle = 0;
		QuadBlob best = null;

		for( QuadBlob c : b.conn ) {
			if( c == a )
				continue;
			double thetaBC = Math.atan2(c.center.y-b.center.y,c.center.x-b.center.x);

			double dir = cw ? UtilAngle.distanceCW(thetaBC, thetaAB) : UtilAngle.distanceCCW(thetaBC, thetaAB);

			if( dir > Math.PI )
				continue;
			
			if( dir > bestAngle ) {
				bestAngle = dir;
				best = c;
			}
		}

		return best;
	}
}
