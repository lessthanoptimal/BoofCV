/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.core.graph;

import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic graph of 2D points.
 *
 * @author Peter Abeles
 */
public class FeatureGraph2D {
	public DogArray<Node> nodes = new DogArray<>(Node::new);
	public DogArray<Edge> edges = new DogArray<>(Edge::new);

	public void reset() {
		nodes.reset();
		edges.reset();
	}

	public void connect( int src , int dst ) {
		Node n = nodes.get(src);
		int idx = n.connection(dst);
		if( idx == -1 ) {
			Edge e = edges.grow();
			e.src = src;
			e.dst = dst;
			n.edges.add(e);
			nodes.get(dst).edges.add(e);
		}
	}

	public static class Node extends Point2D_F64 {
		/**
		 * Optional index to another data structure
		 */
		public int index;
		/**
		 * List of edges connected to it
		 */
		public List<Edge> edges = new ArrayList<>();

		public int connection( int idx ) {
			for( int i = 0; i < edges.size(); i++ ) {
				Edge e = edges.get(i);
				if( e.src == idx || e.dst == idx ) {
					return i;
				}
			}
			return -1;
		}

		public void reset() {
			index = -1;
			x = Double.NaN;
			y = Double.NaN;
			edges.clear();
		}
	}

	/**
	 * Conneciton between two nodes. can be directed or undirected.
	 */
	public static class Edge {
		public int src,dst;

		public void reset() {
			src=-1;
			dst=-1;
		}

		public boolean isConnected( int idx ) {
			return src==idx || dst==idx;
		}
	}

}
