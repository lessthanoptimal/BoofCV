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

package boofcv.examples.features;

import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Example of how to use {@link BinaryPolygonConvexDetector} to find black polygons in an image.  This algorithm
 * is the basis for several fiducial detectors in BoofCV and fits the polygon to sub-pixel accuracy and produces
 * reasonable results on blurred images too.  It is highly configurable and can even sparsely fit polygons
 * in a distorted image.  Meaning the expensive step of undistorting the entire image is not needed.
 *
 * @author Peter Abeles
 */
public class ExampleDetectPolygon {
	public static void main(String[] args) {
		String files[] = new String[]{
				"../data/applet/polygons01.jpg",
				"../data/applet/shapes02.png",
				"../data/applet/fiducial/image/examples/image01.jpg"};

		ConfigPolygonDetector config = new ConfigPolygonDetector(3,4,5,7);

		// Tighten the tolerance for what defines a line
		config.contour2Poly_splitDistanceFraction = 0.02;

		BinaryPolygonConvexDetector<ImageUInt8> detector = FactoryShapeDetector.polygon(config, ImageUInt8.class);

		// Is the input image distorted?  Let the detector sparsely undistort it for improved speed and accuracy
		// detector.setLensDistortion(...blah...);

		ListDisplayPanel panel = new ListDisplayPanel();

		for( String fileName : files ) {
			BufferedImage image = UtilImageIO.loadImage(fileName);

			ImageUInt8 input = ConvertBufferedImage.convertFromSingle(image, null, ImageUInt8.class);
			ImageUInt8 binary = new ImageUInt8(input.width,input.height);

			// Binarization is done outside to allows creative tricks.  For example, when applied to a chessboard
			// pattern where square touch each other, the binary image is eroded first so that they don't touch.
			// The squares are expanded automatically during the subpixel optimization step.
			int threshold = GThresholdImageOps.computeOtsu(input,0,255);
			ThresholdImageOps.threshold(input, binary, threshold, true);

			// it takes in a grey scale image and binary image
			// the binary image is used to do a crude polygon fit, then the grey image is used to refine the lines
			// using a sub-pixel algorithm
			detector.process(input, binary);

			// visualize results by drawing red polygons
			FastQueue<Polygon2D_F64> found = detector.getFound();
			Graphics2D g2 = image.createGraphics();
			g2.setColor(Color.RED);
			g2.setStroke(new BasicStroke(3));
			for (int i = 0; i < found.size; i++) {
				VisualizeShapes.drawPolygon(found.get(i),true,g2,true);
			}

			panel.addImage(image,new File(fileName).getName());
		}

		ShowImages.showWindow(panel,"Found Convex Polygons with 3, 4,5, and 7 sides",true);
	}
}
