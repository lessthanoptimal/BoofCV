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

package boofcv.alg.structure;

import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * A scene graph which is designed to be easy to manipulate as the scene's structure is determined. Intended to be
 * used with {@link PairwiseImageGraph}.
 *
 * @author Peter Abeles
 */
public class SceneWorkingGraph {

	/** Camera database ID to scene Camera*/
	public final TIntObjectMap<Camera> cameras = new TIntObjectHashMap<>();
	/** List of scene cameras */
	public final DogArray<Camera> listCameras = new DogArray<>(Camera::new, Camera::reset);

	/** List of all views in the scene graph. Look up based on the image/view's id */
	public final Map<String, View> views = new HashMap<>();
	public final List<View> listViews = new ArrayList<>();

	/** list of views that have already been explored when expanding the scene */
	public HashSet<String> exploredViews = new HashSet<>();

	/** If stored in an array, the index of the scene in the array */
	public int index;

	/** List of views in pairwise graph it could expand into */
	public FastArray<PairwiseImageGraph.View> open = new FastArray<>(PairwiseImageGraph.View.class);

	/**
	 * Number of views in the seed set. This is used to determine if a feature is a member of the seed set
	 * as the first views are always part of the seed
	 */
	public int numSeedViews;

	/**
	 * Resets it into it's initial state.
	 */
	public void reset() {
		cameras.clear();
		listCameras.reset();
		views.clear();
		listViews.clear();
		exploredViews.clear();
		index = -1;
		open.reset();
		numSeedViews = 0;
	}

	public void setTo( SceneWorkingGraph src ) {
		reset();
		for (int cameraIdx = 0; cameraIdx < src.listCameras.size; cameraIdx++) {
			Camera csrc = src.listCameras.get(cameraIdx);
			Camera c = listCameras.grow();
			c.setTo(csrc);
			cameras.put(c.indexDB, c);
		}
		for (int viewIdx = 0; viewIdx < src.listViews.size(); viewIdx++) {
			View vsrc = src.listViews.get(viewIdx);
			Camera c = getViewCamera(vsrc);
			addView(vsrc.pview, c).setTo(vsrc);
		}
		this.exploredViews.addAll(src.exploredViews);
		this.index = src.index;
		this.numSeedViews = src.numSeedViews;
		this.open.addAll(src.open);
	}

	public View lookupView( String id ) {
		return Objects.requireNonNull(views.get(id));
	}

	public boolean isSeedSet( String id ) {
		return Objects.requireNonNull(views.get(id)).index < numSeedViews;
	}

	public boolean isKnown( PairwiseImageGraph.View pview ) {
		return views.containsKey(pview.id);
	}

	public Camera addCamera( int indexDB ) {
		Camera c = listCameras.grow();
		c.localIndex = listCameras.size - 1;
		c.indexDB = indexDB;
		cameras.put(indexDB, c);
		return c;
	}

	public Camera addCameraCopy( Camera src ) {
		Camera c = listCameras.grow();
		c.localIndex = listCameras.size - 1;
		c.indexDB = src.indexDB;
		c.prior.setTo(src.prior);
		c.intrinsic.setTo(src.intrinsic);
		cameras.put(src.indexDB, c);
		return c;
	}

	/**
	 * Adds a new view to the graph. If the view already exists an exception is thrown
	 *
	 * @param pview (Input) Pairwise view to create the new view from.
	 * @return The new view created
	 */
	public View addView( PairwiseImageGraph.View pview, Camera camera ) {
		View v = new View();
		v.pview = pview;
		v.cameraIdx = camera.localIndex;
		checkTrue(null == views.put(v.pview.id, v),
				"There shouldn't be an existing view with the same key: '" + v.pview.id + "'");
		v.index = listViews.size();
		listViews.add(v);
		return v;
	}

	public List<View> getAllViews() {
		return listViews;
	}

	public Camera getViewCamera( View v ) {
		return listCameras.get(v.cameraIdx);
	}

	/**
	 * Observation (pixel coordinates) of an image feature inside of a {@link View}. Specifies which observation in
	 * the view it's associated with, the view, and a copy of the actual obnervation.
	 */
	@SuppressWarnings("NullAway.Init")
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

		@SuppressWarnings("NullAway")
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
		/**
		 * List of views from which these inliers were selected from
		 * the first view is always the view which contains this set of info
		 */
		public final FastArray<PairwiseImageGraph.View> views = new FastArray<>(PairwiseImageGraph.View.class);
		/**
		 * indexes of observations for each view listed in 'views'. obs[view][idx] will refer to the same feature
		 * for all 'idx'
		 */
		public final DogArray<DogArray_I32> observations = new DogArray<>(DogArray_I32::new, DogArray_I32::reset);

		/**
		 * Score for this inlier set and how good the geometry is. Used to compare the merit of different sets.
		 */
		public double scoreGeometric;

		public boolean isEmpty() { return observations.size == 0; }

		/** Returns total number of features are included in this inlier set */
		public int getInlierCount() { return observations.get(0).size; }

		public void setTo( InlierInfo src ) {
			this.reset();
			this.views.addAll(src.views);
			this.observations.resize(src.observations.size);
			for (int i = 0; i < src.observations.size; i++) {
				this.observations.get(i).setTo(src.observations.get(i));
			}
			this.scoreGeometric = src.scoreGeometric;
		}

		public void reset() {
			views.reset();
			observations.reset();
			scoreGeometric = 0.0;
		}

		@Override public String toString() {
			return String.format("InlierInfo {views.size=%d, score=%.1f}", views.size, scoreGeometric);
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

		for (int i = 0; i < listViews.size(); i++) {
			storage.put(listViews.get(i).pview.id, i);
		}

		return storage;
	}

	/**
	 * Information on a camera which captured one or more views in this scene.
	 */
	public static class Camera {
		/** Index in this scene's array */
		public int localIndex;

		/** Camera's index in the DB */
		public int indexDB;

		/** Prior information about this camera's calibration */
		public final CameraPinholeBrown prior = new CameraPinholeBrown(2);

		/** The estimate value of this camera's intrinsics */
		public final BundlePinholeSimplified intrinsic = new BundlePinholeSimplified();

		public void reset() {
			localIndex = -1;
			indexDB = -1;
			prior.reset();
			intrinsic.reset();
		}

		public void setTo( Camera src ) {
			this.localIndex = src.localIndex;
			this.indexDB = src.indexDB;
			this.prior.setTo(src.prior);
			this.intrinsic.setTo(src.intrinsic);
		}
	}

	/**
	 * Data structure related to an image. Points to image features, intrinsic parameters, and extrinsic parameters.
	 */
	@SuppressWarnings({"NullAway.Init"})
	static public class View {
		/** Reference to the {@link PairwiseImageGraph.View} that this view was generated from */
		public PairwiseImageGraph.View pview;

		/**
		 * Specifies which observations were used to compute the projective transform for this view
		 * If empty that means one set of inliers are used for multiple views and only one view needed
		 * this to be saved this happens for the seed view
		 */
		public final DogArray<InlierInfo> inliers = new DogArray<>(InlierInfo::new, InlierInfo::reset);

		/** projective camera matrix */
		public final DMatrixRMaj projective = new DMatrixRMaj(3, 4);

		/** SE3 from world to this view */
		public final Se3_F64 world_to_view = new Se3_F64();

		/** Index of the camera that generated this view */
		public int cameraIdx = -1;

		/** Index of the view in the list. This will be the same index in the SBA scene */
		public int index = -1;

		/**
		 * Returns the score from the best inlier set
		 */
		public double getBestInlierScore() {
			double max = 0;
			for (int i = 0; i < inliers.size; i++) {
				max = Math.max(max, inliers.get(i).scoreGeometric);
			}
			return max;
		}

		/**
		 * Returns the inlier set with the best score. null if there are no inlier sets
		 */
		public @Nullable InlierInfo getBestInliers() {
			InlierInfo best = null;
			double bestScore = 0.0;

			for (int i = 0; i < inliers.size; i++) {
				InlierInfo info = inliers.get(i);
				if (info.scoreGeometric > bestScore) {
					best = info;
					bestScore = info.scoreGeometric;
				}
			}

			return best;
		}

		@SuppressWarnings({"NullAway"})
		public void reset() {
			index = -1;
			pview = null;
			cameraIdx = -1;
			projective.zero();
			inliers.reset();
			world_to_view.reset();
		}

		public void setTo( View src ) {
			reset();
			index = src.index;
			pview = src.pview;
			cameraIdx = src.cameraIdx;
			projective.setTo(src.projective);
			inliers.resetResize(src.inliers.size);
			for (int i = 0; i < src.inliers.size; i++) {
				inliers.get(i).setTo(src.inliers.get(i));
			}
			world_to_view.setTo(src.world_to_view);
		}

		@Override
		public String toString() {
			return "View{id='" + pview.id + "' inliers=" + inliers.size + "}";
		}
	}
}
