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

import boofcv.abst.fiducial.Uchiya_to_FiducialDetector;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.factory.fiducial.ConfigUchiyaMarker;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.fiducial.FiducialIO;
import boofcv.io.fiducial.RandomDotDefinition;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Random dot markers are exactly what their name implies. Each marker is a set of random dots that the tracker
 * learns to identify using hash codes computed from geometric invariants which describe the relationships between
 * the dots. These markers are based off of Uchiya Markers.
 *
 * @author Peter Abeles
 */
public class ExampleFiducialRandomDots {
	public static void main( String[] args ) {
		// The definitions file specifies where dots are on each marker and other bits of meta data
		RandomDotDefinition defs = FiducialIO.loadRandomDotYaml(
				UtilIO.fileExample("fiducial/random_dots/descriptions.yaml"));

		// The only parameter that you have to set is markerLength. It's used to compute bounding
		// boxes and similar. If you don't know what the width is just set it to 1.0
		ConfigUchiyaMarker config = new ConfigUchiyaMarker();
		config.markerWidth = defs.markerWidth;
		config.markerHeight = defs.markerHeight;

		Uchiya_to_FiducialDetector<GrayU8> detector = FactoryFiducial.randomDots(config, GrayU8.class);

		// Load / learn all the markers. This can take a few seconds if there are a lot of markers
		for (List<Point2D_F64> marker : defs.markers) {
			detector.addMarker(marker);
		}

		// If you want 3D pose information then the camera need sto be calibrated. If you don't provide
		// this information then just things like the bounding box will be returned
		CameraPinholeBrown intrinsic = CalibrationIO.load(
				UtilIO.fileExample("fiducial/random_dots/intrinsic.yaml"));
		detector.setLensDistortion(LensDistortionFactory.narrow(intrinsic), intrinsic.width, intrinsic.height);

		// It's now ready to start processing images. Let's load an image
		BufferedImage image = UtilImageIO.loadImageNotNull(UtilIO.pathExample("fiducial/random_dots/image02.jpg"));
		GrayU8 gray = ConvertBufferedImage.convertFrom(image, false, ImageType.SB_U8);

		detector.detect(gray);

		// Time to visualize the results
		Graphics2D g2 = image.createGraphics();
		Se3_F64 targetToSensor = new Se3_F64();
		Polygon2D_F64 bounds = new Polygon2D_F64();
		Point2D_F64 center = new Point2D_F64();
		for (int i = 0; i < detector.totalFound(); i++) {
			detector.getBounds(i, bounds);
			detector.getCenter(i, center);

			g2.setColor(new Color(50, 50, 255));
			g2.setStroke(new BasicStroke(10));
			VisualizeShapes.drawPolygon(bounds, true, 1.0, g2);
			VisualizeFiducial.drawLabel(center, "" + detector.getId(i), g2);

			System.out.println("Target ID = " + detector.getId(i));

			if (detector.is3D()) {
				detector.getFiducialToCamera(i, targetToSensor);
				VisualizeFiducial.drawCube(targetToSensor, intrinsic, detector.getWidth(i), 3, g2);
			}
		}
		ShowImages.showWindow(image, "Random Dot Markers", true);
	}
}
