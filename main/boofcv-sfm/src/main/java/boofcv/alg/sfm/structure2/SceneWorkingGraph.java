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

import boofcv.struct.calib.CameraPinhole;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;

import java.util.*;

/**
 * @author Peter Abeles
 */
public class SceneWorkingGraph {

	public final Map<String,View> views = new HashMap<>();
	public final List<Feature> features = new ArrayList<>(); // TODO change to something else so remove() is faster

	public void reset() {
		views.clear();
		features.clear();
	}

	public View lookupView( String id ) {
		return views.get(id);
	}


	public boolean isKnown( PairwiseImageGraph2.View pview ) {
		return views.containsKey(pview.id);
	}

	public View addView( PairwiseImageGraph2.View pview ) {
		View v = new View();
		v.pview = pview;
		views.put(v.pview.id,v);
		return v;
	}

	public Feature createFeature() {
		Feature f = new Feature();
		f.reset();
		features.add(f);
		return f;
	}

	public Collection<View> getAllViews() {
		return views.values();
	}

	public static class Feature {
		// location in world coordinates
		public final Point3D_F64 location = new Point3D_F64();
		// which views it's visible in
		public final List<Observation> observations = new ArrayList<>();

		public void reset() {
			location.set(Double.NaN,Double.NaN,Double.NaN);
			observations.clear();
		}

		public Point2D_F64 findObservation(View v) {
			for (int i = 0; i < observations.size(); i++) {
				if( observations.get(i).view == v )
					return observations.get(i).pixel;
			}
			return null;
		}
	}

	public static class Observation {
		// The view this feature was observed in
		public View view;
		// index/identifier for the observation of this feature in the view
		public int observationIdx;
		// The value of the observation in the image. pixels
		public final Point2D_F64 pixel = new Point2D_F64();

		public Observation() {}

		public Observation(View view, int observationIdx) {
			this.view = view;
			this.observationIdx = observationIdx;
		}

		public void reset() {
			this.view = null;
			this.observationIdx = -1;
		}
	}

	/**
	 * Information on the set of inlier observations used to compute the camera location
	 */
	public static class InlierInfo
	{
		// List of views from which these inliers were selected from
		// the first view is always the view which contains this set of info
		public final FastArray<PairwiseImageGraph2.View> views = new FastArray<>(PairwiseImageGraph2.View.class);
		// indexes of observations for each view listed in 'views'.  obs[view][idx] will refer to the same feature
		// for all 'idx'
		public final FastQueue<GrowQueue_I32> observations = new FastQueue<>(GrowQueue_I32::new);

		public boolean isEmpty() {
			return observations.size==0;
		}

		public void reset() {
			views.reset();
			observations.reset();
		}
	}

	public class View {
		public PairwiseImageGraph2.View pview;
		// view to global
		// provides a way to lookup features given their ID in the view
		public final TIntObjectHashMap<Feature> obs_to_feat = new TIntObjectHashMap<>();

		// Specifies which observations were used to compute the projective transform for this view
		// If empty that means one set of inliers are used to multiple views and only one view needed this to be saved
		// this happens for the seed view
		public final InlierInfo projectiveInliers = new InlierInfo();

		// projective camera matrix
		public final DMatrixRMaj projective = new DMatrixRMaj(3,4);
		// metric camera
		public final CameraPinhole pinhole = new CameraPinhole();
		public final Se3_F64 world_to_view = new Se3_F64();

		// index in list of views. Only value during construction of SBA data structures
		public int index;

		/**
		 * Given the observation index return the feature associated with it. Return null if there are none
		 */
		public Feature getFeatureFromObs( int observationIdx ) {
			return obs_to_feat.get(observationIdx);
		}

		/**
		 * Assigns the specified observation in this view to the specified feature
		 * @param observationIdx Index of the observation
		 * @param feature Which feature it belongs to
		 */
		public void setObsToFeature( int observationIdx , Feature feature ) {
			obs_to_feat.put(observationIdx,feature);
		}

		public void reset() {
			pview = null;
			obs_to_feat.clear();
			projective.zero();
			pinhole.reset();
			projectiveInliers.reset();
		}
	}
}
