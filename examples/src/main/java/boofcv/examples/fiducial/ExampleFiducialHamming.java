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

package boofcv.examples.fiducial;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.factory.fiducial.ConfigHammingMarker;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.factory.fiducial.HammingDictionary;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Hamming fiducials are an entire family of markers/tags which work by identifying unique ID's by minimizing
 * the hamming distance. This family includes ArUco, ArUco 3, AprilTag, and others. Several prebuilt dictionaries
 * are included with BoofCV and you can specify your own easily. Hamming tags have error correction capabilities
 * can are resiliant to noise. How resilient depends on the dictionary. In general the fewer unique IDs available
 * the better it is at error correction. The recommended dictionary is ARUCO_MIP_25h7.
 *
 * See:
 * Aruco https://www.uco.es/investiga/grupos/ava/node/26
 * AprilTag https://april.eecs.umich.edu/software/apriltag
 *
 * @author Peter Abeles
 */
public class ExampleFiducialHamming {
	public static void main( String[] args ) {
		String directory = UtilIO.pathExample("fiducial/square_hamming/aruco_25h7");

		// load the lens distortion parameters and the input image
		CameraPinholeBrown param = CalibrationIO.load(new File(directory, "intrinsic.yaml"));
		LensDistortionNarrowFOV lensDistortion = new LensDistortionBrown(param);

		// You need to create a different configuration for each dictionary type
		ConfigHammingMarker configMarker = ConfigHammingMarker.loadDictionary(HammingDictionary.ARUCO_MIP_25h7);
		FiducialDetector<GrayF32> detector = FactoryFiducial.squareHamming(configMarker, /*detector*/null, GrayF32.class);

		// Provide it lens parameters so that a 3D pose estimate is possible
		detector.setLensDistortion(lensDistortion, param.width, param.height);

		// Load and process all example images
		ListDisplayPanel gui = new ListDisplayPanel();
		for (int imageID = 1; imageID <= 3; imageID++) {
			String name = String.format("image%02d.jpg", imageID);
			System.out.println("processing: " + name);

			// Load the image
			BufferedImage buffered = UtilImageIO.loadImageNotNull(new File(directory, name).getPath());

			// Convert to a BoofCV format
			GrayF32 input = ConvertBufferedImage.convertFrom(buffered, (GrayF32)null);

			// Run the detector
			detector.detect(input);

			// Render a 3D compute on top of all detections
			Graphics2D g2 = buffered.createGraphics();
			Se3_F64 targetToSensor = new Se3_F64();
			Point2D_F64 locationPixel = new Point2D_F64();
			Polygon2D_F64 bounds = new Polygon2D_F64();
			for (int i = 0; i < detector.totalFound(); i++) {
				detector.getCenter(i, locationPixel);
				detector.getBounds(i, bounds);

				g2.setColor(new Color(50, 50, 255));
				g2.setStroke(new BasicStroke(10));
				VisualizeShapes.drawPolygon(bounds, true, 1.0, g2);

				if (detector.hasID())
					System.out.println("Target ID = " + detector.getId(i));
				if (detector.hasMessage())
					System.out.println("Message   = " + detector.getMessage(i));
				System.out.println("2D Image Location = " + locationPixel);

				if (detector.is3D()) {
					detector.getFiducialToCamera(i, targetToSensor);
					System.out.println("3D Location:");
					System.out.println(targetToSensor);
					VisualizeFiducial.drawCube(targetToSensor, param, detector.getWidth(i), 3, g2);
					VisualizeFiducial.drawLabelCenter(targetToSensor, param, "" + detector.getId(i), g2);
				} else {
					VisualizeFiducial.drawLabel(locationPixel, "" + detector.getId(i), g2);
				}
			}
			gui.addImage(buffered, name, ScaleOptions.ALL);
		}
		ShowImages.showWindow(gui, "Example Fiducial Hamming", true);
	}
}
