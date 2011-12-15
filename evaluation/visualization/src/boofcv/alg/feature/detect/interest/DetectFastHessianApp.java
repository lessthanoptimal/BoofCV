/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.BoofDefaults;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;

import java.awt.image.BufferedImage;

/**
 * Displays a window showing the selected corner-laplace features across diffferent scale spaces.
 *
 * @author Peter Abeles
 */
public class DetectFastHessianApp {

//	static String fileName = "evaluation/data/outdoors01.jpg";
	static String fileName = "data/sunflowers.png";
//	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
//	static String fileName = "evaluation/data/indoors01.jpg";
//	static String fileName = "evaluation/data/shapes01.png";
//	static String fileName = "evaluation/data/scale/mountain_4p2mm.jpg";
//	static String fileName = "evaluation/data/scale/mountain_7p1mm.jpg";
//	static String fileName = "evaluation/data/scale/mountain_19p9mm.jpg";

	static int NUM_FEATURES = 120;

	private static <T extends ImageSingleBand> void doStuff( Class<T> imageType , BufferedImage input ) {
		T workImage = ConvertBufferedImage.convertFrom(input,null,imageType);

		FeatureExtractor extractor = FactoryFeatureExtractor.nonmax( 5 , 1 , 5, false, true);
		FastHessianFeatureDetector<T> det = new FastHessianFeatureDetector<T>(extractor,NUM_FEATURES, 2, 9,4,4);

		T integral = GIntegralImageOps.transform(workImage,null);
		det.detect(integral);

		System.out.println("total features found: "+det.getFoundPoints().size());

		VisualizeFeatures.drawScalePoints(input.createGraphics(),det.getFoundPoints(), BoofDefaults.SCALE_SPACE_CANONICAL_RADIUS);

		ShowImages.showWindow(input,"Found Features: "+imageType.getSimpleName());
	}

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);

		doStuff(ImageFloat32.class,input);
//		doStuff(ImageUInt8.class,input);

		System.out.println("Done");
	}
}
