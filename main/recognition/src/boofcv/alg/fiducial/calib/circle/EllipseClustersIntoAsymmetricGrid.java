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

package boofcv.alg.fiducial.calib.circle;

import boofcv.alg.fiducial.calib.circle.EllipsesIntoClusters.Node;
import georegression.metric.Intersection2D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.sorting.QuickSortComparator;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * <p>Given a cluster of ellipses (created with {@link EllipsesIntoClusters}) order the ellipses into an asymmetric
 * grid.  In an asymmetric grid each row is offset by 1/2 the horizontal spacing between.  This forms a sawtooth
 * pattern vertically.</p>
 *
 * <p>See {@link Grid} for a description of how the output grids are described.  It uses a sparse format.</p>
 *
 * @author Peter Abeles
 */
public class EllipseClustersIntoAsymmetricGrid {

	private FastQueue<Grid> foundGrids = new FastQueue<>(Grid.class,true);

	// When finding lines this is the largest change in angle between the two edges allowed for it to be on the line
	public static double MAX_LINE_ANGLE_CHANGE = UtilAngle.degreeToRadian(20);

	// Information on each ellipse/node in a cluster
	FastQueue<NodeInfo> listInfo = new FastQueue<>(NodeInfo.class,true);
	// Used to sort edges in a node.  used instead of built in sorting algorithm to maximize memory being recycled
	QuickSortComparator<Edge> sorter;

	// All ellipses in the contour around the grid
	FastQueue<NodeInfo> contour = new FastQueue<>(NodeInfo.class,false);

	// Local storage in one of the functions below.  Here to minimize GC
	LineSegment2D_F64 line0111 = new LineSegment2D_F64();
	LineSegment2D_F64 line1001 = new LineSegment2D_F64();
	Point2D_F64 intersection = new Point2D_F64();

	public EllipseClustersIntoAsymmetricGrid() {

		sorter = new QuickSortComparator<Edge>(new Comparator<Edge>(){

			@Override
			public int compare(Edge o1, Edge o2) {
				if( o1.angle < o2.angle )
					return -1;
				else if( o1.angle > o2.angle )
					return 1;
				else
					return 0;
			}
		});
	}

	/**
	 * Computes grids from the clusters.  Call {@link #getGrids()} to retrieve the results.
	 *
	 * @param ellipses (input) List of all the ellipses
	 * @param clusters (Input) Description of all the clusters
	 */
	public void process(List<EllipseRotated_F64> ellipses , List<List<Node>> clusters ) {

		foundGrids.reset();

		for (int i = 0; i < clusters.size(); i++) {
			List<Node> cluster = clusters.get(i);
			int clusterSize = cluster.size();
			computeNodeInfo(ellipses, cluster);

			// finds all the nodes in the outside of the cluster
			if( !findContour() )
				continue;

			// Find corner to start alignment
			NodeInfo corner = selectSeedCorner();

			// find the row and column which the corner is a member of
			List<NodeInfo> cornerRow = findLine(corner,corner.left,clusterSize);
			List<NodeInfo> cornerColumn = findLine(corner,corner.right,clusterSize);

			// Go down the columns and find each of the rows
			List<List<NodeInfo>> outerGrid = new ArrayList<List<NodeInfo>>();
			outerGrid.add( cornerRow );

			for (int j = 1; j < cornerColumn.size(); j++) {
				List<NodeInfo> prev = outerGrid.get( j - 1);
				NodeInfo seed = cornerColumn.get(j);
				NodeInfo next = selectSeedNext(prev.get(0),prev.get(1), seed);
				if( next == null )
					throw new RuntimeException("Outer column with a row that has only one element");
				List<NodeInfo> row = findLine( seed , next, clusterSize);
				outerGrid.add( row );
			}

			List<List<NodeInfo>> innerGrid = findInnerGrid(outerGrid, clusterSize);

			// perform sanity checks
			if( !checkGridSize(outerGrid,innerGrid, cluster.size()) ) {
				continue;
			}

			if( !checkDuplicates(outerGrid) || !checkDuplicates(innerGrid)) {
				continue;
			}

			// combine inner and outer grids together
			combineGrids(outerGrid,innerGrid);
		}
	}

	/**
	 * Makes sure the found grid is the same size as the original cluster.  If it's not then.
	 * not all the nodes were used.  All lists must have he same size too.
	 */
	boolean checkGridSize(List<List<NodeInfo>> outerGrid ,
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
			if( expected != outerGrid.get(i).size() )
				return false;
			total += innerGrid.get(i).size();
		}

		return total == clusterSize;
	}

	/**
	 * Checks to see if any node is used more than once
	 */
	boolean checkDuplicates(List<List<NodeInfo>> grid ) {

		for (int i = 0; i < grid.size(); i++) {
			List<NodeInfo> list = grid.get(i);
			for (int j = 0; j < list.size(); j++) {
				NodeInfo n = list.get(j);
				if( n.marked )
					return false;
				n.marked = true;
			}
		}
		return true;
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
			for (int i = 0; i < list.size(); i++) {
				g.ellipses.add( list.get(i).ellipse );
			}
		}
	}

	List<List<NodeInfo>> findInnerGrid( List<List<NodeInfo>> outerGrid , int clusterSize) {
		NodeInfo c00 = outerGrid.get(0).get(0);
		NodeInfo c01 = outerGrid.get(0).get(1);
		NodeInfo c10 = outerGrid.get(1).get(0);
		NodeInfo c11 = outerGrid.get(1).get(1);

		NodeInfo corner = selectInnerSeed( c00, c01, c10 , c11 );

		NodeInfo rowNext = selectSeedNext(c00,c01,corner);
		NodeInfo colNext = selectSeedNext(c00,c10,corner);

		List<NodeInfo> row = findLine(corner, rowNext, clusterSize);
		List<NodeInfo> column = findLine(corner, colNext, clusterSize);

		List<List<NodeInfo>> grid = new ArrayList<List<NodeInfo>>();

		if( row != null ) {
			grid.add(row);
			for (int i = 1; i < column.size(); i++) {
				List<NodeInfo> prev = grid.get(i - 1);
				NodeInfo seed = column.get(i);
				NodeInfo next = selectSeedNext(prev.get(0), prev.get(1), seed);
				row = findLine(seed, next, clusterSize);
				if (row == null)
					throw new RuntimeException("Inner grid missing a row");
				grid.add(row);
			}
		} else {
			// Inner grid is composed of only a single column
			for (int i = 0; i < column.size(); i++) {
				List<NodeInfo> l = new ArrayList<NodeInfo>();
				l.add( column.get(i) );
				grid.add( l );
			}
		}
		return grid;
	}

	/**
	 * Select the first node (currentSeed) in the next row it finds the next element in the next row by
	 * looking at the first and second elements in the previous row.  It selects the edge in
	 * currentSeed which cones closest to matching the angle of 'prevSeed' and 'prevNext'
	 * @param prevSeed First node in the previous row
	 * @param prevNext Second node in the previous row
	 * @param currentSeed First node in the current row
	 * @return The found node or null if one was not found
	 */
	static protected NodeInfo selectSeedNext(  NodeInfo prevSeed , NodeInfo prevNext ,
											   NodeInfo currentSeed) {
		double anglePrev = direction(prevSeed, prevNext);

		double bestDistance = Double.MAX_VALUE;
		NodeInfo best = null;

		// cut down on verbosity by saving the reference here
		Point2D_F64 c = currentSeed.ellipse.center;

		for (int i = 0; i < currentSeed.edges.size(); i++) {
			Edge edge = currentSeed.edges.get(i);

			if( UtilAngle.dist(edge.angle, anglePrev) <= MAX_LINE_ANGLE_CHANGE ) {
				double d = edge.target.ellipse.center.distance2(c);

				if( d < bestDistance ) {
					bestDistance = d;
					best = edge.target;
				}
			}
		}

		return best;
	}

	/**
	 * The passed in nodes should have the corner of the inner grid inside of them
	 * The intersection of the true circle's center would be the same as true
	 * corner's center, however since this is distorted it will only be approximate.
	 * So the ellipse with a center that is classes is found
	 */
	protected NodeInfo selectInnerSeed( NodeInfo c00 , NodeInfo c01 ,
										NodeInfo c10 , NodeInfo c11 ) {
		line0111.a.set(c00.ellipse.center);
		line0111.b.set(c11.ellipse.center);
		line1001.a.set(c10.ellipse.center);
		line1001.b.set(c01.ellipse.center);

		if( null == Intersection2D_F64.intersection(line0111,line1001,intersection) )
			return null;

		// pick the best solution from two perspectives.  Two perspectives are used
		// to provide additional robustness
		NodeInfo a = findClosestEdge(c00,intersection);
		NodeInfo b = findClosestEdge(c11,intersection);

		if( a == b )
			return a;
		return null;
	}

	/**
	 * Finds the node which is an edge of 'n' that is closest to point 'p'
	 */
	protected static NodeInfo findClosestEdge( NodeInfo n , Point2D_F64 p ) {
		double bestDistance = Double.MAX_VALUE;
		NodeInfo best = null;

		for (int i = 0; i < n.edges.size(); i++) {
			Edge e = n.edges.get(i);
			double d = e.target.ellipse.center.distance2(p);
			if( d < bestDistance ) {
				bestDistance = d;
				best = e.target;
			}
		}

		return best;
	}

	/**
	 * Finds all the nodes which form an approximate line
	 * @param seed First ellipse
	 * @param next Second ellipse, specified direction of line relative to seed
	 * @return All the nodes along the line
	 */
	static protected List<NodeInfo> findLine( NodeInfo seed , NodeInfo next , int clusterSize ) {
		double anglePrev = direction(seed, next);

		List<NodeInfo> line = new ArrayList<NodeInfo>();
		line.add( seed );

		for( int i = 0; i < clusterSize+1; i++) {
			// find the child of next which is within tolerance and closest to it
			double bestDistance = Double.MAX_VALUE;
			double bestAngle = Double.NaN;
			NodeInfo best = null;

			for (int j = 0; j < next.edges.size(); j++) {
				double angle = next.edges.get(j).angle;
				NodeInfo c = next.edges.get(j).target;

				double diff = UtilAngle.dist(angle,anglePrev);
				if( diff <= MAX_LINE_ANGLE_CHANGE ) {
					double d = c.ellipse.center.distance2(next.ellipse.center);
					if( d < bestDistance ) {
						bestDistance = d;
						bestAngle = angle;
						best = c;
					}
				}
			}

			if( best == null )
				return line;
			else {
				line.add(best);
				anglePrev = bestAngle;
				next = best;
			}
		}
		throw new RuntimeException("Stuck in a loop?  Maximum line length exceeded");
	}

	private static double direction(NodeInfo seed, NodeInfo next) {
		return Math.atan2( seed.ellipse.center.y - next.ellipse.center.y ,
				seed.ellipse.center.x - next.ellipse.center.x );
	}

	/**
	 * For each cluster create a {@link NodeInfo} and compute different properties
	 */
	void computeNodeInfo(List<EllipseRotated_F64> ellipses , List<Node> cluster ) {

		// create an info object for each member inside of the cluster
		listInfo.reset();
		for (int i = 0; i < cluster.size(); i++) {
			Node n = cluster.get(i);
			EllipseRotated_F64 t = ellipses.get( n.which );

			NodeInfo info = listInfo.grow();
			info.reset();
			info.ellipse = t;
		}

		addEdgesToInfo(cluster);
		findLargestAnglesForAllNodes();
	}

	/**
	 * Adds edges to node info and computes their orientation
	 */
	void addEdgesToInfo(List<Node> cluster) {
		for (int i = 0; i < cluster.size(); i++) {
			Node n = cluster.get(i);
			NodeInfo infoA = listInfo.get(i);
			EllipseRotated_F64 a = infoA.ellipse;

			// create the edges and order them based on their direction
			for (int j = 0; j < n.connections.size(); j++) {
				NodeInfo infoB = listInfo.get( indexOf(cluster, n.connections.get(j)));
				EllipseRotated_F64 b = infoB.ellipse;

				Edge edge = infoA.edges.grow();

				edge.target = infoB;
				edge.angle = Math.atan2( b.center.y - a.center.y , b.center.x - a.center.x );
			}

			sorter.sort(infoA.edges.data, infoA.edges.size);
		}
	}

	/**
	 * Finds the two edges with the greatest angular distance between them.
	 */
	void findLargestAnglesForAllNodes() {
		for (int i = 0; i < listInfo.size(); i++) {
			NodeInfo info = listInfo.get(i);

			if( info.edges.size < 2 )
				continue;

			for (int k = 0, j = info.edges.size-1; k < info.edges.size; j=k,k++) {
				double angleA = info.edges.get(j).angle;
				double angleB = info.edges.get(k).angle;

				double distance = UtilAngle.distanceCCW(angleA,angleB);

				if( distance > info.angleBetween ) {
					info.angleBetween = distance;
					info.left = info.edges.get(j).target;
					info.right = info.edges.get(k).target;
				}
			}
		}
	}

	/**
	 * Finds nodes in the outside of the grid.  First the node in the grid with the largest 'angleBetween'
	 * is selected as a seed.  It is assumed at this node must be on the contour.  Then the graph is traversed
	 * in CCW direction until a loop is formed.
	 *
	 * @return true if valid and false if invalid
	 */
	boolean findContour() {
		// find the node with the largest angleBetween
		NodeInfo seed = listInfo.get(0);
		for (int i = 1; i < listInfo.size(); i++) {
			NodeInfo info = listInfo.get(i);

			if( info.angleBetween > seed.angleBetween ) {
				seed = info;
			}
		}

		// trace around the contour
		contour.reset();
		contour.add( seed );
		seed.contour = true;
		NodeInfo current = seed.right;
		while( current != seed && contour.size() < listInfo.size() ) {
			contour.add( current );
			seed.contour = true;
			current = current.right;
		}

		// fail if it is too small or was cycling
		return !(contour.size < 4 || contour.size >= listInfo.size());
	}

	/**
	 * Finds the node with the index of 'value'
	 */
	public static int indexOf(List<Node> list , int value ) {

		for (int i = 0; i < list.size(); i++) {
			if( list.get(i).which == value )
				return i;
		}

		return -1;
	}

	/**
	 * Selects the node on the contour which is closest to 90 degrees and is thus likely to be
	 * a node on the corner
	 */
	NodeInfo selectSeedCorner() {
		NodeInfo best = null;
		double bestError = Double.MAX_VALUE;

		for (int i = 0; i < contour.size; i++) {
			NodeInfo info = contour.get(i);

			double error = UtilAngle.dist(Math.PI/2.0,info.angleBetween);

			if( error < bestError ) {
				bestError = error;
				best = info;
			}
		}

		return best;
	}

	/**
	 * Returns the set of grids which were found
	 * @return found grids
	 */
	public FastQueue<Grid> getGrids() {
		return foundGrids;
	}

	public static class NodeInfo {
		EllipseRotated_F64 ellipse;

		// List of all the ellipses connected to this one in CCW order
		FastQueue<Edge> edges = new FastQueue<Edge>(Edge.class,true);

		// flag used to indicate if a node is along the shape's contour
		boolean contour;
		// the largest angle between two nodes is angleBetween and
		// left is before right in CCW direction
		NodeInfo left,right;
		double angleBetween;

		// used to indicate if it has been inspected already
		boolean marked;

		public void reset() {
			contour = false;
			ellipse = null;
			left = right = null;
			angleBetween = 0;
			marked = false;
			edges.reset();
		}
	}

	public static class Edge {
		NodeInfo target;
		double angle;
	}

	/**
	 * Specifies the grid.  Note that the grid is 'sparse'.  every other node is skipped implicitly.
	 * This is caused by the asymmetry.  Each row is offset by one circle/grid element.
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
	 */
	public static class Grid
	{
		public List<EllipseRotated_F64> ellipses = new ArrayList<EllipseRotated_F64>();
		public int rows;
		public int columns;

		public void reset() {
			rows = columns = -1;
			ellipses.clear();
		}
	}

}
