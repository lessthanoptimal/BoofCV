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

package gecv.alg.wavelet;

import gecv.abst.wavelet.FactoryWaveletTransform;
import gecv.abst.wavelet.WaveletTransform;
import gecv.alg.misc.ImageTestingOps;
import gecv.alg.misc.PixelMath;
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.image.ImagePanel;
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
	float noiseLevel = 50;

	Random rand = new Random(2234);

	ImagePanel panelInput;
	ImageFloat32 image;
	ImageFloat32 imageWavelet;
//	WaveletDescription<WlCoef_F32> desc = FactoryWaveletHaar.generate_F32();

//
//	WaveletDescription<WlCoef_F32> desc = FactoryWaveletDaub.daubJ_F32(4);
//	WaveletDescription<WlCoef_F32> desc = FactoryWaveletDaub.biorthogonal_F32(5,WaveletBorderType.WRAP);

	WaveletDescription<WlCoef_F32> desc = FactoryWaveletDaub.biorthogonal_F32(5,WaveletBorderType.REFLECT);
	WaveletTransform<ImageFloat32,ImageFloat32,WlCoef_F32> tran = FactoryWaveletTransform.create_F32(desc,numLevels);

	public void process() {
		createTestImage();
//		loadImage();

		width = image.getWidth();
		height = image.getHeight();

		System.out.println("width "+width+"  height "+height);

		ImageFloat32 imageInv = new ImageFloat32(width,height);

		panelInput = ShowImages.showWindow(image,"Input Image",true);
		ConvertBufferedImage.convertTo(image,panelInput.getImage());
		panelInput.repaint();

		imageWavelet = tran.transform(image,imageWavelet);

		int scaleW = imageWavelet.width/UtilWavelet.computeScale(numLevels);
		int scaleH = imageWavelet.height/UtilWavelet.computeScale(numLevels);
		scaleW += scaleW%2;
		scaleH += scaleH%2;

//		float thresh = 25;
//		for( int y = 0; y < height; y++ ) {
//			for( int x = 0; x < width; x++ ) {
//				if( x < scaleW && y < scaleH )
//					continue;
//				float v = Math.abs(imageWavelet.get(x,y));
//				if( v < thresh )
//					imageWavelet.set(x,y,0);
//			}
//		}

		for( int y = 0; y < imageWavelet.height; y++ ) {
			for( int x = 0; x < imageWavelet.width; x++ ) {
				if( x < imageWavelet.width/2 && y < imageWavelet.height/2 )
					continue;
				imageWavelet.set(x,y,0);
			}
		}

		ShowImages.showWindow(imageWavelet,"Transformed",true);
		tran.invert(imageWavelet,imageInv);

		PixelMath.boundImage(imageInv,0,255);

		ShowImages.showWindow(imageInv,"Inverted",true);

		double error = 0;
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double e = image.get(x,y)-imageInv.get(x,y);
				error += Math.abs(e);
			}
		}
		System.out.println("average error per pixel "+(error/(width*height)));
	}

	private void loadImage() {
		BufferedImage in = UtilImageIO.loadImage("/home/pja/rgb.jpg");
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
		ImageTestingOps.addUniform(image,rand,-noiseLevel,noiseLevel);
		PixelMath.boundImage(image,0,255);

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
