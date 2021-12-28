/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.mvs;

import java.util.*;

/**
 * Specifies which views can be used as stereo pairs and the quality of the 3D information between the views
 *
 * @author Peter Abeles
 */
public class StereoPairGraph {
	/** List of all the views */
	public final Map<String, Vertex> vertexes = new HashMap<>();

	/**
	 * Creates a new vertex. Throws an exception if a Vertex with the same ID already exists.
	 *
	 * @param id ID of the new vertex
	 * @param indexSba Index of the vertex's view in SBA
	 * @return The new Vertex.
	 */
	public Vertex addVertex( String id, int indexSba ) {
		var v = new Vertex();
		v.id = id;
		v.indexSba = indexSba;

		if (null != vertexes.put(v.id, v))
			throw new IllegalArgumentException("There was already a node with id=" + v.id);

		return v;
	}

	/**
	 * Connects two vertexes together with the specified quality
	 *
	 * @param a (Input) ID of vertex A
	 * @param b (Input) ID of vertex B
	 * @param quality3D (Input) Quality of connection between them
	 * @return The Edge connecting the two vertexes
	 */
	public Edge connect( String a, String b, double quality3D ) {
		Vertex va = Objects.requireNonNull(vertexes.get(a));
		Vertex vb = Objects.requireNonNull(vertexes.get(b));

		Edge e = new Edge();
		e.va = va;
		e.vb = vb;
		e.quality3D = quality3D;

		va.pairs.add(e);
		vb.pairs.add(e);

		return e;
	}

	public void reset() {
		vertexes.clear();
	}

	public static class Vertex {
		/** The view this is in reference to */
		public String id = "";
		/** Index in the SBA scene */
		public int indexSba = -1;
		/** List of all views it can form a 3D stereo pair with */
		public final List<Edge> pairs = new ArrayList<>();
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class Edge {
		/** Vertexes this edge is connected to */
		public Vertex va, vb;

		/** How good the 3D information is between these two views. 0.0 = no 3D. 1.0 = best possible. */
		public double quality3D = 0.0;

		public Vertex other( Vertex src ) {
			if (src == va)
				return vb;
			else if (src == vb)
				return va;
			throw new IllegalArgumentException("Edge does not link to src.id=" + src.id);
		}
	}
}
