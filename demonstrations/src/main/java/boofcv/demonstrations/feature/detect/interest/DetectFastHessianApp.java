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

package boofcv.demonstrations.feature.detect.interest;

import boofcv.BoofDefaults;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Displays a window showing the selected corner-laplace features across diffferent scale spaces.
 *
 * @author Peter Abeles
 */
public class DetectFastHessianApp {

	//	static String fileName = UtilIO.pathExample("outdoors01.jpg");
	static String fileName = UtilIO.pathExample("sunflowers.jpg");
//	static String fileName = UtilIO.pathExample("particles01.jpg");
//	static String fileName = UtilIO.pathExample("scale/beach02.jpg");
//	static String fileName = UtilIO.pathExample("indoors01.jpg");
//	static String fileName = UtilIO.pathExample("shapes/shapes01.png");
//	static String fileName = UtilIO.pathExample("scale/mountain_4p2mm.jpg");
//	static String fileName = UtilIO.pathExample("scale/mountain_7p1mm.jpg");
//	static String fileName = UtilIO.pathExample("scale/mountain_19p9mm.jpg");

	static int NUM_FEATURES = 120;

	private static <T extends ImageGray<T>> void doStuff( Class<T> imageType, BufferedImage input ) {
		T workImage = ConvertBufferedImage.convertFromSingle(input, null, imageType);

		ConfigFastHessian config = new ConfigFastHessian();
		config.extract = new ConfigExtract(5, 1, 5, true);
		config.maxFeaturesPerScale = 200;
		config.maxFeaturesAll = 400;
		config.initialSampleStep = 2;

		FastHessianFeatureDetector<T> det = FactoryInterestPointAlgs.fastHessian(config);

		T integral = GIntegralImageOps.transform(workImage, null);
		det.detect(integral);

		System.out.println("total features found: " + det.getFoundFeatures().size());

		VisualizeFeatures.drawScalePoints(input.createGraphics(), det.getFoundFeatures(),
				BoofDefaults.SURF_SCALE_TO_RADIUS);

		ShowImages.showWindow(input, "Found Features: " + imageType.getSimpleName(), true);
	}

	public static void main( String[] args ) {
		BufferedImage input = Objects.requireNonNull(UtilImageIO.loadImage(fileName));

		doStuff(GrayF32.class, input);
//		doStuff(GrayU8.class,input);
	}
}
