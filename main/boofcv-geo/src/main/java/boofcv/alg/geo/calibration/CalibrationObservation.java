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

package boofcv.alg.geo.calibration;

import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * List of observed features and their pixel locations on a single calibration target from one image.
 * Partially observable calibration targets is supported by the associated index for each point.
 *
 * @author Peter Abeles
 */
public class CalibrationObservation {

	/**
	 * Shape of the image in which observations were created from. Use to configure intrinsics and sanity checks
	 */
	@Getter @Setter public int width, height;

	/**
	 * List of pixel observations and the index of the control point
	 */
	@Getter public List<PointIndex2D_F64> points = new ArrayList<>();

	public CalibrationObservation( int width, int height ) {
		this.width = width;
		this.height = height;
	}

	public CalibrationObservation() {}

	public void setTo( CalibrationObservation obs ) {
		reset();
		this.width = obs.width;
		this.height = obs.height;
		for (int i = 0; i < obs.size(); i++) {
			PointIndex2D_F64 p = obs.points.get(i);
			points.add(p.copy());
		}
	}

	public PointIndex2D_F64 get( int index ) {
		return points.get(index);
	}

	/**
	 * Adds a new observation. A copy is made.
	 *
	 * @param observation The observation. A copy is internally created.
	 * @param which Index of the observed feature
	 */
	public void add( Point2D_F64 observation, int which ) {
		points.add(new PointIndex2D_F64(observation.x, observation.y, which));
	}

	public void add( double x, double y, int which ) {
		points.add(new PointIndex2D_F64(x, y, which));
	}

	public void reset() {
		points.clear();
		this.width = 0;
		this.height = 0;
	}

	/**
	 * Ensures the points order is in increasing order of index value
	 */
	public void sort() {
		// check to see if they are already ordered first to avoid wasting CPU
		boolean ordered = true;
		for (int i = 1; i < points.size(); i++) {
			if (points.get(i - 1).index >= points.get(i).index) {
				ordered = false;
				break;
			}
		}
		if (ordered)
			return;

		// Old style sort is used for java 1.8 compatibility
		Collections.sort(points, Comparator.comparingInt(o -> o.index));
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
