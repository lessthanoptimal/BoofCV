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

package boofcv.app;

import boofcv.alg.geo.calibration.FactoryPlanarCalibrationTarget;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * Application for calibrating stereo images.  See {@link CalibrateStereoPlanar} for details on calibration
 * procedures.
 * </p>
 *
 *
 * @author Peter Abeles
 */
public class CalibrateStereoPlanarApp {

	public static void main( String args[] ) {
//		PlanarCalibrationDetector detector = new WrapPlanarGridTarget(3,4);
		PlanarCalibrationDetector detector = new WrapPlanarChessTarget(3,4,6);

//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(3, 4, 30,30);
//		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(8, 8, 1, 7 / 18);
		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridChess(3, 4, 30);

		CalibrateStereoPlanar app = new CalibrateStereoPlanar(detector,true);

//		app.reset();
		app.configure(target,false,2);

		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Chess";
//		String directory = "../data/evaluation/calibration/stereo/Bumblebee2_Square";

		List<String> left = CalibrateMonoPlanarApp.directoryList(directory, "left");
		List<String> right = CalibrateMonoPlanarApp.directoryList(directory,"right");

		// ensure the lists are in the same order
		Collections.sort(left);
		Collections.sort(right);

		for( int i = 0; i < left.size(); i++ ) {
			BufferedImage l = UtilImageIO.loadImage(left.get(i));
			BufferedImage r = UtilImageIO.loadImage(right.get(i));

			ImageFloat32 imageLeft = ConvertBufferedImage.convertFrom(l,(ImageFloat32)null);
			ImageFloat32 imageRight = ConvertBufferedImage.convertFrom(r,(ImageFloat32)null);

			app.addPair(imageLeft,imageRight);
		}
		StereoParameters stereoCalib = app.process();

		// save results to a file and print out
		BoofMiscOps.saveXML(stereoCalib, "stereo.xml");
		stereoCalib.print();
	}
}
