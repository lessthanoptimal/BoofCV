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

package gecv.alg.denoise;

import gecv.alg.denoise.wavelet.DenoiseBayesShrink_F32;
import gecv.alg.filter.derivative.LaplacianEdge;
import gecv.alg.misc.PixelMath;
import gecv.alg.wavelet.FactoryWaveletDaub;
import gecv.alg.wavelet.UtilWavelet;
import gecv.alg.wavelet.WaveletTransformOps;
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.image.ImagePanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageDimension;
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
// todo compute SNR before and after
public class WaveletDenoiseApp {

	int width;
	int height;
	int numLevels = 3;
	float noiseSigma = 30;

	Random rand = new Random(2234);

	ImagePanel panelInput;
	ImageFloat32 image;
	ImageFloat32 imageNoisy;
	ImageFloat32 temp;
	ImageFloat32 imageWavelet;
	ImageFloat32 imageInv;

//	WaveletDescription<WlCoef_F32> waveletDesc = FactoryWaveletHaar.generate_F32();
//
	WaveletDescription<WlCoef_F32> waveletDesc = FactoryWaveletDaub.daubJ_F32(4);

//	WaveletDescription<WlCoef_F32> waveletDesc = FactoryWaveletDaub.biorthogonal_F32(5, BorderType.REFLECT);

	public void process() {
//		createTestImage();
		loadImage();

		addNoiseToImage();
//		imageNoisy = image.clone();

		width = image.getWidth();
		height = image.getHeight();

		ImageDimension d = UtilWavelet.transformDimension(image,numLevels);

		imageWavelet = new ImageFloat32(d.width,d.height);
		ImageFloat32 imageInv = new ImageFloat32(width,height);

		panelInput = ShowImages.showWindow(image,"Input Image",true);
		ConvertBufferedImage.convertTo(image,panelInput.getImage());
		panelInput.repaint();

		WaveletTransformOps.transformN(waveletDesc,imageNoisy.clone(),imageWavelet,null,numLevels);

		ShowImages.showWindow(imageWavelet,"Transformed",true);

//		DenoiseVisuShrink_F32 visu = new DenoiseVisuShrink_F32();
//		visu.denoise(imageWavelet,numLevels);

		DenoiseBayesShrink_F32 bayes = new DenoiseBayesShrink_F32();
		bayes.denoise(imageWavelet,numLevels);

//		DenoiseSureShrink_F32 sure = new DenoiseSureShrink_F32();
//		sure.denoise(imageWavelet,numLevels);

		WaveletTransformOps.inverseN(waveletDesc,imageWavelet,imageInv,null,numLevels);

//		BlurImageOps.gaussian(imageNoisy,imageInv,1,imageNoisy.clone());


		PixelMath.boundImage(imageInv,0,255);

		ShowImages.showWindow(imageNoisy,"Noisy",true);
		ShowImages.showWindow(imageInv,"Inverted",true);

		ImageFloat32 diff = new ImageFloat32(width,height);
		PixelMath.diffAbs(image,imageInv,diff);
		ShowImages.showWindow(diff,"Difference",true);

		System.out.println("noisy  error per pixel   "+computeMSE(imageNoisy));
		System.out.println("noisy  edge error        "+computeEdgeMSE(imageNoisy));
		System.out.println("denoised error per pixel "+computeMSE(imageInv));
		System.out.println("denoised edge error      "+computeEdgeMSE(imageInv));
	}

	private double computeMSE(ImageFloat32 imageInv) {
		double error = 0;
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double e = image.get(x,y)-imageInv.get(x,y);
				error += e*e;
			}
		}
		return error/(width*height);
	}

	private double computeEdgeMSE(ImageFloat32 imageInv) {
		ImageFloat32 edge = new ImageFloat32(imageInv.width,imageInv.height);
		LaplacianEdge.process_F32(image,edge);
		PixelMath.abs(edge,edge);
		float max = PixelMath.maxAbs(edge);
		PixelMath.divide(edge,edge,max);
		float total = PixelMath.sum(edge);

		double error = 0;
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				double w = edge.get(x,y)/total;
				double e = (image.get(x,y)-imageInv.get(x,y));
				error += (e*e)*w;
			}
		}
		return error;
	}

	private void loadImage() {
//		BufferedImage in = UtilImageIO.loadImage("/home/pja/rgb.jpg");
		BufferedImage in = UtilImageIO.loadImage("/home/pja/projects/gecv/evaluation/data/standard/lena512.bmp");
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

	private void addNoiseToImage() {
		imageNoisy = image.clone();
		addNormal(imageNoisy,rand,noiseSigma);
//		ImageTestingOps.addUniform(imageNoisy,rand,-noiseLevel,noiseLevel);
		PixelMath.boundImage(imageNoisy,0,255);
	}

	public static void addNormal(ImageFloat32 img, Random rand , float sigma ) {
		final int h = img.getHeight();
		final int w = img.getWidth();

		float[] data = img.data;

		for (int y = 0; y < h; y++) {
			int index = img.getStartIndex() + y * img.getStride();
			for (int x = 0; x < w; x++) {
				data[index++] += (float)(rand.nextGaussian()*sigma);
			}
		}
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
