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

package boofcv.examples.calibration;

import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.alg.geo.calibration.CalibrationPlanarGridZhang99;
import boofcv.alg.geo.calibration.Zhang99ParamAll;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Example of how to calibrate a single (monocular) camera given a set of observed calibration.
 * points using the Zhang99 algorithm.  Unlike in the other examples, the image processing has
 * not already been done.  In theory you could add your own more exotic targets by writing
 * custom code to detect the target, which is left as an exercise for the reader.  For example,
 * have multiple targets visible or calibrate using circles.
 *
 * @author Peter Abeles
 */
public class ExampleCalibrateMonocularPoints {

	/**
	 * Given a description of the calibration target and the observed location of the calibration
	 *
	 * @param layout How calibration points are laid out on the board
	 * @param observations Observations of the target in different images
	 */
	public static void calibrate( List<Point2D_F64> layout,
								  List<List<Point2D_F64>> observations ) {

		// Assume zero skew and model lens distortion with two radial parameters
		CalibrationPlanarGridZhang99 zhang99 =
				new CalibrationPlanarGridZhang99(layout,true,2,false);

		if( !zhang99.process(observations) )
			throw new RuntimeException("Calibration failed!");

		// Get camera parameters and extrinsic target location in each image
		Zhang99ParamAll found = zhang99.getOptimized();

		// Convenient function for converting from specialized Zhang99 format to generalized
		IntrinsicParameters param = found.convertToIntrinsic();

		// print the results to standard out
		param.print();
		// save to a file using XML
		UtilIO.saveXML(param, "intrinsic.xml");
	}

	/**
	 * Detects calibration points found in several images and returned as a list. Not the focus of this example.
	 */
	public static List<List<Point2D_F64>> loadObservations( PlanarCalibrationDetector detector ) {

		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Chess";
		List<String> imageNames = BoofMiscOps.directoryList(directory, "left");

		List<List<Point2D_F64>> ret = new ArrayList<List<Point2D_F64>>();

		for( String n : imageNames ) {
			BufferedImage img = UtilImageIO.loadImage(n);
			ImageFloat32 input = ConvertBufferedImage.convertFrom(img,(ImageFloat32)null);

			if( !detector.process(input) )
				throw new RuntimeException("Detection failed!");

			ret.add(detector.getDetectedPoints());
		}

		return ret;
	}

	public static void main( String args[] ) {
		PlanarCalibrationDetector detector = FactoryPlanarCalibrationTarget.
				detectorChessboard(new ConfigChessboard(5, 7, 30));

		List<List<Point2D_F64>> calibPts = loadObservations(detector);

		calibrate(detector.getLayout(),calibPts);
	}
}
