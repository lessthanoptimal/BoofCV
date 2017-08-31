/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.circle;

import boofcv.alg.fiducial.calib.circle.EllipsesIntoClusters.Node;
import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Given a cluster of ellipses (created with {@link EllipsesIntoClusters}) order the ellipses into an asymmetric
 * grid.  In an asymmetric grid each row is offset by 1/2 the horizontal spacing between.  This forms a sawtooth
 * pattern vertically.</p>
 *
 *
 * <p></p>Note that the returned grid is 'sparse'.  every other node is skipped implicitly.
 * This is caused by the asymmetry.  Each row is offset by one circle/grid element.</p>
 *
 * <pre>Examples:
 * 3x6 grid will have 9 elements total.
 * grid(0,0) = [0]
 * grid(0,2) = [1]
 * grid(0,4) = [2]
 * grid(1,1) = [3]
 * grid(1,3) = [4]
 * grid(1,5) = [5]
 * </pre>
 *
 * <p>See {@link Grid} for a description of how the output grids are described.  It uses a sparse format.</p>
 * <p>See {@link DetectCircleAsymmetricGrid} for an example of an asymmetric grid</p>
 *
 * @author Peter Abeles
 */
public class EllipseClustersIntoAsymmetricGrid extends EllipseClustersIntoGrid {

	// Local storage in one of the functions below.  Here to minimize GC
	private LineSegment2D_F64 line0110 = new LineSegment2D_F64();
	private LineSegment2D_F64 line0011 = new LineSegment2D_F64();
	private Point2D_F64 intersection = new Point2D_F64();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(List<EllipseRotated_F64> ellipses , List<List<Node>> clusters ) {

		System.out.println("---------------- ENTER clusters into asym grid");
		foundGrids.reset();
		if( clusters.size() == 0 )
			return;

		verbose = true;

		for (int i = 0; i < clusters.size(); i++) {
			List<Node> cluster = clusters.get(i);
			int clusterSize = cluster.size();
			computeNodeInfo(ellipses, cluster);

			// finds all the nodes in the outside of the cluster
			if( !findContour(true) ) {
				if( verbose ) System.out.println("Contour find failed");
				continue;
			}

			// Find corner to start alignment
			NodeInfo corner = selectSeedCorner();

			// find the row and column which the corner is a member of
			List<NodeInfo> cornerRow = findLine(corner,corner.left,clusterSize, null);
			List<NodeInfo> cornerColumn = findLine(corner,corner.right,clusterSize, null);

			// Go down the columns and find each of the rows
			List<List<NodeInfo>> outerGrid = new ArrayList<>();
			outerGrid.add( cornerRow );

			boolean failed = false;
			for (int j = 1; j < cornerColumn.size(); j++) {
				List<NodeInfo> prev = outerGrid.get( j - 1);
				NodeInfo seed = cornerColumn.get(j);
				NodeInfo next = selectSeedNext(prev.get(0),prev.get(1), seed);
				if( next == null ) {
					if( verbose )
						System.out.println("Outer column with a row that has only one element");
					failed = true;
					break;
				}
				List<NodeInfo> row = findLine( seed , next, clusterSize, null);
				outerGrid.add( row );
			}
			if( failed )
				continue;

			System.out.println("Rows/Cols = "+cornerRow.size()+" / "+cornerColumn.size());

			List<List<NodeInfo>> innerGrid = findInnerGrid(outerGrid, clusterSize);

			// see if it failed to find the inner grid
			if( innerGrid == null ) {
				if( verbose ) System.out.println("Inner grid find failed");
				continue;
			}

			// perform sanity checks
			if( !checkGridSize(outerGrid,innerGrid, cluster.size()) ) {
				if( verbose ) {
					System.out.println("grid size check failed");
					for (int j = 0; j < outerGrid.size(); j++) {
						System.out.println("  outer row "+outerGrid.get(j).size());
					}
					for (int j = 0; j < innerGrid.size(); j++) {
						System.out.println("  inner row "+innerGrid.get(j).size());
					}
				}
				continue;
			}

			if( checkDuplicates(outerGrid) || checkDuplicates(innerGrid)) {
				if( verbose ) System.out.println("contains duplicates");
				continue;
			}

			// combine inner and outer grids together
			combineGrids(outerGrid,innerGrid);
		}
	}

	/**
	 * The passed in nodes should have the corner of the inner grid inside of them
	 * The intersection of the true circle's center would be the same as true
	 * corner's center, however since this is distorted it will only be approximate.
	 * So the ellipse with a center that is classes is found
	 */
	protected NodeInfo selectInnerSeed( NodeInfo c00 , NodeInfo c01 ,
										NodeInfo c10 , NodeInfo c11 ) {
		line0110.a.set(c01.ellipse.center);
		line0110.b.set(c10.ellipse.center);
		line0011.a.set(c00.ellipse.center);
		line0011.b.set(c11.ellipse.center);

		if( null == Intersection2D_F64.intersection(line0110, line0011,intersection) )
			return null;

		// pick the best solution from two perspectives.  Two perspectives are used
		// to provide additional robustness
		NodeInfo a = findClosestEdge(c00,intersection);
		NodeInfo b = findClosestEdge(c11,intersection);

		if( a == b ) {
			a.marked = true;
			return a;
		}
		return null;
	}

	/**
	 * Makes sure the found grid is the same size as the original cluster.  If it's not then.
	 * not all the nodes were used.  All lists must have he same size too.
	 */
	static boolean checkGridSize(List<List<NodeInfo>> outerGrid ,
						  List<List<NodeInfo>> innerGrid ,
						  int clusterSize ) {
		int total = 0;
		int expected = outerGrid.get(0).size();
		for (int i = 0; i < outerGrid.size(); i++) {
			if( expected != outerGrid.get(i).size() )
				return false;
			total += outerGrid.get(i).size();
		}
		expected = innerGrid.get(0).size();
		for (int i = 0; i < innerGrid.size(); i++) {
			if( expected != innerGrid.get(i).size() )
				return false;
			total += innerGrid.get(i).size();
		}

		return total == clusterSize;
	}

	/**
	 * Combines the inner and outer grid into one grid for output.  See {@link Grid} for a discussion
	 * on how elements are ordered internally.
	 */
	void combineGrids( List<List<NodeInfo>> outerGrid , List<List<NodeInfo>> innerGrid ) {
		Grid g = foundGrids.grow();
		g.reset();

		g.columns = outerGrid.get(0).size() + innerGrid.get(0).size();
		g.rows = outerGrid.size() + innerGrid.size();

		for (int row = 0; row < g.rows; row++) {
			List<NodeInfo> list;
			if( row%2 == 0 ) {
				list = outerGrid.get(row/2);
			} else {
				list = innerGrid.get(row/2);
			}
			for (int i = 0; i < g.columns; i++) {
				if( (i%2) == (row%2))
					g.ellipses.add(list.get(i/2).ellipse );
				else
					g.ellipses.add(null);
			}
		}
	}

	/**
	 * The outside grid has been found now the inner grid needs to be found.  The inner grid is offset
	 * by 1/2 the spacing from the outer grid.
	 *
	 * @param outerGrid The outer grid which was already found
	 * @param clusterSize Number of elements in the cluster.  used to catch bad code instead of looping forever
	 * @return The inner grid
	 */
	List<List<NodeInfo>> findInnerGrid( List<List<NodeInfo>> outerGrid , int clusterSize) {
		NodeInfo c00 = outerGrid.get(0).get(0);
		NodeInfo c01 = outerGrid.get(0).get(1);
		NodeInfo c10 = outerGrid.get(1).get(0);
		NodeInfo c11 = outerGrid.get(1).get(1);

		NodeInfo corner = selectInnerSeed( c00, c01, c10 , c11 );
		if( corner == null ) {
			if( verbose )
				System.out.println("Can't select inner grid seed");
			return null;
		}
		corner.marked = true;

		NodeInfo rowNext = selectSeedNext(c00,c01,corner);
		NodeInfo colNext = selectSeedNext(c00,c10,corner);

		List<NodeInfo> row = findLine(corner, rowNext, clusterSize, null);
		List<NodeInfo> column = findLine(corner, colNext, clusterSize, null);

		List<List<NodeInfo>> grid = new ArrayList<>();

		if( row != null && column != null ) {
			grid.add(row);
			for (int i = 1; i < column.size(); i++) {
				List<NodeInfo> prev = grid.get(i - 1);
				NodeInfo seed = column.get(i);
				NodeInfo next = selectSeedNext(prev.get(0), prev.get(1), seed);
				row = findLine(seed, next, clusterSize, null);
				if (row == null) {
					if( verbose )
						System.out.println("Inner grid missing a row");
					return null;
				}
				grid.add(row);
			}
		} else if( row != null ) {
			// Inner grid is composed of only a row
			grid.add(row);
		} else if( column != null ) {
			// Inner grid is composed of only a single column
			for (int i = 0; i < column.size(); i++) {
				List<NodeInfo> l = new ArrayList<>(); // TODO use recycled memory here
				l.add( column.get(i) );
				grid.add( l );
			}
		} else {
			row = new ArrayList<>();
			row.add(corner);
			grid.add( row );
		}
		return grid;
	}
}
