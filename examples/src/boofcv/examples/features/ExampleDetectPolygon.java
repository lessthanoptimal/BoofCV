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
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Example of how to use {@link BinaryPolygonConvexDetector} to find polygons in an image.  This algorithm
 * is the basis for several fiducial detectors in BoofCV.  It is designed to detect either white or black polygons.
 * It is highly configurable and can even sparsely fit polygons in a distorted image, meaning
 * the expensive step of undistorting the entire image is not needed.
 *
 * @author Peter Abeles
 */
public class ExampleDetectPolygon {
	public static void main(String[] args) {
		BufferedImage image = UtilImageIO.loadImage("../data/applet/shapes02.png");
//		BufferedImage image = UtilImageIO.loadImage("../data/applet/fiducial/image/examples/image01.jpg");

		ImageUInt8 input = ConvertBufferedImage.convertFromSingle(image, null, ImageUInt8.class);
		ImageUInt8 binary = new ImageUInt8(input.width,input.height);

		ConfigPolygonDetector config = new ConfigPolygonDetector(3,4,7);

		// Tighten the tolerance for what defines a line
		config.contour2Poly_splitDistanceFraction = 0.02;

		BinaryPolygonConvexDetector<ImageUInt8> detector = FactoryShapeDetector.polygon(config, ImageUInt8.class);


		int threshold = GThresholdImageOps.computeOtsu(input,0,255);
		ThresholdImageOps.threshold(input, binary, threshold, true);

		// it takes in a grey scale image and binary image
		// the binary image is used to do a crude polygon git, then the grey image is used to refine the lines
		detector.process(input, binary);

		FastQueue<Polygon2D_F64> found = detector.getFound();
		Graphics2D g2 = image.createGraphics();
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(3));
		for (int i = 0; i < found.size; i++) {
			Polygon2D_F64 poly = found.get(i);

			VisualizeShapes.drawPolygon(poly,true,g2,true);
		}

		ShowImages.showWindow(image,"Found Polygons with 3, 4, and 7 sides",true);
	}
}
