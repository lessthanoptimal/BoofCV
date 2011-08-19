/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.feature.detect.interest;

import gecv.alg.interpolate.FactoryInterpolation;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.transform.gss.PyramidUpdateGaussianScale;
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.feature.ScaleSpacePyramidPointPanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.gss.ScaleSpacePyramid;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

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

		InterpolatePixel<T> interpolate = FactoryInterpolation.bilinearPixel(imageType);
		PyramidUpdateGaussianScale<T> update = new PyramidUpdateGaussianScale<T>(interpolate);
		ScaleSpacePyramid<T> ss = new ScaleSpacePyramid<T>(update,1,1.5,2,4,8,12,24);

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
