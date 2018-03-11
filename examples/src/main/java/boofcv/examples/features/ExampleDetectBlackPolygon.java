/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.features;

import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.alg.shapes.polygon.DetectPolygonFromContour;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Polygon2D_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Example of how to use {@link DetectPolygonFromContour} to find black polygons in an image.  This algorithm
 * is the basis for several fiducial detectors in BoofCV and fits the polygon to sub-pixel accuracy and produces
 * reasonable results on blurred images too.  It is highly configurable and can even sparsely fit polygons
 * in a distorted image.  Meaning the expensive step of undistorting the entire image is not needed.
 *
 * @author Peter Abeles
 */
public class ExampleDetectBlackPolygon {
	public static void main(String[] args) {
		String imagesConvex[] = new String[]{
				"shapes/polygons01.jpg",
				"shapes/shapes02.png",
				"fiducial/image/examples/image01.jpg"};

		String imagesConcave[] = new String[]{
				"shapes/concave01.jpg"};

		ListDisplayPanel panel = new ListDisplayPanel();

		// first configure the detector to only detect convex shapes with 3 to 7 sides
		ConfigPolygonDetector config = new ConfigPolygonDetector(3,7);
		DetectPolygonBinaryGrayRefine<GrayU8> detector = FactoryShapeDetector.polygon(config, GrayU8.class);

		processImages(imagesConvex, detector, panel);

		// now lets detect concave shapes with many sides
		config.detector.contourToPoly.maximumSides = 12;
		config.detector.contourToPoly.convex = false;
		detector = FactoryShapeDetector.polygon(config, GrayU8.class);

		processImages(imagesConcave, detector, panel);

		ShowImages.showWindow(panel,"Found Polygons",true);
	}

	private static void processImages(String[] files,
									  DetectPolygonBinaryGrayRefine<GrayU8> detector,
									  ListDisplayPanel panel)
	{
		for( String fileName : files ) {
			BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample(fileName));

			GrayU8 input = ConvertBufferedImage.convertFromSingle(image, null, GrayU8.class);
			GrayU8 binary = new GrayU8(input.width,input.height);

			// Binarization is done outside to allows creative tricks.  For example, when applied to a chessboard
			// pattern where square touch each other, the binary image is eroded first so that they don't touch.
			// The squares are expanded automatically during the subpixel optimization step.
			int threshold = (int)GThresholdImageOps.computeOtsu(input, 0, 255);
			ThresholdImageOps.threshold(input, binary, threshold, true);

			// it takes in a grey scale image and binary image
			// the binary image is used to do a crude polygon fit, then the grey image is used to refine the lines
			// using a sub-pixel algorithm
			detector.process(input, binary);

			// visualize results by drawing red polygons
			java.util.List<Polygon2D_F64> found = detector.getPolygons(null,null);
			Graphics2D g2 = image.createGraphics();
			g2.setStroke(new BasicStroke(3));
			for (int i = 0; i < found.size(); i++) {
				g2.setColor(Color.RED);
				VisualizeShapes.drawPolygon(found.get(i), true, g2, true);
				g2.setColor(Color.CYAN);
				VisualizeShapes.drawPolygonCorners(found.get(i), 2, g2, true);
			}

			panel.addImage(image,new File(fileName).getName());
		}
	}
}
