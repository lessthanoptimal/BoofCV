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
import boofcv.struct.feature.AssociatedTripleIndex;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;

import java.util.*;

/**
 * @author Peter Abeles
 */
public class SceneWorkingGraph {

	private Map<String,View> views = new HashMap<>();
	private List<Feature> features = new ArrayList<>();
	private List<Observation> observations = new ArrayList<>();

	public View lookupView( String id ) {
		return views.get(id);
	}

	/**
	 * Initializes
	 * @param pv
	 * @return
	 */
	public View initialize( PairwiseImageGraph2.View pv ) {
		View v = addView(pv);
		for (int featIdx = 0; featIdx < pv.totalFeatures; featIdx++) {
			Feature f = createFeature();
			f.index = featIdx;
			f.known = false;
			Observation o = createObservation();
			o.viewIdx = featIdx;
			o.view = v;
			f.visible.add(o);
			v.v2g.put(featIdx,f);
		}
		return v;
	}

	public boolean isKnown( PairwiseImageGraph2.View pview ) {
		return views.containsKey(pview.id);
	}

	public Feature lookupFeature( PairwiseImageGraph2.View pview , int viewIdx ) {
		View v = views.get(pview.id);
		if( v == null )
			return null;
		return v.v2g.get(viewIdx);
	}

	public View addView( PairwiseImageGraph2.View pview ) {
		View v = new View();
		v.pview = pview;
		views.put(v.pview.id,v);
		return v;
	}

	/**
	 * Adds the view and the feature's associated with it. All of its features are labeled as known, unknown,
	 * or contradiction. If known then this view is added to the feature. If unknown then a new feature is created.
	 * Features which are contradictions are ignored.
	 */
	public View addViewAndFeatures( PairwiseImageGraph2.View pview ) {
		View viewA = addView(pview);

		int[] l2g = findKnownFeatures(pview);
		addLocalFeatures(pview, viewA, l2g);

		return viewA;
	}

	private int[] findKnownFeatures(PairwiseImageGraph2.View pview) {
		// look up table from local index to global index
		int[] l2g = new int[ pview.totalFeatures ];
		Arrays.fill(l2g,-1);

		// Go through all connections and find features which are already known
		for (int connIdx = 0; connIdx < pview.connections.size; connIdx++) {
			PairwiseImageGraph2.Motion m = pview.connections.get(connIdx);

			boolean isSrc = m.src==pview;

			View viewB = lookupView((isSrc?m.dst:m.src).id);

			// Go through all the inliers and see if the feature is already known. If it is known
			// then update the table
			for (int inlierIdx = 0; inlierIdx < m.inliers.size; inlierIdx++) {
				AssociatedIndex a = m.inliers.data[inlierIdx];
				int indexA; // index of the feature in viewA
				Feature f;  // Reference to the global feature
				if( isSrc ) {
					indexA = a.src;
					f = viewB.v2g.get(a.dst);
				} else {
					indexA = a.dst;
					f = viewB.v2g.get(a.src);
				}

				// Update the look up table. if there's a contradiction ignore the feature
				if( f != null ) {
					int currentId = l2g[indexA];
					if( currentId == -1 ) {
						l2g[indexA] = f.index;
					} else if( currentId != -2 && currentId != f.index ) {
						// mark it as there being a conflict
						l2g[indexA] = -2;
					}
				}
			}
		}
		return l2g;
	}

	private void addLocalFeatures(PairwiseImageGraph2.View pview, View viewA, int[] l2g) {
		for (int viewIdx = 0; viewIdx < pview.totalFeatures; viewIdx++) {
			// There was a contradiction and the feature should be skipped
			if( l2g[viewIdx] == -2 )
				continue;

			Observation o = createObservation();
			o.view = viewA;
			o.viewIdx = viewIdx;
			Feature f;
			if( l2g[viewIdx] == -1 ) {
				// The feature is unknown, so create a new feature
				f = createFeature();

			} else {
				// The view is known, so add this view to the feature
				f = features.get(l2g[viewIdx] );
			}
			f.visible.add(o);
			viewA.v2g.put(viewIdx,f);
		}
	}

	public Feature createFeature() {
		Feature f = new Feature();
		f.reset();
		f.index = features.size();
		features.add(f);
		return f;
	}

	private Observation createObservation() {
		Observation o = new Observation();
		observations.add(o);
		return o;
	}

	/**
	 *
	 * @param feature The feature being observed
	 * @param view The view it was observed in
	 * @param index The index of the feature in the view
	 */
	public void addObservation( Feature feature , View view , int index ) {
		Observation o = createObservation();
		o.view = view;
		o.viewIdx = index;
		feature.visible.add(o);
	}

	public void lookupCommon(String viewA, String viewB, String viewC,
							 List<Feature> features ,
							 FastQueue<AssociatedTripleIndex> matches ) {

	}

	public Collection<View> getAllViews() {
		return views.values();
	}

	public class Feature {
		// the ID or index of the feature
		public int index;
		// if true then the feature's 3D location is known
		public boolean known;
		// which views it's visible in
		public final List<Observation> visible = new ArrayList<>();

		public void reset() {
			index = -1;
			known = false;
			visible.clear();
		}
	}

	public class Observation {
		public View view;
		// index of the feature in the view
		public int viewIdx;

		public void reset() {
			this.view = null;
			this.viewIdx = -1;
		}
	}

	public class View {
		public PairwiseImageGraph2.View pview;
		// view to global
		// provides a way to lookup features given their ID in the view
		public final TIntObjectHashMap<Feature> v2g = new TIntObjectHashMap<>();

		// projective camera matrix
		public final DMatrixRMaj camera = new DMatrixRMaj(3,4);

		public void reset() {
			pview = null;
			v2g.clear();
			camera.zero();
		}
	}
}
