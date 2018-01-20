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

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_I16;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Non-maximum suppression is used to identify local maximums and/or minimums in an image feature intensity map.  This
 * is a common step in feature detection.  BoofCV includes an implementation of non-maximum suppression which is much
 * faster than the naive algorithm that is often used because of its ease of implementation.  The following code
 * demonstrates how some of the tuning parameters affects the final output.
 *
 * @author Peter Abeles
 */
public class ExampleNonMaximumSupression {

	public static BufferedImage renderNonMax( GrayF32 intensity, int radius , float threshold) {
		// Create and configure the feature detector
		NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(new ConfigExtract(radius, threshold ));

		// We will only searching for the maximums.  Other variants will look for minimums or will exclude previous
		// candidate detections from being detected twice
		QueueCorner maximums = new QueueCorner();
		nonmax.process(intensity, null, null, null, maximums );

		// Visualize the intensity image
		BufferedImage output = new BufferedImage(intensity.width,intensity.height, BufferedImage.TYPE_INT_RGB);
		VisualizeImageData.colorizeSign(intensity, output, -1);

		// render each maximum with a circle
		Graphics2D g2 = output.createGraphics();
		g2.setColor(Color.blue);
		for (int i = 0; i < maximums.size(); i++) {
			Point2D_I16 c = maximums.get(i);
			VisualizeFeatures.drawCircle(g2, c.x, c.y, radius);
		}
		return output;
	}

	public static void main(String[] args) {

		BufferedImage buffered = UtilImageIO.loadImage(UtilIO.pathExample("standard/boat.jpg"));

		GrayF32 input = ConvertBufferedImage.convertFrom(buffered, (GrayF32)null);

		// Compute the image gradient
		GrayF32 derivX = input.createSameShape();
		GrayF32 derivY = input.createSameShape();

		GImageDerivativeOps.gradient(DerivativeType.SOBEL, input, derivX, derivY, BorderType.EXTENDED);

		// From the gradient compute intensity of shi-tomasi features
		GeneralFeatureIntensity<GrayF32,GrayF32> featureIntensity =
				FactoryIntensityPoint.shiTomasi(3,false, GrayF32.class);

		featureIntensity.process(input, derivX, derivY, null, null , null);
		GrayF32 intensity = featureIntensity.getIntensity();

		ListDisplayPanel panel = new ListDisplayPanel();
		panel.addImage(buffered, "Input Image");
		// hack to just show intensity - no features can be detected
		panel.addImage(renderNonMax(intensity, 10, Float.MAX_VALUE),  "Intensity Image");

		// Detect maximums with different settings and visualize the results
		panel.addImage(renderNonMax(intensity, 3, -Float.MAX_VALUE),  "Radius 3");
		panel.addImage(renderNonMax(intensity, 3, 30000),    "Radius 3  threshold");
		panel.addImage(renderNonMax(intensity, 20, -Float.MAX_VALUE), "Radius 10");
		panel.addImage(renderNonMax(intensity, 20, 30000),   "Radius 10 threshold");

		ShowImages.showWindow(panel, "Non-Maximum Suppression", true);
	}
}
