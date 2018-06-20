/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Storage for feature observation in each view. Input for bundle adjustment. When possible arrays are used to
 * reduce memory requirements.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentObservations {
	public View views[];

	public BundleAdjustmentObservations( int numViews ) {
		views = new View[numViews];
		for (int i = 0; i < numViews; i++) {
			views[i] = new View();
		}
	}

	/**
	 * Returns the total number of observations across all views
	 * @return number of observations
	 */
	public int getObservationCount() {
		int total = 0;
		for (int i = 0; i < views.length; i++) {
			total += views[i].feature.size;
		}
		return total;
	}

	public View getView( int which ) {
		return views[which];
	}

	public static class View {
		public GrowQueue_I32 feature = new GrowQueue_I32();
		public GrowQueue_F32 observations = new GrowQueue_F32();

		public int size() {
			return feature.size;
		}

		public void get(int index , Point2D_F64 p ) {
			index *= 2;
			p.x = observations.data[index];
			p.y = observations.data[index+1];
		}

		public void get(int index , PointIndex2D_F64 observation ) {
			observation.index = feature.data[index];
			index *= 2;
			observation.set( observations.data[index], observations.data[index+1]);
		}

		public void add( int featureIndex , float x , float y ) {
			feature.add(featureIndex);
			observations.add(x);
			observations.add(y);
		}
	}
}
