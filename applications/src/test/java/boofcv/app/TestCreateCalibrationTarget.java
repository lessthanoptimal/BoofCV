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

package boofcv.app;

import boofcv.abst.fiducial.calib.CalibrationDetectorChessboardX;
import boofcv.abst.fiducial.calib.CalibrationDetectorCircleHexagonalGrid;
import boofcv.abst.fiducial.calib.CalibrationDetectorSquareGrid;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the create calibration target application. Invokes the application to create a target. Then converts
 * the postscript document into an image. After the image has been generated it is then processed by the fiducial
 * detector.
 *
 * @author Peter Abeles
 */
public class TestCreateCalibrationTarget extends CommonFiducialPdfChecks {

	public void createDocument( String args ) {
		// suppress stdout
		out.out = new PrintStream(new OutputStream(){@Override public void write( int b ){}});
		CreateCalibrationTarget.main(args.split("\\s+"));
		out.used = false; // this will ignore the stdout usage which is unavoidable
		err.used = false;
	}

	@Test void chessboard() throws IOException {
		createDocument("-r 7 -c 5 -o target -t CHESSBOARD -u cm -w 3 -p LETTER");
		BufferedImage image = loadPDF();

		GrayF32 gray = new GrayF32(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, gray);

		CalibrationDetectorChessboardX detector =
				FactoryFiducialCalibration.chessboardX(null, new ConfigGridDimen(7, 5, 3));

		assertTrue(detector.process(gray));
	}

	@Test void square_grid() throws IOException {
		createDocument("-r 4 -c 3 -o target -t SQUARE_GRID -u cm -w 3 -s 3 -p LETTER\n");
		BufferedImage image = loadPDF();

		GrayF32 gray = new GrayF32(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, gray);

		CalibrationDetectorSquareGrid detector =
				FactoryFiducialCalibration.squareGrid(null, new ConfigGridDimen(4, 3, 3, 3));

		assertTrue(detector.process(gray));
	}

	@Test void circle_hexagonal() throws IOException {
		createDocument("-r 8 -c 7 -o target -t CIRCLE_HEXAGONAL -u cm -w 2 -d 3 -p LETTER");
		BufferedImage image = loadPDF();

		GrayF32 gray = new GrayF32(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, gray);

		CalibrationDetectorCircleHexagonalGrid detector =
				FactoryFiducialCalibration.circleHexagonalGrid(null, new ConfigGridDimen(8, 7, 2, 3));

		assertTrue(detector.process(gray));
	}

	@Test void circle_regular() throws IOException {
		createDocument("-r 8 -c 6 -o target -t CIRCLE_GRID -u cm -w 2 -d 3 -p LETTER");
		BufferedImage image = loadPDF();

		GrayF32 gray = new GrayF32(image.getWidth(), image.getHeight());
		ConvertBufferedImage.convertFrom(image, gray);

		DetectSingleFiducialCalibration detector =
				FactoryFiducialCalibration.circleRegularGrid(null, new ConfigGridDimen(8, 6, 2, 3));

		assertTrue(detector.process(gray));
	}
}
