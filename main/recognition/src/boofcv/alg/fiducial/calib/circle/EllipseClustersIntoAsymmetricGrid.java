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
 * Given a cluster of ellipses (created with {@link EllipsesIntoClusters}) order the ellipses into an asymmetric
 * grid.  In an asymmetric grid each row is offset by 1/2 the horizontal spacing between.  This forms a sawtooth
 * pattern vertically.
 *
 * @author Peter Abeles
 */
public class EllipseClustersIntoAsymmetricGrid {

	private FastQueue<Grid> foundGrids = new FastQueue<>(Grid.class,true);

	public static double CONTOUR_ANGLE_MIN = Math.PI*0.8;
	public static int MAX_LINE_LENGTH = 10000;

	public static double MAX_LINE_ANGLE_CHANGE = UtilAngle.degreeToRadian(20);

	// Information on each ellipse/node in a cluster
	private FastQueue<NodeInfo> listInfo = new FastQueue<NodeInfo>(NodeInfo.class,true);
	// Used to sort edges in a node.  used instead of built in sorting algorithm to maximize memory being recycled
	private QuickSortComparator<Edge> sorter;

	// All ellipses in the contour around the grid
	private FastQueue<NodeInfo> contour = new FastQueue<NodeInfo>(NodeInfo.class,false);

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
			computeClusterInfo(ellipses, clusters.get(i));

			if( !findContour() )
				continue;

			// Find corner to start alignment
			NodeInfo corner = selectSeedCorner();

			// find the row and column which the corner is a member of
			List<NodeInfo> cornerRow = findLine(corner,corner.left);
			List<NodeInfo> cornerColumn = findLine(corner,corner.right);

			// Go down the columns and find each of the rows
			List<List<NodeInfo>> outerGrid = new ArrayList<List<NodeInfo>>();
			outerGrid.add( cornerRow );

			for (int j = 1; j < cornerColumn.size(); j++) {
				List<NodeInfo> prev = outerGrid.get( j - 1);
				NodeInfo seed = cornerColumn.get(j);
				NodeInfo next = selectSeedNext(prev.get(0),prev.get(1), seed);
				if( next == null )
					throw new RuntimeException("Outer column with a row that has only one element");
				List<NodeInfo> row = findLine( seed , next);
				outerGrid.add( row );
			}

			List<List<NodeInfo>> innerGrid = findInnerGrid(outerGrid);

			sanityCheckGrid(outerGrid,innerGrid);

			// TODO now do the same for the inner grid
		}
	}

	void sanityCheckGrid( List<List<NodeInfo>> outerGrid , List<List<NodeInfo>> innerGrid ) {
		// cobined should be the same as the original cluster's size

		// no duplicates
	}

	void combineGrids( List<List<NodeInfo>> outerGrid , List<List<NodeInfo>> innerGrid ) {
		Grid g = foundGrids.grow();


	}

	List<List<NodeInfo>> findInnerGrid( List<List<NodeInfo>> outerGrid ) {
		NodeInfo c00 = outerGrid.get(0).get(0);
		NodeInfo c01 = outerGrid.get(0).get(1);
		NodeInfo c10 = outerGrid.get(1).get(0);
		NodeInfo c11 = outerGrid.get(1).get(1);

		NodeInfo corner = selectInnerSeed( c00, c01, c10 , c11 );

		NodeInfo rowNext = selectSeedNext(c00,c01,corner);
		NodeInfo colNext = selectSeedNext(c00,c10,corner);

		List<NodeInfo> row = findLine(corner, rowNext);
		List<NodeInfo> column = findLine(corner, colNext);

		List<List<NodeInfo>> grid = new ArrayList<List<NodeInfo>>();

		if( row != null ) {
			grid.add(row);
			for (int i = 1; i < column.size(); i++) {
				List<NodeInfo> prev = grid.get(i - 1);
				NodeInfo seed = column.get(i);
				NodeInfo next = selectSeedNext(prev.get(0), prev.get(1), seed);
				row = findLine(seed, next);
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
	static protected List<NodeInfo> findLine( NodeInfo seed , NodeInfo next ) {
		double anglePrev = direction(seed, next);

		List<NodeInfo> line = new ArrayList<NodeInfo>();
		line.add( seed );

		for( int i = 0; i < MAX_LINE_LENGTH; i++) {
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

	void computeClusterInfo(List<EllipseRotated_F64> ellipses , List<Node> cluster ) {

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
		findLargestAngle();
	}

	/**
	 * Adds edges to node info and computes their orientation
	 */
	private void addEdgesToInfo(List<Node> cluster) {
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
	private void findLargestAngle() {
		for (int i = 0; i < listInfo.size(); i++) {
			NodeInfo info = listInfo.get(i);

			if( info.edges.size < 2 )
				continue;

			for (int j = 0, k = info.edges.size-1; j < info.edges.size; k=j,j++) {
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

	private boolean findContour() {
		contour.reset();

		// mark nodes as being part of the contour
		for (int i = 0; i < listInfo.size(); i++) {
			NodeInfo info = listInfo.get(i);

			if( info.angleBetween >= CONTOUR_ANGLE_MIN ) {
				info.contour = true;
				contour.add( info );
			}
		}

		if( contour.size < 4 )
			return false;

		// Quick sanity check that makes sure all nodes on the contour link to contours
		for (int i = 0; i < contour.size(); i++) {
			NodeInfo info = contour.get(i);
			if( !info.left.contour || !info.right.contour )
				return false;
		}

		int total = contour.size;
		NodeInfo current = contour.get(0);
		NodeInfo start = current;
		contour.reset();

		do {
			// there must be a cycle some place
			if( contour.size >= total )
				return false;
			contour.add( current );
			current = current.right;
		} while( start != current );

		return contour.size == total;
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
	private NodeInfo selectSeedCorner() {
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

		FastQueue<Edge> edges = new FastQueue<Edge>(Edge.class,true);

		boolean contour;
		NodeInfo left,right;
		double angleBetween;

		public void reset() {
			contour = false;
			ellipse = null;
			left = right = null;
			angleBetween = 0;
			edges.reset();
		}
	}

	public static class Edge {
		NodeInfo target;
		double angle;
	}

	public static class Grid
	{
		public List<EllipseRotated_F64> ellipses = new ArrayList<EllipseRotated_F64>();
		public int rows;
		public int columns;
	}

}
