/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.detect.extract.NonMaxLimiter;
import boofcv.abst.feature.detect.extract.NonMaxLimiter.LocalExtreme;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.feature.detect.selector.SelectLimitTypes;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import org.ddogleg.struct.FastAccess;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Visualization of feature Select Limit. After non-maximum suppression, select limit decides which detected
 * features should be used when the requested number of features is exceeded by the number of detected features.
 * Typically, you either want the most intense features or you want to ensure that features are selected
 * throughout the image.
 *
 * @author Peter Abeles
 */
public class ExampleFeatureLimit {
	// Radius for non-maximum suppression
	public static final int NON_MAX_RADIUS = 5;

	// Maximum number of features it will return
	public static final int MAX_FEATURES = 200;

	public static BufferedImage renderLimit( GrayF32 intensity, SelectLimitTypes type ) {
		// Configure how it will select features inside the intensity image
		var limit = new ConfigSelectLimit(type, 0xBEEF);
		NonMaxLimiter nonmax = FactoryFeatureExtractor.nonmaxLimiter(new ConfigExtract(NON_MAX_RADIUS, 0), limit, MAX_FEATURES);

		// Detect the features
		nonmax.process(intensity);
		FastAccess<LocalExtreme> features = nonmax.getFeatures();

		// Visualize the intensity image
		var output = new BufferedImage(intensity.width, intensity.height, BufferedImage.TYPE_INT_RGB);
		VisualizeImageData.colorizeSign(intensity, output, -1);

		// render each selected maximum with a circle
		Graphics2D g2 = output.createGraphics();
		g2.setColor(Color.blue);
		for (int i = 0; i < features.size(); i++) {
			LocalExtreme c = features.get(i);
			VisualizeFeatures.drawCircle(g2, c.location.x, c.location.y, NON_MAX_RADIUS);
		}
		return output;
	}

	public static void main( String[] args ) {
		BufferedImage buffered = UtilImageIO.loadImageNotNull(UtilIO.pathExample("standard/boat.jpg"));

		GrayF32 input = ConvertBufferedImage.convertFrom(buffered, (GrayF32)null);

		// Compute the image gradient
		GrayF32 derivX = input.createSameShape();
		GrayF32 derivY = input.createSameShape();

		GImageDerivativeOps.gradient(DerivativeType.SOBEL, input, derivX, derivY, BorderType.EXTENDED);

		// From the gradient compute intensity of shi-tomasi features
		GeneralFeatureIntensity<GrayF32, GrayF32> featureIntensity =
				FactoryIntensityPoint.shiTomasi(3, false, GrayF32.class);

		featureIntensity.process(input, derivX, derivY, null, null, null);
		GrayF32 intensity = featureIntensity.getIntensity();

		var panel = new ListDisplayPanel();
		panel.addImage(buffered, "Input Image");

		// Detect maximums with different settings and visualize the results
		for (var type : SelectLimitTypes.values()) {
			panel.addImage(renderLimit(intensity, type), type.name());
		}

		ShowImages.showWindow(panel, "Non-Max with Limiter", true);
	}
}
