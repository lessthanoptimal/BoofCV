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

package boofcv.alg.feature.detect.chess;

import org.ddogleg.nn.alg.KdTreeDistance;

/**
 * Corner distance for use in {@link org.ddogleg.nn.NearestNeighbor} searches
 *
 * @author Peter Abeles
 */
public class ChessboardCornerDistance implements KdTreeDistance<ChessboardCorner> {
	@Override
	public double distance( ChessboardCorner a, ChessboardCorner b ) {
		return a.distance2(b);
	}

	@Override
	public double valueAt( ChessboardCorner point, int index ) {
		return switch (index) {
			case 0 -> point.x;
			case 1 -> point.y;
			default -> throw new RuntimeException("Out of bounds");
		};
	}

	@Override
	public int length() {
		return 2;
	}
}
