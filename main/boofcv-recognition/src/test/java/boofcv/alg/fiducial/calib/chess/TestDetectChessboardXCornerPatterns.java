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

package boofcv.alg.fiducial.calib.chess;

import boofcv.abst.fiducial.calib.ConfigChessboardX;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid.GridInfo;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
class TestDetectChessboardXCornerPatterns extends BoofStandardJUnit {
	@Nested
	class SingleTarget extends GenericDetectFindChessboardCorners {
		@Override
		public List<PointIndex2D_F64> findCorners( int numRows, int numCols, GrayF32 image ) {
			ConfigChessboardX config = new ConfigChessboardX();
			DetectChessboardXCornerPatterns<GrayF32> alg = new DetectChessboardXCornerPatterns<>(config, GrayF32.class);

			alg.findPatterns(image);

			for (GridInfo g : alg.getFoundChessboard().toList()) {
				if (g.rows == numRows - 1 && g.cols == numCols - 1) {
					List<PointIndex2D_F64> found = new ArrayList<>();
					g.nodes.forEach(p -> found.add(new PointIndex2D_F64(p.corner, 0)));
					return found;
				}
			}
			return null;
		}
	}
}
