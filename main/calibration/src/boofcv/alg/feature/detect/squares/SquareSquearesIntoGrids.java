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
import georegression.struct.line.LineParametric2D_F64;
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
 * Detector for square grid calibration pattern.
 *
 * @author Peter Abeles
 */
// TODO support partial observations of grid
	// TODO tell the polygon detector that there should be no inner contour
public class SquareSquearesIntoGrids {

	public static int MAX_NEIGHBORS = 6;

	boolean verbose = false;

	// tolerance for fractional distance away a point can be from a line to be considered on the line
	double distanceTol = 0.05;
	// number of radians the acute angle can be for a line to be consdiered parallel
	double acuteAngleTol = UtilAngle.degreeToRadian(10);

	// minimum number of squares in a grid
	int minimumElements;

	double spaceToSquareRatio;


	FastQueue<SquareNode> nodes = new FastQueue<SquareNode>(SquareNode.class,true);
	RecycleManager<SquareEdge> edges = new RecycleManager<SquareEdge>(SquareEdge.class);

	// Storage for line segments used to calculate center
	LineSegment2D_F64 lineA = new LineSegment2D_F64();
	LineSegment2D_F64 lineB = new LineSegment2D_F64();
	Point2D_F64 intersection = new Point2D_F64();


	NearestNeighbor<SquareNode> search = FactoryNearestNeighbor.kdtree();
	FastQueue<double[]> searchPoints;
	FastQueue<NnData<SquareNode>> searchResults = new FastQueue(NnData.class,true);

	// All valid graphics
	List<SquareGrid> valid = new ArrayList<SquareGrid>();

	// todo recycle graphs

	public SquareSquearesIntoGrids(double spaceToSquareRatio, int minimumElements) {
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

	public void process( List<Polygon2D_F64> squares ) {
		recycleData();

		// Don't do anything if there aren't enough squares
		if( squares.size() < minimumElements)
			return;

		// set up nodes
		computeNodeInfo(squares);

		// Connect nodes to each other
		setupSearch();
		connectNodes();

		// Find all valid graphs
		List<List<SquareNode>> all = findClusters();

		for( int i = 0; i < all.size(); i++ ) {
			List<SquareNode> graph = all.get(i);

			if( graph.size() < minimumElements )
				continue;

			if( !checkNumberOfConnections(graph))
				continue;

			SquareGrid grid = orderIntoGrid(graph);
			if( grid != null )
				valid.add( grid );
		}
	}

	/**
	 * Reset and recycle data structures from the previous run
	 */
	private void recycleData() {
		for (int i = 0; i < nodes.size(); i++) {
			SquareNode n = nodes.get(i);
			for (int j = 0; j < 4; j++) {
				if( n.edges[j] != null ) {
					removeEdge(n,i);
				}
			}
		}
		nodes.reset();
		valid.clear();
	}

	private List<List<SquareNode>> findClusters() {
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
	 * Does a weak check on the number of edges in the graph.  Since the structure isn't known it can't make
	 * harder checks
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
		return true;
	}

	private SquareGrid orderIntoGrid(List<SquareNode> graph) {

		List<SquareNode> column = new ArrayList<SquareNode>();
		List<SquareNode> ordered = new ArrayList<SquareNode>();

		for (int i = 0; i < graph.size(); i++) {
			// Find a side with 2 connections and use that as the seed
			SquareNode seed = graph.get(i);
			if( seed.getNumberOfConnections() != 2 )
				continue;

			seed.inGraph = true;

			// find all the nodes along one side, just pick an edge arbitrarily.  This will be the first column
			for (int j = 0; j < 4; j++) {
				if( seed.edges[i] == null )
					continue;

				SquareNode b = seed.edges[i].destination(seed);
				b.inGraph = true;

				column.add(seed);
				column.add(b);
				addInLine(seed,b,column);
				break;
			}

			// handle special case where there is only one element
			if( column.size() <= 1 ) {
				ordered.addAll(column);
				break;
			}
			if (addRows(column, ordered)) return null;


			break;
		}

		SquareGrid grid = new SquareGrid();
		grid.nodes = ordered;
		grid.columns = column.size();
		grid.rows = ordered.size() / column.size();

		return grid;
	}

	/**
	 * Compete the graph by traversing down the first column and adding the rows one at a time
	 */
	private boolean addRows(List<SquareNode> column, List<SquareNode> ordered) {
		// now add the rows by traversing down the column
		int numFirsRow = 0;
		for (int j = 0; j < column.size(); j++) {
			SquareNode n = column.get(j);

			ordered.add(n);

			SquareNode nextRow;
			if( j == 0 ) {
				if( n.getNumberOfConnections() != 2 ) {
					if( verbose ) System.err.println(
							"Unexpected number of connections. want 2 found "+n.getNumberOfConnections());
					return true;
				}

				nextRow = pickNot(n, column.get(j + 1));

			} else if( j == column.size()-1 ) {
				if( n.getNumberOfConnections() != 3 ) {
					if (verbose) System.err.println(
							"Unexpected number of connections. want 2 found " + n.getNumberOfConnections());
					return true;
				}
				nextRow = pickNot(n, column.get(j-1),column.get(j+1));
			} else {
				if( n.getNumberOfConnections() != 2 ) {
					if (verbose) System.err.println(
							"Unexpected number of connections. want 2 found " + n.getNumberOfConnections());
					return true;
				}
				nextRow = pickNot(n,column.get(j-1));
			}

			int numberLine = addInLine(n, nextRow, ordered);

			if( j == 0 ) {
				numFirsRow = numberLine;
			} else if(numberLine != numFirsRow ) {
				if( verbose ) System.err.println("Number of elements in rows do not match.");
				return true;
			}
		}
		return false;
	}

	/**
	 * Add all the nodes into the list which lie along the line defined by a and b.  a is assumed to be
	 * an end point.  Care is taken to not cycle.
	 */
	private int addInLine( SquareNode a , SquareNode b , List<SquareNode> list ) {

		LineParametric2D_F64 line = new LineParametric2D_F64();

		int total = 2;

		while( true ) {
			// maximum distance off of line
			double bestDistance = Math.max(3, b.largestSide / 4.0);
			SquareNode best = null;

			line.setP(a.center);
			line.setSlope(b.center.x - a.center.x, b.center.y - a.center.y);

			// pick the child of b which is closest to the line going through the centers and not outside of tolerance
			for (int i = 0; i < 4; i++) {
				if( b.edges[i] == null )
					continue;

				SquareNode c = b.edges[i].destination(b);

				if (c.inGraph )
					continue;

				double distance = Distance2D_F64.distance(line, c.center);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = c;
				}
			}

			if( best == null )
				return total;
			else {
				total++;
				best.inGraph = true;
				list.add(best);
				a = b;
				b = best;
			}
		}
	}

	/**
	 * There are only two edges on target.  Pick the edge which does not go to the provided child
	 */
	private SquareNode pickNot( SquareNode target , SquareNode child ) {
		for (int i = 0; i < 4; i++) {
			SquareEdge e = target.edges[i];
			SquareNode c = e.destination(target);
			if( c != child )
				return c;
		}
		throw new RuntimeException("There was no odd one out some how");
	}

	/**
	 * There are only three edges on target and two of them are known.  Pick the one which isn't an inptu child
	 */
	private SquareNode pickNot( SquareNode target , SquareNode child0 , SquareNode child1 ) {
		for (int i = 0; i < 4; i++) {
			SquareEdge e = target.edges[i];
			SquareNode c = e.destination(target);
			if( c != child0 && c != child1 )
				return c;
		}
		throw new RuntimeException("There was no odd one out some how");
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
		search.setPoints(searchPoints.toList(), nodes.toList());
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

		edges.recycleInstance(edge);
	}

	private void connect( SquareNode a , int indexA , SquareNode b , int indexB , double distance ) {
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
