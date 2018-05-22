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

package boofcv.alg.geo.calibration;

import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
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
	 * Shape image image observations were created from. Use to configure intrinsics and sanity checks
	 */
	int width,height;

	/**
	 * List of pixel observations and the index of the control point
	 */
	public List<PointIndex2D_F64> points = new ArrayList<>();

	public CalibrationObservation(int width, int height) {
		this.width = width;
		this.height = height;
	}

	protected CalibrationObservation(){

	}

	public void setTo(CalibrationObservation obs ) {
		reset();
		this.width = obs.width;
		this.height = obs.height;
		for (int i = 0; i < obs.size(); i++) {
			PointIndex2D_F64 p = obs.points.get(i);
			points.add( p.copy() );
		}
	}

	public PointIndex2D_F64 get( int index ) {
		return points.get(index);
	}

	/**
	 *
	 * @param observation The observation. A copy is internally created.
	 * @param which Index of the observed feature
	 */
	public void add( Point2D_F64 observation , int which ) {
		points.add(new PointIndex2D_F64(observation, which));
	}

	public void add( double x , double y , int which ) {
		points.add(new PointIndex2D_F64(x,y, which));
	}

	public void reset() {
		points.clear();
		this.width = 0;
		this.height = 0;
	}

	public void sort() {
		points.sort(Comparator.comparingInt(o -> o.index));
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int size() {
		return points.size();
	}

	public List<PointIndex2D_F64> getPoints() {
		return points;
	}

	public CalibrationObservation copy() {
		CalibrationObservation c = new CalibrationObservation();
		c.setTo(this);
		return c;
	}
}
