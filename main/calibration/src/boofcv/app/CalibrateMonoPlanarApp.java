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
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Application that calibrates a single camera.
 * </p>
 *
 * @author Peter Abeles
 */
public class CalibrateMonoPlanarApp {


	public static List<String> directoryList( String directory , String prefix ) {
		List<String> ret = new ArrayList<String>();

		File d = new File(directory);

		if( !d.isDirectory() )
			throw new IllegalArgumentException("Must specify an directory");

		File files[] = d.listFiles();

		for( File f : files ) {
			if( f.isDirectory() || f.isHidden() )
				continue;

			if( f.getName().contains(prefix )) {
				ret.add(f.getAbsolutePath());
			}
		}

		return ret;
	}

	public static void main( String args[] ) {
//		PlanarCalibrationDetector detector = new WrapPlanarGridTarget();
		PlanarCalibrationDetector detector = new WrapPlanarChessTarget(8,8,4);

		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(8, 8, 1, 7 / 18);

		CalibrateMonoPlanar app = new CalibrateMonoPlanar(detector);

		app.reset();
		app.configure(target,false,2);

		String directory = "../data/evaluation/calibration/mono/Sony_DSC-HX5V";

		List<String> images = directoryList(directory,"image");
		
		for( String n : images ) {
			BufferedImage input = UtilImageIO.loadImage(n);
			if( n != null ) {
				ImageFloat32 image = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);
				app.addImage(image);
			}
		}
		IntrinsicParameters intrinsic = app.process();

		// save results to a file and print out
		BoofMiscOps.saveXML(intrinsic,"intrinsic.xml");
		intrinsic.print();
	}
}
