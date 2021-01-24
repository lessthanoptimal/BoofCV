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

package boofcv.alg.sfm.structure;

import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * A scene graph which is designed to be easy to manipulate as the scene's structure is determined. Intended to be
 * used with {@link PairwiseImageGraph}.
 *
 * @author Peter Abeles
 */
public class SceneWorkingGraph {

	/** List of all views in the scene graph. Look up based on the image/view's id */
	public final Map<String, View> views = new HashMap<>();
	public final List<View> viewList = new ArrayList<>();
	/** List of all features in the scene */
	public final List<Feature> features = new ArrayList<>(); // TODO change to something else so remove() is faster

	/**
	 * Resets it into it's initial state.
	 */
	public void reset() {
		views.clear();
		viewList.clear();
		features.clear();
	}

	public View lookupView( String id ) {
		return views.get(id);
	}

	public boolean isKnown( PairwiseImageGraph.View pview ) {
		return views.containsKey(pview.id);
	}

	/**
	 * Adds a new view to the graph. If the view already exists an exception is thrown
	 *
	 * @param pview (Input) Pairwise view to create the new view from.
	 * @return The new view created
	 */
	public View addView( PairwiseImageGraph.View pview ) {
		View v = new View();
		v.pview = pview;
		checkTrue(null == views.put(v.pview.id, v),
				"There shouldn't be an existing view with the same key: '" + v.pview.id + "'");
		v.index = viewList.size();
		viewList.add(v);
		return v;
	}

	public Feature createFeature() {
		Feature f = new Feature();
		f.reset();
		features.add(f);
		return f;
	}

	public List<View> getAllViews() {
		return viewList;
	}

	/**
	 * A 3D point feature and the list of observations of this feature.
	 */
	public static class Feature {
		// location in world coordinates
		public final Point4D_F64 location = new Point4D_F64();
		// which views it's visible in
		public final List<Observation> observations = new ArrayList<>();

		public void reset() {
			location.setTo(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
			observations.clear();
		}

		public Point2D_F64 findObservation( View v ) {
			for (int i = 0; i < observations.size(); i++) {
				if (observations.get(i).view == v)
					return observations.get(i).pixel;
			}
			return null;
		}
	}

	/**
	 * Observation (pixel coordinates) of an image feature inside of a {@link View}. Specifies which observation in
	 * the view it's associated with, the view, and a copy of the actual obnervation.
	 */
	public static class Observation {
		// The view this feature was observed in
		public View view;
		// index/identifier for the observation of this feature in the view
		public int observationIdx;
		// The value of the observation in the image. pixels
		public final Point2D_F64 pixel = new Point2D_F64();

		public Observation() {}

		public Observation( View view, int observationIdx ) {
			this.view = view;
			this.observationIdx = observationIdx;
		}

		public void reset() {
			this.view = null;
			this.observationIdx = -1;
			this.pixel.setTo(Double.NaN, -Double.NaN);
		}
	}

	/**
	 * Information on the set of inlier observations used to compute the camera location
	 */
	public static class InlierInfo {
		// List of views from which these inliers were selected from
		// the first view is always the view which contains this set of info
		public final FastArray<PairwiseImageGraph.View> views = new FastArray<>(PairwiseImageGraph.View.class);
		// indexes of observations for each view listed in 'views'.  obs[view][idx] will refer to the same feature
		// for all 'idx'
		public final DogArray<DogArray_I32> observations = new DogArray<>(DogArray_I32::new, DogArray_I32::reset);

		public boolean isEmpty() { return observations.size == 0; }

		/** Returns total number of features are included in this inlier set */
		public int getInlierCount() { return observations.get(0).size; }

		public void reset() {
			views.reset();
			observations.reset();
		}
	}

	/**
	 * Convenience function to get a mapping of view ID to index.
	 *
	 * @param storage (Output) Optional storage for output.
	 * @return Map from view ID to view index
	 */
	public TObjectIntMap<String> lookupUsedViewIds( @Nullable TObjectIntMap<String> storage ) {
		if (storage == null)
			storage = new TObjectIntHashMap<>();

		for (int i = 0; i < viewList.size(); i++) {
			storage.put(viewList.get(i).pview.id, i);
		}

		return storage;
	}

	/**
	 * Data structure related to an image. Points to image features, intrinsic parameters, and extrinsic parameters.
	 */
	static public class View {
		public PairwiseImageGraph.View pview;
		// view to global
		// provides a way to lookup features given their ID in the view
		public final TIntObjectHashMap<Feature> obs_to_feat = new TIntObjectHashMap<>();

		// Specifies which observations were used to compute the projective transform for this view
		// If empty that means one set of inliers are used to multiple views and only one view needed this to be saved
		// this happens for the seed view
		public final InlierInfo inliers = new InlierInfo();

		// projective camera matrix
		public final DMatrixRMaj projective = new DMatrixRMaj(3, 4);
		// metric camera
		public final BundlePinholeSimplified intrinsic = new BundlePinholeSimplified();
		public final Se3_F64 world_to_view = new Se3_F64();
		public final ImageDimension imageDimension = new ImageDimension();

		// Index of the view in the list. This will be the same index in the SBA scene
		public int index = -1;

		/**
		 * Given the observation index return the feature associated with it. Return null if there are none
		 */
		public Feature getFeatureFromObs( int observationIdx ) {
			return obs_to_feat.get(observationIdx);
		}

		/**
		 * Assigns the specified observation in this view to the specified feature
		 *
		 * @param observationIdx Index of the observation
		 * @param feature Which feature it belongs to
		 */
		public void setObsToFeature( int observationIdx, Feature feature ) {
			obs_to_feat.put(observationIdx, feature);
		}

		public void reset() {
			index = -1;
			pview = null;
			imageDimension.setTo(-1, -1);
			obs_to_feat.clear();
			projective.zero();
			intrinsic.reset();
			inliers.reset();
		}

		@Override
		public String toString() {
			return "View{id='" + pview.id + "' inliers=" + (!inliers.isEmpty()) + "}";
		}
	}
}
