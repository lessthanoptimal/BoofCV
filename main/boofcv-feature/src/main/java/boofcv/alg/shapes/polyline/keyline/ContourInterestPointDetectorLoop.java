/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes.polyline.keyline;

import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.List;

/**
 * Detects points inside a contour which are potential corner points
 *
 * @author Peter Abeles
 */
public class ContourInterestPointDetectorLoop {

	GrowQueue_I32 indexes = new GrowQueue_I32();

	public void process(List<Point2D_I32> contour ) {
		indexes.reset();
	}

	/**
	 * Indexes of interest points
	 * @return interest points
	 */
	public GrowQueue_I32 getIndexes() {
		return null;
	}

	/**
	 * Points all the interest points into a list.
	 * @param contour (Input) The original contour. Not modified.
	 * @param output (Output) Only the interest points from the contour. Modified.
	 */
	public void getInterestPoints( List<Point2D_I32> contour , List<Point2D_I32> output ) {
		output.clear();
	}
}
