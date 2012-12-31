/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.BoofDefaults;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class DebugSiftDetectorApp {

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage("../data/evaluation/sunflowers.png");
//		BufferedImage input = UtilImageIO.loadImage("../data/evaluation/shapes01.png");

		ImageFloat32 gray = ConvertBufferedImage.convertFromSingle(input, null, ImageFloat32.class);

		SiftDetector alg = FactoryInterestPointAlgs.siftDetector(new ConfigSiftDetector(3,10,150,5));
		SiftImageScaleSpace imageSS = new SiftImageScaleSpace(1.6f, 5, 4, false);

		imageSS.constructPyramid(gray);
		imageSS.computeFeatureIntensity();

		alg.process(imageSS);

		System.out.println("total features found: "+alg.getFoundPoints().size());

		VisualizeFeatures.drawScalePoints(input.createGraphics(),
				alg.getFoundPoints().toList(), BoofDefaults.SCALE_SPACE_CANONICAL_RADIUS);


		ListDisplayPanel dog = new ListDisplayPanel();
		for( int i = 0; i < alg.ss.dog.length; i++ ) {
			int scale = i % (alg.ss.numScales-1);
			int octave = i / (alg.ss.numScales-1);

			BufferedImage img = VisualizeImageData.colorizeSign(alg.ss.dog[i],null,-1);
			dog.addImage(img,octave+"  "+scale);
		}

		ListDisplayPanel ss = new ListDisplayPanel();
		for( int i = 0; i < alg.ss.scale.length; i++ ) {
			int scale = i % alg.ss.numScales;
			int octave = i / alg.ss.numScales;

			BufferedImage img = VisualizeImageData.grayMagnitude(alg.ss.scale[i],null,255);
			ss.addImage(img,octave+"  "+scale);
		}
		ShowImages.showWindow(dog, "Octave DOG");
		ShowImages.showWindow(ss, "Octave Scales");
		ShowImages.showWindow(input, "Found Features");

		System.out.println("Done");
	}
}
