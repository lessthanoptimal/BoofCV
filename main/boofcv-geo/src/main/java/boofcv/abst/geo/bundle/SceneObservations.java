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

package boofcv.abst.geo.bundle;

import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;

/**
 * Storage for feature observation in each view. Input for bundle adjustment. When possible arrays are used to
 * reduce memory requirements.
 *
 * @author Peter Abeles
 */
public class SceneObservations {
	/** Views of general points. */
	public final DogArray<View> views = new DogArray<>(View::new, View::reset);

	/** Views of points on rigid objects */
	public final DogArray<View> viewsRigid = new DogArray<>(View::new, View::reset);

	/**
	 * Initialize the data structures for this number of views. Rigid is set to be false.
	 *
	 * @param numViews Number of views
	 */
	public void initialize( int numViews ) {
		this.initialize(numViews, false);
	}

	/**
	 * Initialize the data structures for this number of views
	 *
	 * @param numViews Number of views
	 * @param rigidObjects If true then there are rigid objects that can be observed
	 */
	public void initialize( int numViews, boolean rigidObjects ) {
		views.reset();
		views.resize(numViews);
		if (rigidObjects) {
			viewsRigid.reset();
			viewsRigid.resize(numViews);
		}
	}

	/**
	 * Returns the total number of observations across all views. general and rigid points
	 *
	 * @return number of observations
	 */
	public int getObservationCount() {
		return countObservations(viewsRigid) + countObservations(views);
	}

	private int countObservations( DogArray<View> views ) {
		if (views == null)
			return 0;
		int total = 0;
		for (int i = 0; i < views.size; i++) {
			total += views.data[i].point.size;
		}
		return total;
	}

	/**
	 * True if there are rigid views
	 */
	public boolean hasRigid() {
		return views.size != 0 && views.size == viewsRigid.size;
	}

	public View getView( int which ) {
		return views.data[which];
	}

	public View getViewRigid( int which ) {
		return viewsRigid.data[which];
	}

	public static class View {
		/** list of Point ID's that are visible in this view. -1 indicates the point has been removed */
		public DogArray_I32 point = new DogArray_I32();
		/** Pixel observations of features in 'point' in an interleaved format (x,y) */
		public DogArray_F32 observations = new DogArray_F32();

		public int size() {
			return point.size;
		}

		/**
		 * Removes the feature and observation at the specified element
		 */
		public void remove( int index ) {
			point.remove(index);
			index *= 2;
			observations.remove(index, index + 1);
		}

		/**
		 * Assigns this observation to th specified feature. Does sanity checks to make sure
		 * everything is consistent.
		 */
		public void safeAssignToFeature( int index, int featureIdx ) {
			if (point.contains(featureIdx))
				throw new IllegalArgumentException("Feature has already been assigned to an observation");
			if (-1 != point.get(index))
				throw new IllegalArgumentException("Observation is already assigned a feature");
			point.set(index, featureIdx);
		}

		/**
		 * Sets the feature Id and observation for an observation
		 *
		 * @param index Which observation to set
		 * @param featureIdx The feature which was observed
		 * @param x observation x-axis
		 * @param y observation y-axis
		 */
		public void set( int index, int featureIdx, float x, float y ) {
			point.set(index, featureIdx);
			index *= 2;
			observations.data[index] = x;
			observations.data[index + 1] = y;
		}

		public void setPixel( int index, float x, float y ) {
			index *= 2;
			observations.data[index] = x;
			observations.data[index + 1] = y;
		}

		public int getPointId( int index ) {
			return point.get(index);
		}

		public void getPixel( int index, Point2D_F64 p ) {
			if (index >= point.size)
				throw new IndexOutOfBoundsException(index + " >= " + point.size);
			index *= 2;
			p.x = observations.data[index];
			p.y = observations.data[index + 1];
		}

		public void getPixel( int index, PointIndex2D_F64 observation ) {
			if (index >= point.size)
				throw new IndexOutOfBoundsException(index + " >= " + point.size);
			observation.index = point.data[index];
			index *= 2;
			observation.p.setTo(observations.data[index], observations.data[index + 1]);
		}

		/**
		 * Adds an observation of the specified feature.
		 *
		 * @param featureIndex Feature index
		 * @param x pixel x-coordinate
		 * @param y pixel y-coordinate
		 */
		public void add( int featureIndex, float x, float y ) {
			point.add(featureIndex);
			observations.add(x);
			observations.add(y);
		}

		public void checkDuplicatePoints() {
			for (int i = 0; i < point.size; i++) {
				int pa = point.get(i);
				for (int j = i + 1; j < point.size; j++) {
					if (pa == point.get(j))
						throw new RuntimeException("Duplicates");
				}
			}
		}

		/**
		 * Puts it back into its original state.
		 */
		public void reset() {
			point.reset();
			observations.reset();
		}

		public void resize( int numPoints ) {
			point.resetResize(numPoints, -1);
			observations.resetResize(numPoints*2, -1);
		}
	}

	/**
	 * Makes sure that each feature is only observed in each view
	 */
	public void checkOneObservationPerView() {
		for (int viewIdx = 0; viewIdx < views.size; viewIdx++) {
			SceneObservations.View v = views.data[viewIdx];

			for (int obsIdx = 0; obsIdx < v.size(); obsIdx++) {
				int a = v.point.get(obsIdx);
				for (int i = obsIdx + 1; i < v.size(); i++) {
					if (a == v.point.get(i)) {
						throw new RuntimeException("Same point is viewed more than once in the same view");
					}
				}
			}
		}
	}
}
