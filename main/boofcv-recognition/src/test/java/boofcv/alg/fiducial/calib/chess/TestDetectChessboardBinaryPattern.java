/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.calib.ConfigChessboardBinary;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.ConfigLength;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

import java.util.List;

/**
 * @author Peter Abeles
 */
class TestDetectChessboardBinaryPattern extends BoofStandardJUnit {

	private static class Base extends GenericDetectFindChessboardCorners {
		boolean localThreshold;

		public Base(boolean localThreshold) {
			this.localThreshold = localThreshold;
		}

		@Override
		public List<PointIndex2D_F64> findCorners( int numRows, int numCols, GrayF32 image) {
			ConfigChessboardBinary configDet = new ConfigChessboardBinary();
//			ConfigGridDimen configChess = new ConfigGridDimen(5, 5, 1);

			DetectPolygonBinaryGrayRefine<GrayF32> detectorSquare =
					FactoryShapeDetector.polygon(configDet.square, GrayF32.class);
//		detectorSquare.setVerbose(true);

			InputToBinary<GrayF32> inputToBinary;
			if( localThreshold ) {
				if( !configDet.thresholding.type.isAdaptive() )
					throw new RuntimeException("This assumes that the default is local. Update unit test by specifying a local");
				inputToBinary = FactoryThresholdBinary.threshold(configDet.thresholding, GrayF32.class);
			} else
				inputToBinary = FactoryThresholdBinary.globalFixed(50,true,GrayF32.class);

			DetectChessboardBinaryPattern<GrayF32> alg = new DetectChessboardBinaryPattern<>(
					numRows, numCols, ConfigLength.fixed(4),detectorSquare,inputToBinary);

			if( alg.process(image) ) {
				return alg.getCalibrationPoints();
			} else {
				return null;
			}
		}
	}

	@Nested
	public class Local extends Base {
		public Local() {
			super(true);
		}
	}

	@Nested
	public class Global extends Base {
		public Global() {
			super(false);
		}
	}
}
