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

import boofcv.alg.transform.gss.ScaleSpacePyramid;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.gui.feature.ScaleSpacePyramidPointPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;

/**
 * Displays a window showing the selected corner-laplace features across diffferent scale spaces.
 *
 * @author Peter Abeles
 */
public class DetectFeatureLaplacePyramidApp {

//	static String fileName = "evaluation/data/outdoors01.jpg";
	static String fileName = "evaluation/data/sunflowers.png";
//	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
//	static String fileName = "evaluation/data/shapes01.png";

	static int NUM_FEATURES = 50;

	public static <T extends ImageBase, D extends ImageBase>
	void doStuff( BufferedImage input , Class<T> imageType, Class<D> derivType ) {
		T workImage = ConvertBufferedImage.convertFrom(input,null,imageType);

		ScaleSpacePyramid<T> ss = new ScaleSpacePyramid<T>(imageType,1,1.5,2,4,8,12,24);

		int r = 2;
		FeatureLaplacePyramid<T,D> det = FactoryInterestPointAlgs.hessianLaplacePyramid(r,1,NUM_FEATURES,imageType,derivType);
//		FeatureLaplacePyramid<T,D> det = FactoryInterestPointAlgs.harrisLaplacePyramid(r,1,NUM_FEATURES,imageType,derivType);

		ss.update(workImage);
		det.detect(ss);

		ScaleSpacePyramidPointPanel panel = new ScaleSpacePyramidPointPanel(ss,r);
		panel.setBackground(input);
		panel.setPoints(det.getInterestPoints());

		ShowImages.showWindow(panel,"Feature Laplace Pyramid: "+imageType.getSimpleName());
	}

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);
		doStuff(input,ImageFloat32.class,ImageFloat32.class);
		input = UtilImageIO.loadImage(fileName);
		doStuff(input, ImageUInt8.class, ImageSInt16.class);
		System.out.println("Done");
	}
}
