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
import boofcv.struct.image.ImageSingleBand;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Detector for square grid calibration pattern.
 *
 * @author Peter Abeles
 */
// TODO support partial observations of grid
	// TODO tell the polygon detector that there should be no inner contour
public class SorterSquareGrids<T extends ImageSingleBand> {

	public static int MAX_NEIGHBORS = 6;

	// tolerance for fractional distance away a point can be from a line to be considered on the line
	double distanceTol = 0.05;
	// number of radians the acute angle can be for a line to be consdiered parallel
	double acuteAngleTol = UtilAngle.degreeToRadian(10);

	// minimum number of squares in a grid
	int minimumElements;

	double spaceToSquareRatio;


	List<SquareNode> nodes = new ArrayList<SquareNode>();

	// Storage for line segments used to calculate center
	LineSegment2D_F64 lineA = new LineSegment2D_F64();
	LineSegment2D_F64 lineB = new LineSegment2D_F64();
	Point2D_F64 intersection = new Point2D_F64();


	NearestNeighbor<SquareNode> search = FactoryNearestNeighbor.kdtree();
	FastQueue<double[]> searchPoints;
	FastQueue<NnData<SquareNode>> searchResults = new FastQueue(NnData.class,true);

	// All valid graphics
	List<SquareGrid> valid = new ArrayList<SquareGrid>();

	// todo recycle edges, nodes, and graphs

	public SorterSquareGrids(double spaceToSquareRatio, int minimumElements) {
		this.spaceToSquareRatio = spaceToSquareRatio;
		this.minimumElements = minimumElements;

		searchPoints = new FastQueue<double[]>(double[].class,true) {
			@Override
			protected double[] createInstance() {
				return new double[2];
			}
		};

		search.init(2);
	}

	public void process( FastQueue<Polygon2D_F64> squares ) {
		valid.clear();

		// Don't do anything if there aren't enough squares
		if( squares.size() < minimumElements)
			return;

		// Connect nodes to each other
		setupSearch();
		computeNodeInfo();
		connectNodes();

		// Find all valid graphs
		List<List<SquareNode>> all = findGraphs();

		for( int i = 0; i < all.size(); i++ ) {
			List<SquareNode> graph = all.get(i);

			if( graph.size() < minimumElements )
				continue;

			if( !checkNumberOfConnections(graph))
				continue;

			valid.add( orderGraph(graph));
		}
	}

	private List<List<SquareNode>> findGraphs() {
		List<List<SquareNode>> graphs = new ArrayList<List<SquareNode>>();

		for (int i = 0; i < nodes.size(); i++) {
			SquareNode n = nodes.get(i);

			if( n.graph < 0 ) {
				n.graph = graphs.size();
				List<SquareNode> graph = new ArrayList<SquareNode>();
				graph.add(n);
				addToGraph( n , graph );
			}
		}

		return graphs;
	}

	/**
	 * Finds all neighbors and adds them to the graph.  Repeated until there are no more nodes to add to the graph
	 */
	private void addToGraph(SquareNode seed, List<SquareNode> graph) {
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

	/**
	 * See how many nodes have 2 connections or 4 connections.  Then decide if the graph can be accepted or not.
	 */
	private boolean checkNumberOfConnections( List<SquareNode> graph ) {
		int histogram[] = new int[5];

		for (int i = 0; i < graph.size(); i++) {
			histogram[ graph.get(i).getNumberOfConnections() ]++;
		}

		if( histogram[0] != 0 )
			return false;
		if( histogram[1] != 0 )
			return false;
		if( histogram[2] != 4 )
			return false;
		if( histogram[3] != 0 )
			return false;
		if( histogram[4] != (graph.size()-4) )
			return false;
		return true;
	}

	private SquareGrid orderGraph( List<SquareNode> graph ) {
		// Find a side with 2 connections

		// pick the first child and traverse down,

		// now move down the other child then go down in other direction
		return null;
	}


	private void computeNodeInfo() {
		for (int i = 0; i < nodes.size(); i++) {
			SquareNode n = nodes.get(i);

			lineA.a = n.corners.get(0);
			lineA.b = n.corners.get(2);
			lineB.a = n.corners.get(1);
			lineB.b = n.corners.get(3);

			Intersection2D_F64.intersection(lineA,lineB,n.center);

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
		search.setPoints(searchPoints.toList(),nodes);
	}

	private void connectNodes() {
		for (int i = 0; i < nodes.size(); i++) {
			SquareNode n = nodes.get(i);
			double[] point = searchPoints.get(i);

			// find it's neighbors
			searchResults.reset();
			search.findNearest(point,n.largestSide*1.1,MAX_NEIGHBORS,searchResults);

			// try to attach it's closest neighbors
			for (int j = 0; j < searchResults.size(); j++) {
				NnData<SquareNode> neighbor = searchResults.get(j);
				considerAttach(n,neighbor.data);
			}
		}
	}

	private void considerAttach( SquareNode n , SquareNode candidate ) {

		// Find the side on each line which intersects the line connecting the two centers
		lineA.a = n.center;
		lineA.b = candidate.center;

		int intersectionN = findSideIntersect(n,lineA,lineB);
		int intersectionC = findSideIntersect(candidate,lineA,lineB);

		if( intersectionC < 0 || intersectionN < 0 )
			return;

		double distanceApart = lineA.getLength();

		// TODo make sure it's not too far away

		// see if they are approximately parallel
		if( !areLinesParallel(n.corners.get(intersectionN),n.corners.get(add(intersectionN,1)),
				candidate.corners.get(intersectionC),candidate.corners.get(add(intersectionC,1)))) {
			return;
		}

		// see if the end points lie near the line defined by the adjacent lines and each shape
		if( !areMiddlePointsClose(n.corners.get(add(intersectionN, -1)),n.corners.get(intersectionN),
				candidate.corners.get(add(intersectionC,1)),candidate.corners.get(add(intersectionC,2)))) {
			return;
		}

		if( areMiddlePointsClose(n.corners.get(add(intersectionN,2)),n.corners.get(add(intersectionN,1)),
				candidate.corners.get(intersectionC),candidate.corners.get(add(intersectionC,-1)))) {

			checkConnect(n,intersectionN,candidate,intersectionC,distanceApart);
		}
	}

	private int findSideIntersect( SquareNode n , LineSegment2D_F64 line , LineSegment2D_F64 storage ) {
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

	private boolean areLinesParallel( Point2D_F64 a0 , Point2D_F64 b0 , Point2D_F64 a1 , Point2D_F64 b1 ) {
		vector0.set(b0.x-a0.x,b0.y-a0.y);
		vector1.set(a1.x-b1.x,a1.y-b1.y);

		return vector0.acute(vector1) <= acuteAngleTol;
	}

	/**
	 * Returns true if point p1 and p2 are close to the line defined by points p0 and p3.
	 */
	private boolean areMiddlePointsClose( Point2D_F64 p0 , Point2D_F64 p1 , Point2D_F64 p2 , Point2D_F64 p3 ) {
		lineB.a = p0;
		lineB.b = p3;

		double tol = Math.max(1,lineB.getLength()*distanceTol);

		if(Distance2D_F64.distance(lineB,p1) > tol )
			return false;

		return Distance2D_F64.distance(lineB, p2) <= tol;
	}

	private void checkConnect( SquareNode a , int indexA , SquareNode b , int indexB , double distance ) {
		if( a.edges[indexA] != null && a.edges[indexA].distance > distance ) {
			removeEdge(a, indexA);
		}

		if( b.edges[indexB] != null && b.edges[indexB].distance > distance ) {
			removeEdge(b, indexB);
		}

		if( a.edges[indexA] == null && b.edges[indexB] == null) {
			connect(a,indexA,b,indexB,distance);
		}
	}

	private void removeEdge( SquareNode n , int side ) {
		SquareEdge edge = n.edges[side];

		edge.a.edges[edge.sideA] = null;
		edge.b.edges[edge.sideB] = null;
	}

	private void connect( SquareNode a , int indexA , SquareNode b , int indexB , double distance ) {
		SquareEdge edge = new SquareEdge();
		edge.a = a;
		edge.sideA = indexA;
		edge.b = b;
		edge.sideB = indexB;
		edge.distance = distance;

		a.edges[indexA] = edge;
		b.edges[indexB] = edge;
	}

	private static int add( int index , int value ) {
		return UtilShapePolygon.addOffset(index, value,4);
	}

	/**
	 * Returns a list of all the square grids it found
	 */
	public List<SquareGrid> getGrids() {
		return valid;
	}
}
