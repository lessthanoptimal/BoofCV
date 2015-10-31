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
	 * List of observations
	 */
	public List<Point> points = new ArrayList<Point>();


	public void setTo( CalibrationObservation obs ) {
		reset();
		for (int i = 0; i < obs.size(); i++) {
			Point p = obs.points.get(i);
			points.add( new Point(p.pixel,p.index));
		}
	}

	public Point get( int index ) {
		return points.get(index);
	}

	public void add( Point2D_F64 observation , int which ) {
		points.add(new Point(observation, which));
	}

	public void reset() {
		points.clear();
	}

	public void sort() {
		Collections.sort(points, new Comparator<Point>() {
			@Override
			public int compare(Point o1, Point o2) {
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

	public static class Point {
		/**
		 * Pixel observation of calibration point on the target
		 */
		public Point2D_F64 pixel = new Point2D_F64();
		/**
		 * Which specific calibration point the observation refers to.  These numbers refer to a point's index in the
		 * "layout".  The order of indexes here will always be in increasing order.  For example, if calibration points
		 * 5,7,2,4 are observed then the order will be 2,4,5,7.
		 */
		public int index;

		public Point(Point2D_F64 pixel, int index) {
			this.pixel.set(pixel);
			this.index = index;
		}

		public Point() {
		}
	}
}
