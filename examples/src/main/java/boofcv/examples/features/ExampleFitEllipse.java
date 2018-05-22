/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.shapes.FitData;
import boofcv.alg.shapes.ShapeFittingOps;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.curve.EllipseRotated_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Demonstration which fits ellipses to object contours in a binary image.
 *
 * @author Peter Abeles
 */
public class ExampleFitEllipse {

	public static void main( String args[] ) {
		// load and convert the image into a usable format
		BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample("particles01.jpg"));
		GrayF32 input = ConvertBufferedImage.convertFromSingle(image, null, GrayF32.class);

		GrayU8 binary = new GrayU8(input.width,input.height);

		// the mean pixel value is often a reasonable threshold when creating a binary image
		double mean = ImageStatistics.mean(input);

		// create a binary image by thresholding
		ThresholdImageOps.threshold(input, binary, (float) mean, true);

		// reduce noise with some filtering
		GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
		filtered = BinaryImageOps.dilate8(filtered, 1, null);

		// Find the contour around the shapes
		List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT,null);

		// Fit an ellipse to each external contour and draw the results
		Graphics2D g2 = image.createGraphics();
		g2.setStroke(new BasicStroke(3));
		g2.setColor(Color.RED);

		for( Contour c : contours ) {
			FitData<EllipseRotated_F64> ellipse = ShapeFittingOps.fitEllipse_I32(c.external,0,false,null);
			VisualizeShapes.drawEllipse(ellipse.shape, g2);
		}

//		ShowImages.showWindow(VisualizeBinaryData.renderBinary(filtered, false, null),"Binary",true);
		ShowImages.showWindow(image,"Ellipses",true);
	}
}
