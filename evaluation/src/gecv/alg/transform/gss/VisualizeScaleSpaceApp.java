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

package gecv.alg.transform.gss;

import gecv.alg.misc.PixelMath;
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.ListDisplayPanel;
import gecv.gui.image.ShowImages;
import gecv.gui.image.VisualizeImageData;
import gecv.io.image.UtilImageIO;
import gecv.struct.gss.GaussianScaleSpace;
import gecv.struct.image.ImageFloat32;

import java.awt.image.BufferedImage;

/**
 * Displays the scale space for a particular image.
 *
 * @author Peter Abeles
 */
// TODO abstract and add integer
public class VisualizeScaleSpaceApp {



	public static void main( String args[] ) {
		GaussianScaleSpace<ImageFloat32,ImageFloat32> ss = FactoryGaussianScaleSpace.nocache_F32();

		ss.setScales(1,1.2,2.4,3.6,4.8,6.0);

		BufferedImage input = UtilImageIO.loadImage("evaluation/data/standard/boat.png");
		ImageFloat32 inputF32 = ConvertBufferedImage.convertFrom(input,(ImageFloat32)null);

		ss.setImage(inputF32);

		ListDisplayPanel gui = new ListDisplayPanel();
		ListDisplayPanel guiDX = new ListDisplayPanel();

		gui.addImage(input,"Original Image");

		for( int i = 0; i < ss.getTotalScales(); i++ ) {
			ss.setActiveScale(i);
			double scale = ss.getCurrentScale();
			ImageFloat32 scaledImage = ss.getScaledImage();
			BufferedImage b = ConvertBufferedImage.convertTo(scaledImage,null);
			gui.addImage(b,String.format("Scale %6.2f",scale));

			ImageFloat32 derivX = ss.getDerivative(true);
			b = VisualizeImageData.colorizeSign(derivX,null, PixelMath.maxAbs(derivX));
			guiDX.addImage(b,String.format("Scale %6.2f",scale));
		}

		ShowImages.showWindow(gui,"Scale Space");
		ShowImages.showWindow(guiDX,"Derivative X: Scale Space");
	}
}
