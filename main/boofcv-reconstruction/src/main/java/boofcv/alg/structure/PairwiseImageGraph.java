/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.structure;

import boofcv.struct.feature.AssociatedIndex;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastArray;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Graph describing the relationship between image features using matching features from epipolar geometry.
 *
 * @author Peter Abeles
 */
public class PairwiseImageGraph {

	public final DogArray<View> nodes = new DogArray<>(View::new);
	public final DogArray<Motion> edges = new DogArray<>(Motion::new, Motion::reset);

	public final Map<String, View> mapNodes = new HashMap<>();

	public void reset() {
		mapNodes.clear();
		nodes.reset();
		edges.reset();
	}

	public View createNode( String id ) {
		View v = nodes.grow();
		v.init(nodes.size - 1, id);
		mapNodes.put(id, v);
		return v;
	}

	public View lookupNode( String id ) {
		return Objects.requireNonNull(mapNodes.get(id));
	}

	public Motion connect( View a, View b ) {
		Motion m = edges.grow();
		m.src = a;
		m.dst = b;
		m.index = edges.size - 1;
		a.connections.add(m);
		b.connections.add(m);
		return m;
	}

	/**
	 * Information associated with a single image/frame/view
	 */
	@SuppressWarnings("NullAway.Init")
	public static class View {
		/** Unique identifier for this view */
		public String id;
		/** Array index of this view in the 'nodes' array */
		public int index;
		/** Total number of features observations in this view */
		public int totalObservations;
		/** List of motions associated with this view. It can be either the src or dst */
		public FastArray<Motion> connections = new FastArray<>(Motion.class);

		public View() {}

		public View( String id ) {
			this.id = id;
		}

		void init( int index, String id ) {
			this.id = id;
			this.index = index;
			this.totalObservations = 0;
			this.connections.reset();
		}

		public View connection( int index ) {
			return connections.get(index).other(this);
		}

		public @Nullable Motion findMotion( View target ) {
			int idx = findMotionIdx(target);
			if (idx == -1)
				return null;
			else
				return connections.get(idx);
		}

		public int findMotionIdx( View target ) {
			for (int i = 0; i < connections.size; i++) {
				Motion m = connections.get(i);
				if (m.src == target || m.dst == target) {
					return i;
				}
			}
			return -1;
		}

		/**
		 * Adds the views that it's connected to from the list
		 */
		public void getConnections( int[] indexes, int length,
									List<View> views ) {
			views.clear();
			for (int i = 0; i < length; i++) {
				views.add(connections.get(indexes[i]).other(this));
			}
		}

		@Override
		public String toString() {
			return "PView{id=" + id + ", conn=" + connections.size + ", obs=" + totalObservations + "}";
		}
	}

	/**
	 * Relationship between two views. Which features they have in common and value of the 3D information
	 * that can be derived from the two views.
	 */
	@SuppressWarnings("NullAway.Init")
	public static class Motion {
		/** if this camera motion is known up to a metric transform. otherwise it will be projective */
		public boolean is3D;
		/** 3D information score. See {@link EpipolarScore3D} */
		public double score3D;
		/** Indexes of features in 'src' and 'dst' views which are inliers to the model */
		public final DogArray<AssociatedIndex> inliers = new DogArray<>(AssociatedIndex::new);
		/** Two views that this motion connects */
		public View src, dst;
		/** Index of motion in {@link #edges} */
		public int index;

		@SuppressWarnings("NullAway")
		public void reset() {
			is3D = false;
			score3D = 0;
			inliers.reset();
			src = null;
			dst = null;
			index = -1;
		}

		public boolean isConnected( View v ) {
			return v == src || v == dst;
		}

		/** Given one of the view this motion connects return the other */
		public View other( View src ) {
			if (src == this.src) {
				return dst;
			} else if (src == dst) {
				return this.src;
			} else {
				throw new RuntimeException("BUG!");
			}
		}

		@Override
		public String toString() {
			return "Motion( " + (is3D ? "3D " : "") + " '" + src.id + "' <-> '" + dst.id + "')";
		}
	}
}
