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

package boofcv.examples.calibration;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Example that demonstrates how to detect calibration targets.  Calibration points are found on the
 * targets to a high level of precision.  It is assumed that a single image only shows a single target
 * and that the entire target is visible.  If these conditions are not meet then the target is likely
 * to not be detected.
 *
 * @author Peter Abeles
 */
public class ExampleDetectCalibrationPoints {

	public static void main( String args[] ) {

		// load the test image
//		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Square");
		String directory = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess");

		BufferedImage orig = UtilImageIO.loadImage(directory+"/left01.jpg");
		GrayF32 input = ConvertBufferedImage.convertFrom(orig,(GrayF32)null);

		// To select different types of detectors add or remove comments below
		DetectorFiducialCalibration detector;

		// For chessboard targets, tune RADIUS parameter for your images
//		detector = FactoryCalibrationTarget.squareGrid(new ConfigSquareGrid(4, 3, 30, 30));
		detector = FactoryFiducialCalibration.chessboard( new ConfigChessboard(7, 5, 30));

		// process the image and check for failure condition
		if( !detector.process(input) )
			throw new RuntimeException("Target detection failed!");

		// Ordered observations of calibration points on the target
		CalibrationObservation set = detector.getDetectedPoints();

		// render and display the results
		Graphics2D g2 = orig.createGraphics();
		for( PointIndex2D_F64 p : set.points )
			VisualizeFeatures.drawPoint(g2,p.x,p.y,3,Color.RED, true);

		ShowImages.showWindow(orig,"Calibration Points", true);
	}
}
