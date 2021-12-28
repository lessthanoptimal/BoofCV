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

package boofcv.examples.features;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.interest.ConfigShiTomasi;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I16;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Example showing how corner features can be detected. These features are not scale invariant, but are
 * fast to compute. In OpenCV Shi-Tomasi has the name of goodFeaturesToTrack and uses the unweighted variant.
 *
 * @author Peter Abeles
 */
public class ExampleCornerFeature {
	public static void main( String[] args ) {
		ConfigGeneralDetector configNonMax = new ConfigGeneralDetector();
		// a large radius is used to exaggerate weighted/unweighted affects. Try 1 or 2 for a typical value
		configNonMax.radius = 10;
		configNonMax.threshold = 100;
		configNonMax.maxFeatures = 100;
		ConfigShiTomasi configCorner = new ConfigShiTomasi();
		configCorner.radius = configNonMax.radius; // in general you should use the same radius here
		configCorner.weighted = true;              // weighted corners will appear at the corners on a chessboard

		// set weighted to false and see what happens to the feature's locations. unweighted is much faster
		GeneralFeatureDetector<GrayU8, GrayS16> detector = FactoryDetectPoint.createShiTomasi(configNonMax, configCorner, GrayS16.class);
		ImageGradient<GrayU8, GrayS16> sobel = FactoryDerivative.sobel(GrayU8.class, GrayS16.class);

		BufferedImage image = UtilImageIO.loadImageNotNull(UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/frame05.jpg"));

		// Convert the image into a usable format and predeclare memory
		GrayU8 gray = ConvertBufferedImage.convertFrom(image, (GrayU8)null);
		GrayS16 derivX = new GrayS16(gray.width, gray.height);
		GrayS16 derivY = new GrayS16(gray.width, gray.height);

		// The first image derivatives are needed
		sobel.process(gray, derivX, derivY);

		// Compute the corners
		detector.process(gray, derivX, derivY, null, null, null);

		// Visualize the results
		QueueCorner corners = detector.getMaximums();
		Graphics2D g2 = image.createGraphics();
		for (int i = 0; i < corners.size; i++) {
			Point2D_I16 c = corners.get(i);
			VisualizeFeatures.drawPoint(g2, c.x, c.y, 4, Color.RED, true);
		}

		ShowImages.showWindow(image, "Corners", true);
	}
}
