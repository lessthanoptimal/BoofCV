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

package boofcv.struct.distort;

import georegression.struct.point.Point2D_F64;

/**
 * Combines together multiple {@link Point2Transform2_F64} as a sequence into a single transform.
 *
 * @author Peter Abeles
 */
public class SequencePoint2Transform2_F64 implements Point2Transform2_F64 {
	Point2Transform2_F64[] sequence;

	/**
	 * Specifies the sequence of transforms.  Lower indexes are applied first.
	 *
	 * @param sequence Sequence of transforms.
	 */
	public SequencePoint2Transform2_F64(Point2Transform2_F64... sequence) {
		this.sequence = sequence;
	}

	@Override
	public void compute( double x, double y, Point2D_F64 out) {
		sequence[0].compute(x,y,out);
		for( int i = 1; i < sequence.length; i++ ) {
			sequence[i].compute(out.x,out.y,out);
		}
	}
}
