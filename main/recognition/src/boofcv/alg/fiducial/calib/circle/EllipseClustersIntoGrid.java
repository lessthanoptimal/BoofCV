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
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.sorting.QuickSortComparator;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static boofcv.alg.fiducial.calib.circle.DetectAsymmetricCircleGrid.totalEllipses;

/**
 * <p>Base class for ordering clusters of ellipses into grids</p>
 *
 * <p>See {@link Grid} for a description of how the output grids are described.  It uses a sparse format.</p>
 * <p>See {@link DetectAsymmetricCircleGrid} for an example of an asymmetric grid</p>
 *
 * @author Peter Abeles
 */
public class EllipseClustersIntoGrid {

	protected FastQueue<Grid> foundGrids = new FastQueue<>(Grid.class,true);

	// When finding lines this is the largest change in angle between the two edges allowed for it to be on the line
	protected static double MAX_LINE_ANGLE_CHANGE = UtilAngle.degreeToRadian(10);

	// Information on each ellipse/node in a cluster
	protected FastQueue<NodeInfo> listInfo = new FastQueue<>(NodeInfo.class,true);
	// Used to sort edges in a node.  used instead of built in sorting algorithm to maximize memory being recycled
	protected QuickSortComparator<Edge> sorter;

	// All ellipses in the contour around the grid
	protected FastQueue<NodeInfo> contour = new FastQueue<>(NodeInfo.class,false);

	protected boolean verbose = false;

	public EllipseClustersIntoGrid() {

		sorter = new QuickSortComparator<>(new Comparator<Edge>() {

			@Override
			public int compare(Edge o1, Edge o2) {
				if (o1.angle < o2.angle)
					return -1;
				else if (o1.angle > o2.angle)
					return 1;
				else
					return 0;
			}
		});
	}

	/**
	 * Finds all the nodes which form an approximate line
	 * @param seed First ellipse
	 * @param next Second ellipse, specified direction of line relative to seed
	 * @return All the nodes along the line
	 */
	protected static List<NodeInfo> findLine(NodeInfo seed , NodeInfo next , int clusterSize ) {
		if( next == null )
			return null;

		double anglePrev = direction(seed, next);

		List<NodeInfo> line = new ArrayList<>(); // TODO recycle this
		line.add( seed );
		line.add( next );

		for( int i = 0; i < clusterSize+1; i++) {
			// find the child of next which is within tolerance and closest to it
			double bestScore = Double.MAX_VALUE;
			double bestDistance = Double.MAX_VALUE;
			double bestAngle = Double.NaN;
			double closestDistance = Double.MAX_VALUE;
			NodeInfo best = null;

			for (int j = 0; j < next.edges.size(); j++) {
				double angle = next.edges.get(j).angle;
				NodeInfo c = next.edges.get(j).target;

				double diff = UtilAngle.dist(angle,anglePrev);
				if( diff <= MAX_LINE_ANGLE_CHANGE ) {
					double d = c.ellipse.center.distance(next.ellipse.center);
					double score = (diff+0.01)*d;
					if( score < bestScore ) {
						bestDistance = d;
						bestScore = score;
						bestAngle = angle;
						best = c;
					}
					closestDistance = Math.min(d,closestDistance);
				}
			}

			if( best == null || bestDistance > closestDistance*2.0)
				return line;
			else {
				line.add(best);
				anglePrev = bestAngle;
				next = best;
			}
		}
		throw new RuntimeException("Stuck in a loop?  Maximum line length exceeded");
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
		double angleTarget = direction(prevSeed, prevNext);

		double bestScore = Double.MAX_VALUE;
		NodeInfo best = null;

		// cut down on verbosity by saving the reference here
		Point2D_F64 c = currentSeed.ellipse.center;

		for (int i = 0; i < currentSeed.edges.size(); i++) {
			Edge edge = currentSeed.edges.get(i);

			double angleDiff = UtilAngle.dist(edge.angle, angleTarget);
			if( angleDiff > MAX_LINE_ANGLE_CHANGE*1.5 )
				continue;

			double score = (angleDiff+0.001)*c.distance(edge.target.ellipse.center);

			if( score < bestScore ) {
				bestScore = score;
				best = edge.target;
			}
		}

		return best;
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
	 * Checks to see if any node is used more than once
	 */
	boolean checkDuplicates(List<List<NodeInfo>> grid ) {

		for (int i = 0; i < grid.size(); i++) {
			List<NodeInfo> list = grid.get(i);
			for (int j = 0; j < list.size(); j++) {
				NodeInfo n = list.get(j);
				if( n.marked )
					return true;
				n.marked = true;
			}
		}
		return false;
	}



	static double direction(NodeInfo seed, NodeInfo next) {
		return Math.atan2( next.ellipse.center.y - seed.ellipse.center.y ,
				next.ellipse.center.x - seed.ellipse.center.x );
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
		pruneNearlyIdenticalAngles();
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
	 * If there is a nearly perfect line a node farther down the line can come before.  This just selects the closest
	 */
	void pruneNearlyIdenticalAngles() {
		for (int i = 0; i < listInfo.size(); i++) {
			NodeInfo infoN = listInfo.get(i);

			for (int j = 0; j < infoN.edges.size(); ) {
				int k = (j+1)%infoN.edges.size;

				double angularDiff = UtilAngle.dist(infoN.edges.get(j).angle,infoN.edges.get(k).angle);
				if( angularDiff < UtilAngle.radian(5)) {
					NodeInfo infoJ = infoN.edges.get(j).target;
					NodeInfo infoK = infoN.edges.get(k).target;

					double distJ = infoN.ellipse.center.distance(infoJ.ellipse.center);
					double distK = infoN.ellipse.center.distance(infoK.ellipse.center);

					if( distJ < distK ) {
						infoN.edges.remove(k);
					} else {
						infoN.edges.remove(j);
					}

				} else {
					j++;
				}
			}
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
		NodeInfo prev = seed;
		NodeInfo current = seed.right;
		while( current != null && current != seed && contour.size() < listInfo.size() ) {
			if( prev != current.left )
				return false;
			contour.add( current );
			current.contour = true;
			prev = current;
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
	 * Selects the node on the contour which is closest to 270 degrees and is thus likely to be
	 * a node on the corner
	 */
	NodeInfo selectSeedCorner() {
		NodeInfo best = null;
		double bestError = Double.MAX_VALUE;

		for (int i = 0; i < contour.size; i++) {
			NodeInfo info = contour.get(i);

			double error = UtilAngle.dist(3*Math.PI/2.0,info.angleBetween);

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
		FastQueue<Edge> edges = new FastQueue<>(Edge.class, true);

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

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * Specifies the grid.  See implementation class for grid details.
	 */
	public static class Grid
	{
		public List<EllipseRotated_F64> ellipses = new ArrayList<>();
		public int rows;
		public int columns;

		public void reset() {
			rows = columns = -1;
			ellipses.clear();
		}

		public EllipseRotated_F64 get( int row , int col ) {
			return ellipses.get(row*columns + col);
		}

		public int idx( int row , int col ) {
			return row*columns + col;
		}

		public void setShape( int rows , int columns ) {
			this.rows = rows;
			this.columns = columns;
		}

		public int getNumberOfEllipses() {
			return totalEllipses(rows,columns);
		}

		public int getIndexOfEllipse( int row , int col ) {
			int index = 0;
			index += (row/2)*this.columns + (row%2)*(this.columns/2+this.columns%2);
			return index + col/2;
		}
	}

}
