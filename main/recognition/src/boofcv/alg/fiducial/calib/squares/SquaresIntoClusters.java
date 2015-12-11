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

package boofcv.alg.fiducial.calib.squares;

import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.RecycleManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for clustering unorganized squares into different types of clusters.
 *
 * @author Peter Abeles
 */
public class SquaresIntoClusters {

	protected FastQueue<SquareNode> nodes = new FastQueue<SquareNode>(SquareNode.class,true);

	// storage for found clusters
	protected FastQueue<List<SquareNode>> clusters = new FastQueue(ArrayList.class,true);

	// storage for open list when clustering points
	protected List<SquareNode> open = new ArrayList<SquareNode>();
	protected RecycleManager<SquareEdge> edges = new RecycleManager<SquareEdge>(SquareEdge.class);

	/**
	 * Reset and recycle data structures from the previous run
	 */
	protected void recycleData() {
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


	/**
	 * Put sets of nodes into the same list if they are some how connected
	 */
	protected void findClusters() {

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
		open.clear();
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

				if( other.graph == SquareNode.RESET_GRAPH) {
					other.graph = n.graph;
					graph.add(other);
					open.add(other);
				} else if( other.graph != n.graph ) {
					throw new RuntimeException("BUG! "+other.graph+" "+n.graph);
				}
			}
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
}
