/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * List of observed features on a single calibration target from one image.  Support for partial observations
 * is provided by giving the index of the.
 *
 * @author Peter Abeles
 */
public class CalibrationObservation {
	/**
	 * Pixel observation of calibration point on the target
	 */
	public List<Point2D_F64> observations = new ArrayList<Point2D_F64>();
	/**
	 * Which specific calibration point the observation refers to.  These numbers refer to a point's index in the
	 * "layout".  The order of indexes here will always be in increasing order.  For example, if calibration points
	 * 5,7,2,4 are observed then the order will be 2,4,5,7.
	 */
	public GrowQueue_I32 indexes = new GrowQueue_I32();

	public void setTo( CalibrationObservation obs ) {
		reset();
		for (int i = 0; i < obs.size(); i++) {
			observations.add( obs.observations.get(i).copy() );
			indexes.add( obs.indexes.get(i) );
		}
	}

	public void add( Point2D_F64 observation , int which ) {
		observations.add( observation.copy() );
		indexes.add(which);
	}

	public void reset() {
		observations.clear();
		indexes.reset();
	}

	public int size() {
		return observations.size();
	}
}
