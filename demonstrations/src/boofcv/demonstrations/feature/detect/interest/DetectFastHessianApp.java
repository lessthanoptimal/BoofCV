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

package boofcv.demonstrations.feature.detect.interest;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.BoofDefaults;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

import java.awt.image.BufferedImage;

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

	private static <T extends ImageGray> void doStuff(Class<T> imageType , BufferedImage input ) {
		T workImage = ConvertBufferedImage.convertFromSingle(input, null, imageType);

		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract( 5 , 1 , 5, true) );
		FastHessianFeatureDetector<T> det = new FastHessianFeatureDetector<T>(extractor,NUM_FEATURES, 2, 9,4,4, 6);

		T integral = GIntegralImageOps.transform(workImage,null);
		det.detect(integral);

		System.out.println("total features found: "+det.getFoundPoints().size());

		VisualizeFeatures.drawScalePoints(input.createGraphics(),det.getFoundPoints(),
				BoofDefaults.SURF_SCALE_TO_RADIUS);

		ShowImages.showWindow(input,"Found Features: "+imageType.getSimpleName(),true);
	}

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);

		doStuff(GrayF32.class,input);
//		doStuff(ImageUInt8.class,input);
	}
}
