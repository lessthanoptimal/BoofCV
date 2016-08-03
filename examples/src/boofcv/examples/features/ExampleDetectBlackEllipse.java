/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.EllipseRotated_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * TODO write
 *
 * @author Peter Abeles
 */
public class ExampleDetectBlackEllipse {
	public static void main(String[] args) {
		String images[] = new String[]{
				"shapes/polygons01.jpg",
				"shapes/shapes02.png",
				"shapes/concave01.jpg",
				"fiducial/image/examples/image01.jpg"};
				// TODO add ellipse fiducial

		ListDisplayPanel panel = new ListDisplayPanel();

		BinaryEllipseDetector<GrayU8> detector = FactoryShapeDetector.ellipse(null, GrayU8.class);

		for( String fileName : images ) {
			BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample(fileName));

			GrayU8 input = ConvertBufferedImage.convertFromSingle(image, null, GrayU8.class);
			GrayU8 binary = new GrayU8(input.width,input.height);

			// Binarization is done outside to allows creative tricks.  For example, when applied to a chessboard
			// pattern where square touch each other, the binary image is eroded first so that they don't touch.
			// The squares are expanded automatically during the subpixel optimization step.
			int threshold = GThresholdImageOps.computeOtsu(input, 0, 255);
			ThresholdImageOps.threshold(input, binary, threshold, true);

			// it takes in a grey scale image and binary image
			// the binary image is used to do a crude polygon fit, then the grey image is used to refine the lines
			// using a sub-pixel algorithm
			detector.process(input, binary);

			// visualize results by drawing red polygons
			FastQueue<EllipseRotated_F64> found = detector.getFoundEllipses();
			Graphics2D g2 = image.createGraphics();
			g2.setStroke(new BasicStroke(3));
			g2.setColor(Color.RED);
			for (int i = 0; i < found.size; i++) {
				VisualizeShapes.drawEllipse(found.get(i), g2);
			}

			panel.addImage(image,new File(fileName).getName());
		}

		ShowImages.showWindow(panel,"Detected Ellipses",true);
	}
}
