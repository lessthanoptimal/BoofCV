/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Storage for feature observation in each view. Input for bundle adjustment. When possible arrays are used to
 * reduce memory requirements.
 *
 * @author Peter Abeles
 */
public class SceneObservations {
	/**
	 * Views of general points.
	 */
	public FastQueue<View> views = new FastQueue<>(View.class,true);
	/**
	 * Views of points on rigid objects
	 */
	public FastQueue<View> viewsRigid = new FastQueue<>(View.class,true);

	/**
	 * Constructor with zero rigid objects assumed.
	 *
	 * @param numViews Number of views
	 */
	public SceneObservations(int numViews ) {
		this(numViews,false);
	}

	/**
	 *
	 * @param numViews Number of views
	 * @param rigidObjects If true then there are rigid objects that can be observed
	 */
	public SceneObservations(int numViews , boolean rigidObjects ) {
		views.resize(numViews);
		if( rigidObjects ) {
			viewsRigid.resize(numViews);
		}
	}

	/**
	 * Returns the total number of observations across all views. general and rigid points
	 * @return number of observations
	 */
	public int getObservationCount() {
		return countObservations(viewsRigid) + countObservations(views);
	}

	private int countObservations( FastQueue<View> views ) {
		if( views == null )
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
		return views.size != 0 && views.size==viewsRigid.size;
	}

	public View getView( int which ) {
		return views.data[which];
	}

	public View getViewRigid( int which ) {
		return viewsRigid.data[which];
	}

	public static class View {
		// list of Point ID's which this view can see
		public GrowQueue_I32 point = new GrowQueue_I32();
		// The observation of the point in the view in an interleaved format. In image pixels.
		public GrowQueue_F32 observations = new GrowQueue_F32();

		public int size() {
			return point.size;
		}

		/**
		 * Removes the feature and observation at the specified element
		 */
		public void remove(int index ) {
			point.remove(index);
			index *= 2;
			observations.remove(index,index+1);
		}

		public void set(int index, float x , float y ) {
			index *= 2;
			observations.data[index] = x;
			observations.data[index+1] = y;
		}

		public int getPointId( int index ) {
			return point.get(index);
		}

		public void get(int index , Point2D_F64 p ) {
			if( index >= point.size )
				throw new IndexOutOfBoundsException(index+" >= "+point.size);
			index *= 2;
			p.x = observations.data[index];
			p.y = observations.data[index+1];
		}

		public void get(int index , PointIndex2D_F64 observation ) {
			if( index >= point.size )
				throw new IndexOutOfBoundsException(index+" >= "+point.size);
			observation.index = point.data[index];
			index *= 2;
			observation.set( observations.data[index], observations.data[index+1]);
		}

		/**
		 * Adds an observation of the specified feature.
		 * @param featureIndex Feature index
		 * @param x pixel x-coordinate
		 * @param y pixel y-coordinate
		 */
		public void add( int featureIndex , float x , float y ) {
			point.add(featureIndex);
			observations.add(x);
			observations.add(y);
		}

		public void checkDuplicatePoints() {
			for (int i = 0; i < point.size; i++) {
				int pa = point.get(i);
				for (int j = i+1; j < point.size; j++) {
					if( pa == point.get(j))
						throw new RuntimeException("Duplicates");
				}
			}
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
				for (int i = obsIdx+1; i < v.size(); i++) {
					if( a == v.point.get(i)) {
						new RuntimeException("Same point is viewed more than once in the same view");
					}
				}
			}
		}
	}
}
