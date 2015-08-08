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

package boofcv.alg.feature.detect.squares;

import boofcv.alg.shapes.polygon.UtilShapePolygon;
import georegression.metric.Distance2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.RecycleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes the detected squares in the image and connects them into clusters.  Two squares can only be connected
 * if their sides are approximately adjacent.  Otherwise the clusters are no
 *
 * @author Peter Abeles
 */
public class SquaresIntoClusters {

	// maximum neighbors on nearest-neighbor search
	public static int MAX_NEIGHBORS = 7;

	// tolerance for fractional distance away a point can be from a line to be considered on the line
	double distanceTol = 0.05;
	// number of radians the acute angle can be for a line to be consdiered parallel
	double acuteAngleTol = UtilAngle.degreeToRadian(10);

	// ratio of the length of a square to the distance separating the square
	private double spaceToSquareRatio;

	private FastQueue<SquareNode> nodes = new FastQueue<SquareNode>(SquareNode.class,true);
	protected RecycleManager<SquareEdge> edges = new RecycleManager<SquareEdge>(SquareEdge.class);

	// Storage for line segments used to calculate center
	private LineSegment2D_F64 lineA = new LineSegment2D_F64();
	private LineSegment2D_F64 lineB = new LineSegment2D_F64();
	private Point2D_F64 intersection = new Point2D_F64();

	// used to search for neighbors that which are candidates for connecting
	private NearestNeighbor<SquareNode> search = FactoryNearestNeighbor.kdtree();
	private FastQueue<double[]> searchPoints;
	private FastQueue<NnData<SquareNode>> searchResults = new FastQueue(NnData.class,true);

	// storage for found clusters
	private FastQueue<List<SquareNode>> clusters = new FastQueue(ArrayList.class,true);

	public SquaresIntoClusters(double spaceToSquareRatio ) {
		this.spaceToSquareRatio = spaceToSquareRatio;

		searchPoints = new FastQueue<double[]>(double[].class,true) {
			@Override
			protected double[] createInstance() {
				return new double[2];
			}
		};

		search.init(2);
	}

	public List<List<SquareNode>> process( List<Polygon2D_F64> squares ) {
		recycleData();

		// set up nodes
		computeNodeInfo(squares);

		// Connect nodes to each other
		setupSearch();
		connectNodes();

		// Find all valid graphs
		findClusters();
		return clusters.toList();
	}

	/**
	 * Reset and recycle data structures from the previous run
	 */
	private void recycleData() {
		for (int i = 0; i < nodes.size(); i++) {
			SquareNode n = nodes.get(i);
			for (int j = 0; j < 4; j++) {
				if( n.edges[j] != null ) {
					detachEdge(n.edges[j]);
				}
			}
		}
		nodes.reset();

		for (int i = 0; i < clusters.size; i++) {
			clusters.get(i).clear();
		}
		clusters.reset();
	}

	private void computeNodeInfo( List<Polygon2D_F64> squares ) {

		for (int i = 0; i < squares.size(); i++) {
			SquareNode n = nodes.grow();
			n.reset();
			n.corners = squares.get(i);

			// does not assume CW or CCW ordering just that it is ordered
			lineA.a = n.corners.get(0);
			lineA.b = n.corners.get(2);
			lineB.a = n.corners.get(1);
			lineB.b = n.corners.get(3);

			Intersection2D_F64.intersection(lineA, lineB, n.center);

			for (int j = 0; j < 4; j++) {
				int k = (j+1)%4;
				double l = n.corners.get(j).distance(n.corners.get(k));
				n.sideLengths[j] = l;
				n.largestSide = Math.max(n.largestSide,l);
			}
		}
	}

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

	private void connectNodes() {
		for (int i = 0; i < nodes.size(); i++) {
			SquareNode n = nodes.get(i);
			double[] point = searchPoints.get(i);

			// distance between center when viewed head on will be space + 0.5*2*width.
			// when you factor in foreshortening this search will not be symmetric
			// the smaller will miss its larger neighbor but the larger one will find the smaller one.
			double neighborDistance = n.largestSide*(1.0+spaceToSquareRatio)*1.2;

			// find it's neighbors
			searchResults.reset();
			search.findNearest(point,neighborDistance,MAX_NEIGHBORS,searchResults);

			// try to attach it's closest neighbors
			for (int j = 0; j < searchResults.size(); j++) {
				NnData<SquareNode> neighbor = searchResults.get(j);
				if( neighbor.data != n )
					considerAttach(n,neighbor.data);
			}
		}
	}

	void considerAttach( SquareNode n , SquareNode candidate ) {

		// Find the side on each line which intersects the line connecting the two centers
		lineA.a = n.center;
		lineA.b = candidate.center;

		int intersectionN = findSideIntersect(n,lineA,lineB);
		int intersectionC = findSideIntersect(candidate,lineA,lineB);

		if( intersectionC < 0 || intersectionN < 0 )
			return;

		double distanceApart = lineA.getLength();

		// TODO make sure it's not too far away

		// see if they are approximately parallel
		if( !areSidesParallel(n,intersectionN,candidate,intersectionC)) {
			return;
		}

		// See if the connecting sides end points lie along the line defined by the adjacent sides on
		// each shape
		if( !areMiddlePointsClose(n.corners.get(add(intersectionN, -1)),n.corners.get(intersectionN),
				candidate.corners.get(add(intersectionC,1)),candidate.corners.get(add(intersectionC,2)))) {
			return;
		}

		if( areMiddlePointsClose(n.corners.get(add(intersectionN,2)),n.corners.get(add(intersectionN,1)),
				candidate.corners.get(intersectionC),candidate.corners.get(add(intersectionC,-1)))) {

			checkConnect(n,intersectionN,candidate,intersectionC,distanceApart);
		}
	}

	private void findClusters() {

		for (int i = 0; i < nodes.size(); i++) {
			SquareNode n = nodes.get(i);

			if( n.graph < 0 ) {
				n.graph = clusters.size();
				List<SquareNode> graph = clusters.grow();
				graph.add(n);
				addToCluster(n, graph);
			}
		}
	}

	/**
	 * Finds all neighbors and adds them to the graph.  Repeated until there are no more nodes to add to the graph
	 */
	void addToCluster(SquareNode seed, List<SquareNode> graph) {
		List<SquareNode> open = new ArrayList<SquareNode>();
		open.add(seed);
		while( !open.isEmpty() ) {
			SquareNode n = open.remove( open.size() -1 );

			for (int i = 0; i < 4; i++) {
				SquareEdge edge = n.edges[i];
				if( edge == null )
					continue;

				SquareNode other;
				if( edge.a == n )
					other = edge.b;
				else if( edge.b == n )
					other = edge.a;
				else
					throw new RuntimeException("BUG!");

				if( other.graph == -1) {
					other.graph = n.graph;
					graph.add(other);
					open.add(other);
				} else if( other.graph != n.graph ) {
					throw new RuntimeException("BUG!");
				}
			}
		}
	}

	int findSideIntersect( SquareNode n , LineSegment2D_F64 line , LineSegment2D_F64 storage ) {
		for (int i = 0; i < 4; i++) {
			int j = (i+1)%4;

			storage.a = n.corners.get(i);
			storage.b = n.corners.get(j);

			if( Intersection2D_F64.intersection(line,storage,intersection) != null ) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Returns true if line segment (a0,b0) is parallel to line segment (a1,b1)
	 */
	Vector2D_F64 vector0 = new Vector2D_F64();
	Vector2D_F64 vector1 = new Vector2D_F64();
	boolean areSidesParallel( SquareNode a , int sideA , SquareNode b , int sideB ) {

		Point2D_F64 a0 = a.corners.get(sideA);
		Point2D_F64 a1 = a.corners.get(add(sideA, 1));

		Point2D_F64 b0 = b.corners.get(sideB);
		Point2D_F64 b1 = b.corners.get(add(sideB, 1));

		vector0.set(a1.x - a0.x, a1.y - a0.y);
		vector1.set(b1.x - b0.x, b1.y - b0.y);

		return vector0.acute(vector1) <= acuteAngleTol;
	}

	/**
	 * Returns true if point p1 and p2 are close to the line defined by points p0 and p3.
	 */
	boolean areMiddlePointsClose( Point2D_F64 p0 , Point2D_F64 p1 , Point2D_F64 p2 , Point2D_F64 p3 ) {
		lineB.a = p0;
		lineB.b = p3;

		// tolerance as a fraction the length of a side
		double tol = lineB.getLength()*distanceTol/(2+spaceToSquareRatio);

		if(Distance2D_F64.distance(lineB, p1) > tol )
			return false;

		return Distance2D_F64.distance(lineB, p2) <= tol;
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
	 * Removes the edge from the two nodes and recycles the data structure
	 */
	void detachEdge(SquareEdge edge) {

		edge.a.edges[edge.sideA] = null;
		edge.b.edges[edge.sideB] = null;

		edges.recycleInstance(edge);
	}

	/**
	 * Creates a new edge which will connect the two nodes.  The side on each node which is connected
	 * is specified by the indexes.
	 * @param a First node
	 * @param indexA side on node 'a'
	 * @param b Second node
	 * @param indexB side on node 'b'
	 * @param distance distance apart the center of the two nodes
	 */
	void connect( SquareNode a , int indexA , SquareNode b , int indexB , double distance ) {
		SquareEdge edge = edges.requestInstance();
		edge.reset();

		edge.a = a;
		edge.sideA = indexA;
		edge.b = b;
		edge.sideB = indexB;
		edge.distance = distance;

		a.edges[indexA] = edge;
		b.edges[indexB] = edge;
	}

	/**
	 * Performs addition in the cyclical array
	 */
	private static int add( int index , int value ) {
		return UtilShapePolygon.addOffset(index, value, 4);
	}


}
