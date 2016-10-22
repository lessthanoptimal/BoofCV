/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * List of observed features on a single calibration target from one image.  Support for partial observations
 * is provided by giving the index of the.
 *
 * @author Peter Abeles
 */
public class CalibrationObservation {
	/**
	 * List of pixel observations and the index of the control point
	 */
	public List<PointIndex2D_F64> points = new ArrayList<>();

	public void setTo( CalibrationObservation obs ) {
		reset();
		for (int i = 0; i < obs.size(); i++) {
			PointIndex2D_F64 p = obs.points.get(i);
			points.add( p.copy() );
		}
	}

	public PointIndex2D_F64 get( int index ) {
		return points.get(index);
	}

	public void add( Point2D_F64 observation , int which ) {
		points.add(new PointIndex2D_F64(observation, which));
	}

	public void reset() {
		points.clear();
	}

	public void sort() {
		Collections.sort(points, new Comparator<PointIndex2D_F64>() {
			@Override
			public int compare(PointIndex2D_F64 o1, PointIndex2D_F64 o2) {
				if( o1.index < o2.index )
					return -1;
				else if( o1.index > o2.index )
					return 1;
				else
					return 0;
			}
		});
	}

	public int size() {
		return points.size();
	}


	public CalibrationObservation copy() {
		CalibrationObservation c = new CalibrationObservation();
		c.setTo(this);
		return c;
	}
}
