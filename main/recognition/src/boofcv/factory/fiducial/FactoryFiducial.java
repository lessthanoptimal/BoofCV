/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.fiducial;

import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.ConfigSquareGrid;
import boofcv.abst.fiducial.CalibrationFiducialDetector;
import boofcv.abst.fiducial.FiducialDetector;
import boofcv.abst.fiducial.SquareBinary_to_FiducialDetector;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.feature.shapes.SplitMergeLineFitLoop;
import boofcv.alg.fiducial.DetectFiducialSquareBinary;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.struct.image.ImageSingleBand;

/**
 * @author Peter Abeles
 */
// TODO debugging.  Show all detected quads in an image
// TODO add calibration targets here
public class FactoryFiducial {

	public static <T extends ImageSingleBand>
	FiducialDetector<T> squareBinaryFast( double targetWidth,
										  int binaryThreshold ,
										  int borderTolerance ,
										  int borderMaxIterations ,
										  Class<T> imageType ) {

		InputToBinary<T> binary = FactoryThresholdBinary.globalFixed(binaryThreshold,true,imageType);

		SplitMergeLineFitLoop poly = new SplitMergeLineFitLoop(borderTolerance,0.05,borderMaxIterations );
		DetectFiducialSquareBinary<T> alg = new DetectFiducialSquareBinary<T>(binary,poly,imageType);
		alg.setTargetShape(targetWidth);

		return new SquareBinary_to_FiducialDetector<T>(alg);
	}

	public static  <T extends ImageSingleBand>
	FiducialDetector<T> squareBinaryRobust( double targetWidth,
											int thresholdRadius,
											int borderTolerance ,
											int borderMaxIterations ,
											Class<T> imageType ) {
		InputToBinary<T> binary = FactoryThresholdBinary.adaptiveSquare(thresholdRadius, 0, true, imageType);

		SplitMergeLineFitLoop poly = new SplitMergeLineFitLoop(borderTolerance,0.05,borderMaxIterations );
		DetectFiducialSquareBinary<T> alg = new DetectFiducialSquareBinary<T>(binary,poly,imageType);
		alg.setTargetShape(targetWidth);

		return new SquareBinary_to_FiducialDetector<T>(alg);
	}

	public static <T extends ImageSingleBand>
	CalibrationFiducialDetector<T> calibChessboard( ConfigChessboard config, double sizeOfSquares,Class<T> imageType) {
		return new CalibrationFiducialDetector<T>(config,sizeOfSquares,imageType);
	}

	public static <T extends ImageSingleBand>
	CalibrationFiducialDetector<T> calibSquareGrid( ConfigSquareGrid config, double sizeOfSquares,Class<T> imageType) {
		return new CalibrationFiducialDetector<T>(config,sizeOfSquares,imageType);
	}
}
