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

package gecv.alg.detect.intensity;

import gecv.alg.misc.PixelMath;
import gecv.alg.transform.ii.IntegralImageOps;
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.ListDisplayPanel;
import gecv.gui.image.ShowImages;
import gecv.gui.image.VisualizeImageData;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;


/**
 * Displays intensity of features in the SURF feature detector
 * @author Peter Abeles
 */
public class IntensityFastHessianApp {
//	static String fileName = "evaluation/data/outdoors01.jpg";
	static String fileName = "evaluation/data/sunflowers.png";
//	static String fileName = "evaluation/data/particles01.jpg";
//	static String fileName = "evaluation/data/scale/beach02.jpg";
//	static String fileName = "evaluation/data/indoors01.jpg";
//	static String fileName = "evaluation/data/shapes01.png";

	public static void main( String args[] ) {

		BufferedImage input = UtilImageIO.loadImage(fileName);
		ImageFloat32 inputF32 = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);

		ImageFloat32 integral = IntegralImageOps.transform(inputF32,null);
		ImageFloat32 intensity = new ImageFloat32(integral.width,integral.height);

		ListDisplayPanel guiIntensity = new ListDisplayPanel();
		guiIntensity.addImage(input,"Original");

		int skip = 0;
		for( int octave = 0; octave < 4; octave++ ) {
			if( skip == 0 )
				skip = 1;
			else
				skip = skip+skip;
			for( int sizeIndex = 0; sizeIndex< 4; sizeIndex++ ) {
				int block = 1+skip*2*(sizeIndex+1);
				int size = 3*block;

				IntegralImageFeatureIntensity.hessian(integral,1,size,intensity);
				float maxAbs = PixelMath.maxAbs(intensity);
				BufferedImage b = VisualizeImageData.colorizeSign(intensity,null,maxAbs);
				guiIntensity.addImage(b,String.format("Oct = %2d size %3d",octave+1,size));
			}
		}

		ShowImages.showWindow(guiIntensity,"Feature Intensity");
	}
}
