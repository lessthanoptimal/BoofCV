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

package boofcv.alg.feature.detect.edge;

import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of connected points along an edge.  Points are in consecutive order.
 *
 * @author Peter Abeles
 */
public class EdgeSegment {
	/** index of this segment in the list.  used for book keeping during construction */
	public int index;

	/** which segment did this spawn from */
	public int parent;
	/** which pixel in the segment did it spawn from */
	public int parentPixel;
	/** List of pixels in this segment */
	public List<Point2D_I32> points = new ArrayList<>();

	public void reset() {
		parent = -1;
		parentPixel = -1;
		points.clear();
	}
}
