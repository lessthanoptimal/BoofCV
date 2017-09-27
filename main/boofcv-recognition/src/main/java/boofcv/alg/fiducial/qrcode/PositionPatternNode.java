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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.fiducial.calib.squares.SquareNode;

/**
 * Information for position detection patterns. These are squares. One outer shape that is 1 block think,
 * inner white space 1 block think, then the stone which is 3 blocks think. Total of 7 blocks.
 *
 * Corners in squares must be in CCW order.
 */
public class PositionPatternNode extends SquareNode {

	// threshold for binary classification.
	public double grayThreshold;

	@Override
	public void reset()
	{
		super.reset();
		grayThreshold = -1;
	}
}
