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

import gecv.alg.misc.ImageTestingOps;
import gecv.alg.misc.PixelMath;
import gecv.alg.wavelet.impl.ImplWaveletTransformNaive;
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageFloat32;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;


/**
 * @author Peter Abeles
 */
public class WaveletDenoiseApp {

	int width;
	int height;
	float noiseLevel = 50;

	Random rand = new Random(2234);

	ImagePanel panelInput;
	ImageFloat32 image;
	ImageFloat32 temp;
	ImageFloat32 imageWavelet;
	ImageFloat32 imageInv;

	WaveletDesc_F32 coefF = FactoryWaveletHaar.generate_F32();
	WaveletDesc_F32 coefR = FactoryWaveletHaar.generate_F32();
//
//	WaveletDesc_F32 coefF = FactoryWaveletDaub.standard_F32(4);
//	WaveletDesc_F32 coefR = FactoryWaveletDaub.standard_F32(4);

//	WaveletDesc_F32 coefF = FactoryWaveletDaub.biorthogonal_F32(5);
//	WaveletDesc_F32 coefR = FactoryWaveletDaub.biorthogonalInv_F32(5);

	public void process() {
//		createTestImage();
		loadImage();

		width = image.getWidth();
		height = image.getHeight();
		temp = new ImageFloat32(width,height);
		imageWavelet = new ImageFloat32(width,height);
		ImageFloat32 imageInv = new ImageFloat32(width,height);

		panelInput = ShowImages.showWindow(image,"Input Image",true);
		ConvertBufferedImage.convertTo(image,panelInput.getImage());
		panelInput.repaint();


		ImplWaveletTransformNaive.horizontal(coefF,image,temp);
		ImplWaveletTransformNaive.vertical(coefF,temp,imageWavelet);

		ShowImages.showWindow(imageWavelet,"Transformed",true);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				if( x < width/2 && y < height/2 )
					continue;
				float v = Math.abs(imageWavelet.get(x,y));
				if( v < 5 )
					imageWavelet.set(x,y,0);
			}
		}

//		for( int y = 0; y < height; y++ ) {
//			for( int x = 0; x < width; x++ ) {
//				if( x < width/2 && y < height/2 )
//					continue;
//				float v = imageWavelet.get(x,y)*4;
//				imageWavelet.set(x,y,v);
//			}
//		}

//		for( int y = 0; y < height; y++ ) {
//			for( int x = 0; x < width; x++ ) {
//				if( x < width/2 && y < height/2 ) {
//					imageWavelet.set(x,y,0);
//				}
//			}
//		}

		ImplWaveletTransformNaive.verticalInverse(coefR,imageWavelet,temp);
		ImplWaveletTransformNaive.horizontalInverse(coefR,temp,imageInv);

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
		WaveletDenoiseApp app = new WaveletDenoiseApp();

		app.process();
	}
}
