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

package boofcv.abst.calib;

import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.*;
import boofcv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class BenchmarkCalibrationDetectors {

	public static final int TEST_TIME = 1000;

	public static ImageFloat32 imageChess;
	public static ImageFloat32 imageSquare;

	public static class Chessboard extends PerformerBase {
		PlanarCalibrationDetector detector = FactoryPlanarCalibrationTarget.
				detectorChessboard(new ConfigChessboard(5,7,30));

		@Override
		public void process() {
			if( !detector.process(imageChess) )
				throw new RuntimeException("Can't find target!");
		}
	}

	public static class Square extends PerformerBase {
		PlanarCalibrationDetector detector = FactoryPlanarCalibrationTarget.
				detectorSquareGrid(new ConfigSquareGrid(3, 4, 30, 30));

		@Override
		public void process() {
			if( !detector.process(imageSquare) )
				throw new RuntimeException("Can't find target!");
		}
	}

	public static ImageFloat32 loadImage(String fileName) {
		BufferedImage img;
		try {
			img = ImageIO.read(new File(fileName));
		} catch (IOException e) {
			return null;
		}

		return ConvertBufferedImage.convertFrom(img, (ImageFloat32) null);
	}

	public static void main(String[] args) {
		String chess = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/left01.jpg");
		String square = UtilIO.pathExample("calibration/stereo/Bumblebee2_Square/left01.jpg");

		imageChess = loadImage(chess);
		imageSquare = loadImage(square);

		ProfileOperation.printOpsPerSec(new Chessboard(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Square(), TEST_TIME);
	}
}
