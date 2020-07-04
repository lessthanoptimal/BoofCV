/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure2;

import boofcv.struct.feature.AssociatedIndex;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Graph describing the relationship between image features using matching features from epipolar geometry.
 *
 * @author Peter Abeles
 */
public class PairwiseImageGraph2 {

	public FastQueue<View> nodes = new FastQueue<>(View::new);
	public FastQueue<Motion> edges = new FastQueue<>(Motion::new);

	public Map<String,View> mapNodes = new HashMap<>();

	public void reset() {
		mapNodes.clear();
		nodes.reset();
		edges.reset();
	}

	public View createNode( String id ) {
		View v = nodes.grow();
		v.init(id);
		mapNodes.put(id,v);
		return v;
	}

	public View lookupNode( String id ) {
		return mapNodes.get(id);
	}

	public Motion connect( View a , View b ) {
		Motion m = edges.grow();
		m.src = a;
		m.dst = b;
		m.index = edges.size-1;
		a.connections.add(m);
		b.connections.add(m);
		return m;
	}

	public static class View {
		/**
		 * Unique identifier for this view
		 */
		public String id;
		/**
		 * Total number of features in this view
		 */
		public int totalFeatures;

		/**
		 * List of motions associated with this view. It can be either the src or dst
		 */
		public FastArray<Motion> connections = new FastArray<>(Motion.class);

		void init( String id ) {
			this.id = id;
			this.connections.reset();
		}

		public View connection( int index ) {
			return connections.get(index).other(this);
		}

		public Motion findMotion( View target ) {
			int idx = findMotionIdx(target);
			if( idx == -1 )
				return null;
			else
				return connections.get(idx);
		}

		public int findMotionIdx( View target ) {
			for (int i = 0; i < connections.size; i++) {
				Motion m = connections.get(i);
				if( m.src == target || m.dst == target ) {
					return i;
				}
			}
			return -1;
		}


		/**
		 * Adds the views that it's connected to from the list
		 */
		public void getConnections(int[] indexes , int length ,
								   List<View> views ) {
			views.clear();
			for (int i = 0; i < length; i++) {
				views.add( connections.get(indexes[i]).other(this));
			}
		}
	}

	public static class Motion {
		/**
		 * 3x3 matrix describing epipolar geometry. Fundamental, Essential, or Homography
		 */
		public final DMatrixRMaj F = new DMatrixRMaj(3,3);

		/** if this camera motion is known up to a metric transform. otherwise it will be projective */
		public boolean is3D;

		/**
		 * Number of inliers when an 3D model (Fundamental/Essential) was fit to observations.
		 */
		public int countF;
		/**
		 * Number of inliers when a homography was fit to observations.
		 */
		public int countH;

		/** Indexes of features in 'src' and 'dst' views which are inliers to the model {@link #F} */
		public final FastQueue<AssociatedIndex> inliers = new FastQueue<>(AssociatedIndex::new);

		/** Two views that this motion connects */
		public View src,dst;

		/** Index of motion in {@link #edges}*/
		public int index;

		public void init() {
			F.zero();
			is3D = false;
			index = -1;
			src = null;
			dst = null;
		}

		/** Given one of the view this motion connects return the other */
		public View other( View src ) {
			if( src == this.src) {
				return dst;
			} else if( src == dst){
				return this.src;
			} else {
				throw new RuntimeException("BUG!");
			}
		}
	}
}
