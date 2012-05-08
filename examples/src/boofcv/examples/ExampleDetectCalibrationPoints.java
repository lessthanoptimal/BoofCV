/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.examples;

import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Example that demonstrates how to detect targets used for camera calibration.  Features are detected
 * inside these targets to a high level of precision.  It is assumed that a single image
 * only shows a single target and that the entire target is visible.  If these conditions are not meet
 * the detector is likely to fail.
 *
 * @author Peter Abeles
 */
public class ExampleDetectCalibrationPoints {

	public static void main( String args[] ) {

		// load the test image
//		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Square";
		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Chess";

		BufferedImage orig = UtilImageIO.loadImage(directory+"/left01.jpg");
		ImageFloat32 input = ConvertBufferedImage.convertFrom(orig,(ImageFloat32)null);

		// To select different types of detectors add or remove comments below
		PlanarCalibrationDetector detector;

		// For chessboard targets, tune RADIUS parameter for your images
//		detector = FactoryPlanarCalibrationTarget.detectorSquareGrid( 3, 4);
		detector = FactoryPlanarCalibrationTarget.detectorChessboard( 3, 4, 6);

		// process the image and check for failure condition
		if( !detector.process(input) )
			throw new RuntimeException("Target detection failed!");

		// Ordered observations of calibration points on the target
		List<Point2D_F64> points = detector.getPoints();

		// render and display the results
		Graphics2D g2 = orig.createGraphics();
		for( Point2D_F64 p : points )
			VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,3,Color.RED);

		ShowImages.showWindow(orig,"Calibration Points");
	}
}
