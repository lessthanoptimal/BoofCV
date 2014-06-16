/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.calib;

import boofcv.alg.feature.detect.chess.DetectChessCalibrationPoints;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * Wrapper around {@link DetectChessCalibrationPoints} for {@link PlanarCalibrationDetector}
 * 
 * @author Peter Abeles
 */
public class WrapPlanarChessTarget implements PlanarCalibrationDetector {

	DetectChessCalibrationPoints<ImageFloat32,ImageFloat32> alg;

	public WrapPlanarChessTarget(ConfigChessboard config ) {
		alg = new DetectChessCalibrationPoints<ImageFloat32, ImageFloat32>(
				config.numCols,config.numRows,config.nonmaxRadius,
				config.relativeSizeThreshold,ImageFloat32.class);
		alg.setUserBinaryThreshold(config.binaryGlobalThreshold);
		alg.setUserAdaptiveRadius(config.binaryAdaptiveRadius);
		alg.setUserAdaptiveBias(config.binaryAdaptiveBias);
	}

	@Override
	public boolean process(ImageFloat32 input) {
		return alg.process(input);
	}

	@Override
	public List<Point2D_F64> getPoints() {
		// points should be at sub-pixel accuracy and in the correct orientation
		return alg.getPoints();
	}

	public DetectChessCalibrationPoints<ImageFloat32, ImageFloat32> getAlg() {
		return alg;
	}
}
