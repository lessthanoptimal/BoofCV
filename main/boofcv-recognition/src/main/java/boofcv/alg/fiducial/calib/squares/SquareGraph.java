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

package boofcv.alg.fiducial.calib.squares;

import boofcv.misc.CircularIndex;
import georegression.metric.Intersection2D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector2D_F64;
import org.ddogleg.struct.RecycleManager;

/**
 * Used for constructing a graph of squares that form a regular grid. Each square can have one edge per side.
 *
 * @author Peter Abeles
 */
public class SquareGraph {
	protected RecycleManager<SquareEdge> edgeManager = new RecycleManager<>(SquareEdge.class);

	Vector2D_F64 vector0 = new Vector2D_F64();
	Vector2D_F64 vector1 = new Vector2D_F64();

	double parallelThreshold = UtilAngle.radian(45);

	public void computeNodeInfo( SquareNode n ) {
		// Under perspective distortion the geometric center is the intersection of the lines formed by
		// opposing corners.
		if( null == Intersection2D_F64.intersection(
				n.square.get(0),n.square.get(2),n.square.get(1),n.square.get(3),
				n.center) ) {
			// This should be impossible
			throw new RuntimeException("BAD");
		}

		// side lengths
		n.largestSide = 0;
		n.smallestSide = Double.MAX_VALUE;
		for (int j = 0,i=3; j < 4; i=j,j++) {
			double l = n.square.get(j).distance(n.square.get(i));
			n.sideLengths[i] = l;
			n.largestSide = Math.max(n.largestSide,l);
			n.smallestSide = Math.min(n.smallestSide,l);
		}
	}

	/**
	 * Removes the edge from the two nodes and recycles the data structure
	 */
	public void detachEdge(SquareEdge edge) {

		edge.a.edges[edge.sideA] = null;
		edge.b.edges[edge.sideB] = null;
		edge.distance = 0;

		edgeManager.recycleInstance(edge);
	}

	/**
	 * Finds the side which intersects the line on the shape.  The line is assumed to pass through the shape
	 * so if there is no intersection it is considered a bug
	 */
	public int findSideIntersect(SquareNode n , LineSegment2D_F64 line , Point2D_F64 intersection, LineSegment2D_F64 storage ) {
		for (int j = 0,i=3; j < 4; i=j,j++) {
			storage.a = n.square.get(i);
			storage.b = n.square.get(j);

			if( Intersection2D_F64.intersection(line,storage,intersection) != null ) {
				return i;
			}
		}

		// bug but I won't throw an exception to stop it from blowing up a bunch
		return -1;
	}

	/**
	 * Checks to see if the two nodes can be connected.  If one of the nodes is already connected to
	 * another it then checks to see if the proposed connection is more desirable.  If it is the old
	 * connection is removed and a new one created.  Otherwise nothing happens.
	 */
	public void checkConnect( SquareNode a , int indexA , SquareNode b , int indexB , double distance ) {
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
	 * Creates a new edge which will connect the two nodes.  The side on each node which is connected
	 * is specified by the indexes.
	 * @param a First node
	 * @param indexA side on node 'a'
	 * @param b Second node
	 * @param indexB side on node 'b'
	 * @param distance distance apart the center of the two nodes
	 */
	void connect( SquareNode a , int indexA , SquareNode b , int indexB , double distance ) {
		SquareEdge edge = edgeManager.requestInstance();
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
	 * Checks to see if the two sides are almost parallel to each other by looking at their acute
	 * angle.
	 */
	public boolean almostParallel(SquareNode a , int sideA , SquareNode b , int sideB ) {
		double selected = acuteAngle(a,sideA,b,sideB);

		if( selected > parallelThreshold )
			return false;

		// see if the two sides are about parallel too
//		double left = acuteAngle(a,add(sideA,-1),b,add(sideB,1));
//		double right = acuteAngle(a,add(sideA,1),b,add(sideB,-1));
//
//		if( left > selected+parallelThreshold || right > selected+parallelThreshold )
//			return false;


//		if( selected >  acuteAngle(a,sideA,b,add(sideB,1)) || selected >  acuteAngle(a,sideA,b,add(sideB,-1)) )
//			return false;
//
//		if( selected >  acuteAngle(a,add(sideA,1),b,sideB) || selected >  acuteAngle(a,add(sideA,-1),b,sideB) )
//			return false;

		return true;
	}

	/**
	 * Returns an angle between 0 and PI/4 which describes the difference in slope
	 * between the two sides
	 */
	public double acuteAngle(  SquareNode a , int sideA , SquareNode b , int sideB ) {
		Point2D_F64 a0 = a.square.get(sideA);
		Point2D_F64 a1 = a.square.get(add(sideA, 1));

		Point2D_F64 b0 = b.square.get(sideB);
		Point2D_F64 b1 = b.square.get(add(sideB, 1));

		vector0.set(a1.x - a0.x, a1.y - a0.y);
		vector1.set(b1.x - b0.x, b1.y - b0.y);

		double acute = vector0.acute(vector1);
		return Math.min(UtilAngle.dist(Math.PI, acute), acute);
	}

	/**
	 * Performs addition in the cyclical array
	 */
	private static int add( int index , int value ) {
		return CircularIndex.addOffset(index, value, 4);
	}

	public RecycleManager<SquareEdge> getEdgeManager() {
		return edgeManager;
	}

	public double getParallelThreshold() {
		return parallelThreshold;
	}

	public void setParallelThreshold(double parallelThreshold) {
		this.parallelThreshold = parallelThreshold;
	}
}
