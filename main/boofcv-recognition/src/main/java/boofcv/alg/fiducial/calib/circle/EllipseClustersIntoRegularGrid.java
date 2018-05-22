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

import georegression.metric.UtilAngle;
import georegression.struct.curve.EllipseRotated_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Given a cluster of ellipses (created with {@link EllipsesIntoClusters}) order the ellipses into an regular
 * grid.  Must be a proper grid too.  That means number of rows and columns both need to be 2 or more.</p>
 * 
 * <p>Unlike the {@link EllipseClustersIntoHexagonalGrid asymmetric} grid the return grid object will be dense
 * with every element filled as expected.</p>
 *
 * @author Peter Abeles
 */
public class EllipseClustersIntoRegularGrid extends EllipseClustersIntoGrid {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(List<EllipseRotated_F64> ellipses , List<List<EllipsesIntoClusters.Node>> clusters ) {

		foundGrids.reset();

		for (int i = 0; i < clusters.size(); i++) {
			List<EllipsesIntoClusters.Node> cluster = clusters.get(i);
			int clusterSize = cluster.size();
			if( clusterSize < 2 )
				continue;
			computeNodeInfo(ellipses, cluster);

			// finds all the nodes in the outside of the cluster
			if( !findContour(false) ) {
				if( verbose ) System.out.println("Contour find failed");
				continue;
			}

			// Find corner to start alignment
			NodeInfo corner = selectSeedCorner();
			corner.marked = true;

			boolean ccw = UtilAngle.distanceCCW(direction(corner,corner.right),direction(corner,corner.left)) > Math.PI;

			// find the row and column which the corner is a member of
			List<NodeInfo> cornerRow = findLine(corner,corner.left,clusterSize, null,ccw);
			List<NodeInfo> cornerColumn = findLine(corner,corner.right,clusterSize, null,!ccw);

			if( cornerRow == null || cornerColumn == null ) {
				if( verbose )System.out.println("Corner row/column line find failed");
				continue;
			}

			// Go down the columns and find each of the rows
			List<List<NodeInfo>> gridByRows = new ArrayList<>();
			gridByRows.add( cornerRow );

			boolean failed = false;
			for (int j = 1; j < cornerColumn.size(); j++) {
				List<NodeInfo> prev = gridByRows.get( j - 1);
				NodeInfo seed = cornerColumn.get(j);
				NodeInfo next = selectSeedNext(prev.get(0),prev.get(1), seed,ccw);
				if( next == null ) {
					if( verbose )
						System.out.println("Outer column with a row that has only one element");
					failed = true;
					break;
				}
				List<NodeInfo> row = findLine( seed , next, clusterSize, null,ccw);
				gridByRows.add( row );
			}
			if( failed )
				continue;

			// perform sanity checks
			if( !checkGridSize(gridByRows, cluster.size()) ) {
				if( verbose ) {
					System.out.println("grid size check failed");
					for (int j = 0; j < gridByRows.size(); j++) {
						System.out.println("  row "+gridByRows.get(j).size());
					}
				}
				continue;
			}

			if( checkDuplicates(gridByRows) ) {
				if( verbose ) System.out.println("contains duplicates");
				continue;
			}

			createRegularGrid(gridByRows,foundGrids.grow());
		}
	}

	/**
	 * Makes sure the found grid is the same size as the original cluster.  If it's not then.
	 * not all the nodes were used.  All lists must have he same size too.
	 */
	static boolean checkGridSize(List<List<NodeInfo>> grid ,
								 int clusterSize ) {
		int total = 0;
		int expected = grid.get(0).size();
		for (int i = 0; i < grid.size(); i++) {
			if( expected != grid.get(i).size() )
				return false;
			total += grid.get(i).size();
		}

		return total == clusterSize;
	}

	/**
	 * Combines the inner and outer grid into one grid for output.  See {@link Grid} for a discussion
	 * on how elements are ordered internally.
	 */
	static void createRegularGrid( List<List<NodeInfo>> gridByRows , Grid g) {
		g.reset();

		g.columns = gridByRows.get(0).size();
		g.rows = gridByRows.size();

		for (int row = 0; row < g.rows; row++) {
			List<NodeInfo> list = gridByRows.get(row);
			for (int i = 0; i < g.columns; i++) {
				g.ellipses.add(list.get(i).ellipse );
			}
		}
	}
}
