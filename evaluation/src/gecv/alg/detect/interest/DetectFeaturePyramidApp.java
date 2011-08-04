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

package gecv.alg.detect.interest;

import gecv.alg.interpolate.FactoryInterpolation;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.transform.gss.PyramidUpdateGaussianScale;
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.feature.ScaleSpacePyramidPointPanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.gss.ScaleSpacePyramid;
import gecv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Displays a window showing the selected corner-laplace features across diffferent scale spaces.
 *
 * @author Peter Abeles
 */
public class DetectFeaturePyramidApp {

//	static String fileName = "evaluation/data/outdoors01.jpg";
	static String fileName = "evaluation/data/sunflowers.png";
//	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";

	static int NUM_FEATURES = 50;

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);
		ImageFloat32 inputF32 = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);

		InterpolatePixel<ImageFloat32> interpolate = FactoryInterpolation.bilinearPixel(ImageFloat32.class);
		PyramidUpdateGaussianScale<ImageFloat32> update = new PyramidUpdateGaussianScale<ImageFloat32>(interpolate);
		ScaleSpacePyramid<ImageFloat32> ss = new ScaleSpacePyramid<ImageFloat32>(update,1,2,4,8,10,12,16);

		int r = 2;
		FeaturePyramid<ImageFloat32,ImageFloat32> det = FactoryInterestPointAlgs.hessianPyramid(r,1,NUM_FEATURES,ImageFloat32.class,ImageFloat32.class);
//		FeaturePyramid<ImageFloat32,ImageFloat32> det = FactoryInterestPointAlgs.harrisPyramid(r,1,NUM_FEATURES,ImageFloat32.class,ImageFloat32.class);

		ss.update(inputF32);
		det.detect(ss);

		ScaleSpacePyramidPointPanel panel = new ScaleSpacePyramidPointPanel(ss,r);
		panel.setBackground(input);
		panel.setPoints(det.getInterestPoints());

		ShowImages.showWindow(panel,"Feature Pyramid");
		System.out.println("Done");
	}
}
