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

package boofcv.alg.fiducial.calib.squares;

import boofcv.misc.CircularIndex;
import georegression.geometry.UtilLine2D_F64;
import georegression.metric.Distance2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.RecycleManager;

import java.util.List;

/**
 * Processes the detected squares in the image and connects them into clusters.  Squares can be connected to each
 * other if two equivalent sides are parallel and their distance apart is "reasonable".  The parallel requirement
 * take advantage of line under perspective distortion remaining parallel.
 *
 * @author Peter Abeles
 */
public class SquaresIntoRegularClusters extends SquaresIntoClusters {

	// maximum neighbors on nearest-neighbor search
	public int maxNeighbors;

	// tolerance for fractional distance away a point can be from a line to be considered on the line
	double distanceTol = 0.2;

	// maximum distance two squares can be from each other relative to the size of a square
	double maxNeighborDistanceRatio;

	// ratio of the length of a square to the distance separating the square
	private double spaceToSquareRatio;

	protected RecycleManager<SquareEdge> edges = new RecycleManager<>(SquareEdge.class);

	// Storage for line segments used to calculate center
	private LineGeneral2D_F64 line = new LineGeneral2D_F64();

	private Point2D_F64 intersection = new Point2D_F64();

	// used to search for neighbors that which are candidates for connecting
	private NearestNeighbor<SquareNode> search = FactoryNearestNeighbor.kdtree();
	private FastQueue<double[]> searchPoints;
	private FastQueue<NnData<SquareNode>> searchResults = new FastQueue(NnData.class,true);


	/**
	 * Declares data structures and configures algorithm
	 * @param spaceToSquareRatio Ratio of space between squares to square lengths
	 * @param maxNeighbors The maximum number of neighbors it will look at when connecting a node
	 * @param maxNeighborDistanceRatio Maximum distance away a neighbor can be from a square to be connected.  Relative
	 *                                 to the size of the square.  Try 1.35
	 */
	public SquaresIntoRegularClusters(double spaceToSquareRatio, int maxNeighbors, double maxNeighborDistanceRatio) {
		this.spaceToSquareRatio = spaceToSquareRatio;
		this.maxNeighbors = maxNeighbors;
		//  avoid a roll over later on in the code
		if( this.maxNeighbors == Integer.MAX_VALUE ) {
			this.maxNeighbors = Integer.MAX_VALUE-1;
		}
		this.maxNeighborDistanceRatio = maxNeighborDistanceRatio;

		searchPoints = new FastQueue<double[]>(double[].class,true) {
			@Override
			protected double[] createInstance() {
				return new double[2];
			}
		};

		search.init(2);
	}

	/**
	 * Processes the unordered set of squares and creates a graph out of them using prior knowledge and geometric
	 * constraints.
	 * @param squares Set of squares
	 * @return List of graphs.  All data structures are recycled on the next call to process().
	 */
	public List<List<SquareNode>> process( List<Polygon2D_F64> squares ) {
		recycleData();

		// set up nodes
		computeNodeInfo(squares);

		// Connect nodes to each other
		connectNodes();

		// Find all valid graphs
		findClusters();
		return clusters.toList();
	}

	void computeNodeInfo( List<Polygon2D_F64> squares ) {

		for (int i = 0; i < squares.size(); i++) {
			SquareNode n = nodes.grow();
			n.reset();
			n.corners = squares.get(i);

			if( n.corners.size() != 4 )
				throw new RuntimeException("Sqaures have four corners not "+n.corners.size());

			// does not assume CW or CCW ordering just that it is ordered
			lineA.a = n.corners.get(0);
			lineA.b = n.corners.get(2);
			lineB.a = n.corners.get(1);
			lineB.b = n.corners.get(3);

			// this will be the geometric center and invariant of perspective distortion
			Intersection2D_F64.intersection(lineA, lineB, n.center);


			for (int j = 0; j < 4; j++) {
				int k = (j+1)%4;
				double l = n.corners.get(j).distance(n.corners.get(k));
				n.sideLengths[j] = l;
				n.largestSide = Math.max(n.largestSide,l);
			}
		}
	}

	/**
	 * Goes through each node and uses a nearest-neighbor search to find the closest nodes in its local neighborhood.
	 * It then checks those to see if it should connect
	 */
	void connectNodes() {
		setupSearch();

		for (int i = 0; i < nodes.size(); i++) {
			SquareNode n = nodes.get(i);

			double[] point = searchPoints.get(i);

			// distance between center when viewed head on will be space + 0.5*2*width.
			// when you factor in foreshortening this search will not be symmetric
			// the smaller will miss its larger neighbor but the larger one will find the smaller one.
			double neighborDistance = n.largestSide*(1.0+spaceToSquareRatio)*maxNeighborDistanceRatio;

			// find it's neighbors
			searchResults.reset();
			search.findNearest(point, neighborDistance*neighborDistance, maxNeighbors + 1, searchResults);

			// try to attach it's closest neighbors
			for (int j = 0; j < searchResults.size(); j++) {
				NnData<SquareNode> neighbor = searchResults.get(j);
				if( neighbor.data != n )
					considerConnect(n, neighbor.data);
			}
		}
	}

	/**
	 * Sets up data structures for nearest-neighbor search used in {@link #connectNodes()}
	 */
	private void setupSearch() {
		searchPoints.reset();
		for (int i = 0; i < nodes.size(); i++) {
			SquareNode n = nodes.get(i);

			double[] point = searchPoints.grow();
			point[0] = n.center.x;
			point[1] = n.center.y;
		}
		search.setPoints(searchPoints.toList(), nodes.toList());
	}

	/**
	 * Connects the 'candidate' node to node 'n' if they meet several criteria.  See code for details.
	 */
	void considerConnect(SquareNode node0, SquareNode node1) {
		// Find the side on each line which intersects the line connecting the two centers
		lineA.a = node0.center;
		lineA.b = node1.center;

		int intersection0 = findSideIntersect(node0,lineA,lineB);
		int intersection1 = findSideIntersect(node1,lineA,lineB);

		if( intersection1 < 0 || intersection0 < 0 ) {
			return;
		}

		// see if they have a similar shape
		double sideSideRatio0 = node0.largestSide/node0.smallestSideLength();
		double sideSideRatio1 = node1.largestSide/node1.smallestSideLength();

		if( Math.abs(sideSideRatio0-sideSideRatio1) > 1.2 ) {
			return;
		}

		// compare the size of the two closest sides.  They should be similarish
		double closeSide0 = node0.sideLengths[intersection0];
		double closeSide1 = node1.sideLengths[intersection1];
		double ratio = closeSide0>closeSide1  ? closeSide1/closeSide0 : closeSide0/closeSide1;
		if( ratio < 0.5 ) {
			return;
		}

		double distanceApart = lineA.getLength();

		// Checks to see if the two sides selected above are closest to being parallel to each other.
		// Perspective distortion will make the lines not parallel, but will still have a smaller
		// acute angle than the adjacent sides
		if( !mostParallel(node0, intersection0, node1, intersection1)) {
			return;
		}

		// The following two tests see if the end points which define the two selected sides are close to
		// the line created by the end points which define the opposing side.
		// Another way of saying this, for the "top" corner on the side, is it close to the line defined
		// by the side "top" sides on both squares.
		// just look at the code its easier than understanding that description
		if( !areMiddlePointsClose(node0.corners.get(add(intersection0, -1)), node0.corners.get(intersection0),
				node1.corners.get(add(intersection1, 1)), node1.corners.get(add(intersection1, 2)))) {
			return;
		}

		if( !areMiddlePointsClose(node0.corners.get(add(intersection0,2)),node0.corners.get(add(intersection0,1)),
				node1.corners.get(intersection1),node1.corners.get(add(intersection1,-1)))) {
			return;
		}
		checkConnect(node0,intersection0,node1,intersection1,distanceApart);

	}

	/**
	 * Finds the side which intersects the line on the shape.  The line is assumed to pass through the shape
	 * so if there is no intersection it is considered a bug
	 */
	int findSideIntersect( SquareNode n , LineSegment2D_F64 line , LineSegment2D_F64 storage ) {
		for (int i = 0; i < 4; i++) {
			int j = (i+1)%4;

			storage.a = n.corners.get(i);
			storage.b = n.corners.get(j);

			if( Intersection2D_F64.intersection(line,storage,intersection) != null ) {
				return i;
			}
		}

		// bug but I won't throw an exception to stop it from blowing up a bunch
		return -1;
	}

	/**
	 * Returns true if the two sides are the two sides on each shape which are closest to being parallel
	 * to each other.  Only the two sides which are adjacent are considered
	 */
	boolean mostParallel( SquareNode a , int sideA , SquareNode b , int sideB ) {
		double selected = acuteAngle(a,sideA,b,sideB);

		if( selected >  acuteAngle(a,sideA,b,add(sideB,1)) || selected >  acuteAngle(a,sideA,b,add(sideB,-1)) )
			return false;

		if( selected >  acuteAngle(a,add(sideA,1),b,sideB) || selected >  acuteAngle(a,add(sideA,-1),b,sideB) )
			return false;

		return true;
	}

	/**
	 * Returns an angle between 0 and PI/4 which describes the difference in slope
	 * between the two sides
	 */
	Vector2D_F64 vector0 = new Vector2D_F64();
	Vector2D_F64 vector1 = new Vector2D_F64();
	double acuteAngle(  SquareNode a , int sideA , SquareNode b , int sideB ) {
		Point2D_F64 a0 = a.corners.get(sideA);
		Point2D_F64 a1 = a.corners.get(add(sideA, 1));

		Point2D_F64 b0 = b.corners.get(sideB);
		Point2D_F64 b1 = b.corners.get(add(sideB, 1));

		vector0.set(a1.x - a0.x, a1.y - a0.y);
		vector1.set(b1.x - b0.x, b1.y - b0.y);

		double acute = vector0.acute(vector1);
		return Math.min(UtilAngle.dist(Math.PI, acute), acute);
	}

	/**
	 * Returns true if point p1 and p2 are close to the line defined by points p0 and p3.
	 */
	boolean areMiddlePointsClose( Point2D_F64 p0 , Point2D_F64 p1 , Point2D_F64 p2 , Point2D_F64 p3 ) {
		UtilLine2D_F64.convert(p0,p3,line);

		// (computed expected length of a square) * (fractional tolerance)
		double tol1 = p0.distance(p1)*distanceTol;

		// see if inner points are close to the line
		if(Distance2D_F64.distance(line, p1) > tol1 )
			return false;

		double tol2 = p2.distance(p3)*distanceTol;

		if( Distance2D_F64.distance(lineB, p2) > tol2 )
			return false;

		//------------ Now see if the line defined by one side of a square is close to the closest point on the same
		//             side on the other square
		UtilLine2D_F64.convert(p0,p1,line);
		if(Distance2D_F64.distance(line, p2) > tol2 )
			return false;

		UtilLine2D_F64.convert(p3,p2,line);
		if(Distance2D_F64.distance(line, p1) > tol1 )
			return false;

		return true;
	}


	/**
	 * Checks to see if the two nodes can be connected.  If one of the nodes is already connected to
	 * another it then checks to see if the proposed connection is more desirable.  If it is the old
	 * connection is removed and a new one created.  Otherwise nothing happens.
	 */
	void checkConnect( SquareNode a , int indexA , SquareNode b , int indexB , double distance ) {
		if( a.edges[indexA] != null && a.edges[indexA].distance > distance ) {
			detachEdge(a.edges[indexA]);
		}

		if( b.edges[indexB] != null && b.edges[indexB].distance > distance ) {
			detachEdge(b.edges[indexB]);
		}

		if( a.edges[indexA] == null && b.edges[indexB] == null) {
			connect(a,indexA,b,indexB,distance);
		}
	}

	/**
	 * Performs addition in the cyclical array
	 */
	private static int add( int index , int value ) {
		return CircularIndex.addOffset(index, value, 4);
	}


}
