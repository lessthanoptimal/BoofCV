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

package boofcv.abst.fiducial.calib;

import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkCalibrationDetectors {

	public static GrayF32 imageChess;
	public static GrayF32 imageSquare;

	DetectSingleFiducialCalibration chessboardB = FactoryFiducialCalibration.
			chessboardB((ConfigChessboardBinary)null,new ConfigGridDimen(7, 5, 30));
	DetectSingleFiducialCalibration chessboardX = FactoryFiducialCalibration.
			chessboardX(null,new ConfigGridDimen(7, 5, 30));
	DetectSingleFiducialCalibration squareGrid = FactoryFiducialCalibration.
			squareGrid(new ConfigSquareGrid(),new ConfigGridDimen(4, 3, 30, 30));

	@Setup public void setup() {
		String chess = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/left01.jpg");
		String square = UtilIO.pathExample("calibration/stereo/Bumblebee2_Square/left01.jpg");

		imageChess = loadImage(chess);
		imageSquare = loadImage(square);
	}

	@Benchmark public void ChessboardBinary() {
		if( !chessboardB.process(imageChess) )
			throw new RuntimeException("Can't find target!");
	}

	@Benchmark public void ChessboardXCorner() {
		if( !chessboardX.process(imageChess) )
			throw new RuntimeException("Can't find target!");
	}

	@Benchmark public void Square() {
		if( !squareGrid.process(imageSquare) )
			throw new RuntimeException("Can't find target!");
	}

	public static GrayF32 loadImage(String fileName) {
		BufferedImage img;
		try {
			img = ImageIO.read(new File(fileName));
		} catch (IOException e) {
			return null;
		}

		return ConvertBufferedImage.convertFrom(img, (GrayF32) null);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkCalibrationDetectors.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
