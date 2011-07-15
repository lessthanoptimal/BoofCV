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

package gecv.alg.transform.wavelet;

import gecv.abst.wavelet.FactoryWaveletTransform;
import gecv.abst.wavelet.WaveletTransform;
import gecv.alg.misc.ImageTestingOps;
import gecv.alg.misc.PixelMath;
import gecv.core.image.ConvertBufferedImage;
import gecv.core.image.border.BorderType;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletDescription;
import gecv.struct.wavelet.WlCoef_F32;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;


/**
 * @author Peter Abeles
 */
public class WaveletVisualizeApp {

	int width;
	int height;
	int numLevels = 4;

	Random rand = new Random(2234);

	ImageFloat32 image;
	ImageFloat32 imageWavelet;

//	WaveletDescription<WlCoef_F32> waveletDesc = FactoryWaveletHaar.generate_F32();
//	WaveletDescription<WlCoef_F32> waveletDesc = FactoryWaveletDaub.daubJ_F32(4);
	WaveletDescription<WlCoef_F32> waveletDesc = FactoryWaveletDaub.biorthogonal_F32(5, BorderType.REFLECT);

	WaveletTransform<ImageFloat32, ImageFloat32,WlCoef_F32> waveletTran = FactoryWaveletTransform.create_F32(waveletDesc,numLevels);


	public void process() {
//		createTestImage();
		loadImage();

		width = image.getWidth();
		height = image.getHeight();

		System.out.println("width "+width+"  height "+height);

		ImageFloat32 imageInv = new ImageFloat32(width,height);

		imageWavelet = waveletTran.transform(image,imageWavelet);

		waveletTran.invert(imageWavelet,imageInv);

		PixelMath.boundImage(imageInv,0,255);

		ShowImages.showWindow(image,"Input Image",true);
		ShowImages.showWindow(imageWavelet,"Transformed",true);
		ShowImages.showWindow(imageInv,"Inverted",true);

		double error = ImageTestingOps.computeMeanSquaredError(image,imageInv);

		System.out.println("Mean Squared Error "+error);
	}

	private void loadImage() {
		BufferedImage in = UtilImageIO.loadImage("evaluation/data/standard/lena512.bmp");
		image = ConvertBufferedImage.convertFrom(in,image);
	}

	private void createTestImage() {
		width = 250;
		height = 300;
		BufferedImage workImg = new BufferedImage(width,height,BufferedImage.TYPE_INT_BGR);
		Graphics2D g2 = workImg.createGraphics();
		g2.setColor(new Color(200,200,200));
		g2.fillRect(0,0,width,height);
		g2.setColor(Color.BLACK);
		addRectangle(g2,new AffineTransform(),40,50,60,50);

		AffineTransform tran = new AffineTransform();
		tran.setToRotation(0.5);
		addRectangle(g2,tran,120,140,60,50);

		tran.setToRotation(-1.2);
		addRectangle(g2,tran,-120,200,60,40);

		image = ConvertBufferedImage.convertFrom(workImg,image);
	}

	private void addRectangle( Graphics2D g2 , AffineTransform tran , int x0 , int y0 , int w , int h )
	{
		g2.setTransform(tran);
		g2.fillRect(x0,y0,w,h);
	}

	public static void main( String args[] ) {
		WaveletVisualizeApp app = new WaveletVisualizeApp();

		app.process();
	}
}
