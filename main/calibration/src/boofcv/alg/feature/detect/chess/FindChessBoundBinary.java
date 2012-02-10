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

import boofcv.alg.feature.detect.grid.ConnectGridSquares;
import boofcv.alg.feature.detect.grid.UtilCalibrationGrid;
import boofcv.alg.feature.detect.quadblob.DetectQuadBlobsBinary;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class FindChessBoundBinary {

	DetectQuadBlobsBinary detectBlobs;

	int expectedBlobs;

	int numRows;
	int numCols;

	List<Point2D_I32> boundingQuad;
	List<QuadBlob> graphBlobs;
	List<QuadBlob> cornerBlobs;

	// how close two corners need to be for them to be connected
	int connectThreshold;

	public FindChessBoundBinary(int numRows, int numCols , int minContourSize )
	{
		this.numRows = numRows;
		this.numCols = numCols;

		expectedBlobs = numRows*numCols + (numCols-1)*(numRows-1);

		this.connectThreshold = minContourSize/5;
		detectBlobs = new DetectQuadBlobsBinary(minContourSize,0.25,expectedBlobs);
	}

	public boolean process( ImageUInt8 binary ) {

		// detect blobs
		if( !detectBlobs.process(binary) )
			return false;

		// connect blobs
		graphBlobs = detectBlobs.getDetected();

		connect(graphBlobs, 15);

		// Remove all but the largest islands in the graph to reduce the number of combinations
		ConnectGridSquares.pruneSmallIslands(graphBlobs);
		if( graphBlobs.size() != expectedBlobs )
			return false;

		// Examine connections
		cornerBlobs = new ArrayList<QuadBlob>();
		if( !checkGraphStructure(graphBlobs,cornerBlobs))
			return false;

		// find bounds
		boundingQuad = findBoundingQuad(cornerBlobs);
		
		return true;
	}


	/**
	 * Connect blobs together based on corner distance. If two corners are uniquely close
	 * together then.
	 */
	public static void connect( List<QuadBlob> blobs , double tol )
	{
		for( int i = 0; i < blobs.size(); i++ ) {
			QuadBlob a = blobs.get(i);
			
			for( int indexA = 0; indexA < 4; indexA++ ) {
				Point2D_I32 ac = a.corners.get(indexA);

				int count = 0;
				QuadBlob match = null;
				
				for( int j = i+1; j < blobs.size(); j++ ) {
					QuadBlob b = blobs.get(j);
					for( int indexB = 0; indexB < 4; indexB++ ) {
						Point2D_I32 bc = b.corners.get(indexB);
						
						if( UtilPoint2D_I32.distance(ac,bc) < tol ) {
							match = b;
							count++;
						}
					}
				}
				
				if( count == 1 ) {
					a.conn.add(match);
					match.conn.add(a);
				}
			}
		}
	}

	/**
	 * Counts the number of connections each node has.  If it is a legit grid
	 * then it should have a known number of nodes with a specific number
	 * of connections.
	 */
	public boolean checkGraphStructure( List<QuadBlob> blobs , List<QuadBlob> corners )
	{
		// make a histogram of connection counts
		int conn[] = new int[5];
		
		for( QuadBlob b : blobs ) {
			if( b.conn.size() == 1 )
				corners.add(b);
			
			conn[b.conn.size()]++;
		}

		if( conn[0] != 0 )
			return false;

		if( conn[1] != 4 )
			return false;

		if( conn[2] != 2*(numCols+numRows-4) )
			return false;

		if( conn[3] != 0 )
			return false;

		if( conn[4] != expectedBlobs-conn[1]-conn[2] )
			return false;

		return true;
	}
	
	public List<Point2D_I32> findBoundingQuad(  List<QuadBlob> corners ) {
		List<Point2D_I32> points = new ArrayList<Point2D_I32>();
		
		Point2D_I32 center = new Point2D_I32();
		
		// add the centers
		for( QuadBlob b : corners ) {
			points.add(b.center);
			center.x += b.center.x;
			center.y += b.center.y;
		}

		center.x /= 4;
		center.y /= 4;

		UtilCalibrationGrid.sortByAngleCCW(center, points);

		return points;
	}

	public List<Point2D_I32> getBoundingQuad() {
		return boundingQuad;
	}

	public DetectQuadBlobsBinary getDetectBlobs() {
		return detectBlobs;
	}

	public List<QuadBlob> getGraphBlobs() {
		return graphBlobs;
	}

	public List<QuadBlob> getCornerBlobs() {
		return cornerBlobs;
	}
}
