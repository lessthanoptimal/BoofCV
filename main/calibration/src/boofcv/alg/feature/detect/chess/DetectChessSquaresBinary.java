/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.detect.squares.*;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Polygon2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Given a binary image it detects the presence of chess board calibration grids. 1) Detect blobs
 * in binary image and select square like ones. 2) Connect blobs based distance of corners. 3)
 * prune graph. 4) sanity check graph structure. 5) Find bounding quadrilateral.
 *
 * @author Peter Abeles
 */
public class DetectChessSquaresBinary<T extends ImageSingleBand> {

	// detector for squares
	BinaryPolygonConvexDetector<T> detectorSquare;

	// Converts detected squares into a graph and into grids
	SquaresIntoClusters s2c;
	ClustersIntoGrids c2g;


	// size of square grids
	private int outerRows,outerCols;
	private int innerRows,innerCols;

	// bounding quadrilateral
	private Polygon2D_I32 boundPolygon = new Polygon2D_I32();

	SquareGridTools tools = new SquareGridTools();

	// bounding polygon of inner grid
	private Polygon2D_F64 innerPolygon = new Polygon2D_F64();

	// nodes in the outer which could be the zero node
	private List<SquareNode> outerOrigins = new ArrayList<SquareNode>();

	/**
	 * Configures chess board detector.
	 *
	 * @param numCols Number of columns in square grid
	 * @param numRows Number of rows in square grid
	 */
	public DetectChessSquaresBinary(int numCols, int numRows,
									BinaryPolygonConvexDetector<T> detectorSquare )
	{

		// number of black squares in rows/columns
		outerCols = numCols/2 + numCols%2;
		outerRows = numRows/2 + numRows%2;

		innerCols = numCols/2;
		innerRows = numRows/2;

		this.detectorSquare = detectorSquare;

		s2c = new SquaresIntoClusters(1.0);
		c2g = new ClustersIntoGrids(innerCols*innerRows);
	}

	/**
	 * Detects chessboard in the binary image.  Square corners must be disconnected.
	 * Returns true if a chessboard was found, false otherwise.
	 *
	 * @param input Original input image.
	 * @param binary Binary image of chessboard
	 * @return True if successful.
	 */
	public boolean process( T input , ImageUInt8 binary ) {
		boundPolygon.vertexes.reset();

		detectorSquare.process(input, binary);

		FastQueue<Polygon2D_F64> found = detectorSquare.getFound();

		List<List<SquareNode>> clusters = s2c.process(found.toList());
		c2g.process(clusters);
		List<SquareGrid> grids = c2g.getGrids();

		// find inner and outer grids of squares
		SquareGrid inner = null;
		SquareGrid outer = null;

		for (int i = 0; i < grids.size(); i++) {
			SquareGrid g = grids.get(i);

			if( (g.columns == innerCols && g.rows == innerRows) ||
					(g.columns == innerRows && g.rows == innerCols)) {
				if( inner != null )
					return false;
				inner = g;
			} else if( (g.columns == outerCols && g.rows == outerRows) ||
					(g.columns == outerRows && g.rows == outerCols)) {
				if( outer != null )
					return false;
				outer = g;
			}
		}

		if( inner == null || outer == null )
			return false;

		// make sure the rows/columns are correctly aligned
		if( inner.columns != innerCols ) {
			tools.transpose(inner);
		}

		if( outer.columns != outerCols ) {
			tools.transpose(outer);
		}

		tools.boundingPolygon(inner,innerPolygon);
		selectSeedZero(inner,outer,innerPolygon);

		// TODO align inner with outer now that a matching pair is known

		// TODO create list of possible zeros again

		// TODO select canonical


		return true;
	}

	SquareNode seedOuter;
	SquareNode seedInner;
	private void selectSeedZero( SquareGrid gridOuter , SquareGrid gridInner , Polygon2D_F64 bounding  ) {
		outerOrigins.clear();
		if( gridOuter.columns == gridOuter.rows ) {
			outerOrigins.add(gridOuter.get(0, 0));
			checkAdd(gridOuter.get( 0,-1),outerOrigins);
			checkAdd(gridOuter.get(-1,-1),outerOrigins);
			checkAdd(gridOuter.get(-1, 0),outerOrigins);
		} else {
			outerOrigins.add(gridOuter.get(0, 0));
			checkAdd(gridOuter.get(-1,-1),outerOrigins);
		}

		SquareNode best = null;
		int bestCorner = 0;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < outerOrigins.size(); i++) {
			SquareNode n = outerOrigins.get(i);

			double d = Double.MAX_VALUE;
			int corner = -1;
			for (int j = 0; j < 4; j++) {
				double a = n.distanceSqCorner(bounding.get(j));
				if( a < d ) {
					corner = j;
					d = a;
				}
			}
			if( d < bestDistance ) {
				best = n;
				bestCorner = corner;
				bestDistance = d;
			}
		}

		seedOuter = best;
		switch( bestCorner ) {
			case 0:seedInner = gridInner.get(0,0); break;
			case 1:seedInner = gridInner.get(0,-1); break;
			case 2:seedInner = gridInner.get(-1,-1); break;
			case 3:seedInner = gridInner.get(-1,0); break;
			default: throw new RuntimeException("Bug!");
		}
	}

	private void checkAdd( SquareNode node , List<SquareNode> list ) {
		if( !list.contains(node)) {
			list.add(node);
		}
	}

	boolean isAsymmetric() {
		return innerCols == outerCols || innerRows == outerRows;
	}

}
