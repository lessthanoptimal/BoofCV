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

import gecv.core.image.ConvertBufferedImage;
import gecv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import gecv.factory.transform.gss.FactoryGaussianScaleSpace;
import gecv.gui.feature.ScaleSpacePointPanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.gss.GaussianScaleSpace;
import gecv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Displays a window showing the selected corner-laplace features across diffferent scale spaces.
 *
 * @author Peter Abeles
 */
public class DetectFeatureLaplaceScaleSpaceApp {

//	static String fileName = "evaluation/data/outdoors01.jpg";
//	static String fileName = "evaluation/data/sunflowers.png";
//	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
//	static String fileName = "evaluation/data/indoors01.jpg";
//	static String fileName = "evaluation/data/shapes01.png";
//	static String fileName = "evaluation/data/scale/mountain_4p2mm.jpg";
	static String fileName = "evaluation/data/scale/mountain_7p1mm.jpg";
//	static String fileName = "evaluation/data/scale/mountain_19p9mm.jpg";

	static int NUM_FEATURES = 2000;

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);
		ImageFloat32 inputF32 = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);

		GaussianScaleSpace<ImageFloat32,ImageFloat32> ss = FactoryGaussianScaleSpace.nocache_F32();
		ss.setScales(1,1.5,2,4,8,12,24);
		ss.setImage(inputF32);

		int r = 2;
		FeatureLaplaceScaleSpace<ImageFloat32,ImageFloat32> det = FactoryInterestPointAlgs.hessianLaplace(r,1,NUM_FEATURES,ImageFloat32.class,ImageFloat32.class);
//		FeatureLaplaceScaleSpace<ImageFloat32,ImageFloat32> det = FactoryInterestPointAlgs.harrisLaplace(r,1,NUM_FEATURES,ImageFloat32.class,ImageFloat32.class);

		det.detect(ss);

		ScaleSpacePointPanel panel = new ScaleSpacePointPanel(ss,r);
		panel.setBackground(input);
		panel.setPoints(det.getInterestPoints());

		ShowImages.showWindow(panel,"Feature Laplace Scale Space");
		System.out.println("Done");
	}
}
