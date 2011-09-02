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

import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import boofcv.factory.transform.gss.FactoryGaussianScaleSpace;
import boofcv.gui.feature.ScaleSpacePointPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Displays a window showing the selected corner-laplace features across different scale spaces.
 *
 * @author Peter Abeles
 */
public class DetectFeatureScaleSpaceApp {

//	static String fileName = "evaluation/data/outdoors01.jpg";
	static String fileName = "evaluation/data/sunflowers.png";
//	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
//	static String fileName = "evaluation/data/indoors01.jpg";
//	static String fileName = "evaluation/data/shapes01.png";

	static int NUM_FEATURES = 50;

	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage(fileName);
		ImageFloat32 inputF32 = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);

		GaussianScaleSpace<ImageFloat32,ImageFloat32> ss = FactoryGaussianScaleSpace.nocache_F32();
		ss.setScales(1,1.5,2,4,8,12,24);
		ss.setImage(inputF32);

		int r = 2;
		FeatureScaleSpace<ImageFloat32,ImageFloat32> det = FactoryInterestPointAlgs.hessianScaleSpace(r,1,NUM_FEATURES,ImageFloat32.class,ImageFloat32.class);
//		FeatureScaleSpace<ImageFloat32,ImageFloat32> det = FactoryInterestPointAlgs.harrisScaleSpace(r,1,NUM_FEATURES,ImageFloat32.class,ImageFloat32.class);

		det.detect(ss);

		ScaleSpacePointPanel panel = new ScaleSpacePointPanel(ss,r);
		panel.setBackground(input);
		panel.setPoints(det.getInterestPoints());

		ShowImages.showWindow(panel,"Feature Scale Space");
		System.out.println("Done");
	}
}
