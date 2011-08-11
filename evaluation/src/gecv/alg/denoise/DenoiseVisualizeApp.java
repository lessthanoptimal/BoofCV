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

import gecv.abst.denoise.WaveletDenoiseFilter;
import gecv.abst.filter.FilterImageInterface;
import gecv.abst.wavelet.FactoryWaveletTransform;
import gecv.abst.wavelet.WaveletTransform;
import gecv.alg.denoise.wavelet.DenoiseSureShrink_F32;
import gecv.alg.misc.ImageTestingOps;
import gecv.alg.misc.PixelMath;
import gecv.alg.transform.wavelet.FactoryWaveletCoiflet;
import gecv.core.image.ConvertBufferedImage;
import gecv.core.image.border.BorderType;
import gecv.gui.ListDisplayPanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageFloat32;
import gecv.struct.wavelet.WaveletDescription;
import gecv.struct.wavelet.WlCoef_F32;

import java.awt.image.BufferedImage;
import java.util.Random;


/**
 * Displays the results of denoising an image.
 *
 * @author Peter Abeles
 */
public class DenoiseVisualizeApp {

	// amount of noise added to the test images
	float noiseSigma = 15;

	Random rand = new Random(2234);
	ImageFloat32 image;
	ImageFloat32 imageNoisy;
	ImageFloat32 imageDenoised;

	int numLevels = 3;

	BorderType borderType = BorderType.REFLECT;

//	WaveletDescription<WlCoef_F32> waveletDesc = FactoryWaveletHaar.generate_F32();
//	WaveletDescription<WlCoef_F32> waveletDesc = FactoryWaveletDaub.daubJ_F32(4);
//	WaveletDescription<WlCoef_F32> waveletDesc = FactoryWaveletDaub.biorthogonal_F32(5,borderType);
	WaveletDescription<WlCoef_F32> waveletDesc = FactoryWaveletCoiflet.generate_F32(6);

	WaveletTransform<ImageFloat32, ImageFloat32,WlCoef_F32> waveletTran = FactoryWaveletTransform.create_F32(waveletDesc,numLevels);

//	DenoiseWavelet denoiser = new DenoiseVisuShrink_F32();
//	DenoiseWavelet denoiser = new DenoiseBayesShrink_F32();
	DenoiseWavelet denoiser = new DenoiseSureShrink_F32();

	String imagePath = "evaluation/data/standard/barbara.png";
//	String imagePath = "evaluation/data/particles01.jpg";

	FilterImageInterface<ImageFloat32,ImageFloat32> filter = new WaveletDenoiseFilter<ImageFloat32>(waveletTran,denoiser);

	public void process() {

		System.out.println(imagePath);
		loadImage(imagePath);
		addNoiseToImage();
		imageDenoised = new ImageFloat32(image.width,image.height);

		System.out.printf("Noise MSE    %8.1ff\n",computeMSE(imageNoisy));

		filter.process(imageNoisy,imageDenoised);

		System.out.printf("Denoised MSE %8.1ff\n",computeMSE(imageDenoised));

		ListDisplayPanel gui = new ListDisplayPanel();
		gui.addImage(ConvertBufferedImage.convertTo(image,null),"Original");
		gui.addImage(ConvertBufferedImage.convertTo(imageNoisy,null),"Noisy");
		gui.addImage(ConvertBufferedImage.convertTo(imageDenoised,null),"De-noised");

		ShowImages.showWindow(gui,"Image Denoising");
	}

	private double computeMSE(ImageFloat32 imageInv) {
		return ImageTestingOps.computeMeanSquaredError(imageInv,image);
	}

	private void loadImage( String imagePath ) {
		BufferedImage in = UtilImageIO.loadImage(imagePath);
		image = ConvertBufferedImage.convertFrom(in,(ImageFloat32)null);
		if( image == null ) {
			throw new RuntimeException("Couldn't load image: "+imagePath);
		}
	}

	private void addNoiseToImage() {
		imageNoisy = image.clone();
		ImageTestingOps.addGaussian(imageNoisy,rand,noiseSigma);
		PixelMath.boundImage(imageNoisy,0,255);
	}


	public static void main( String args[] ) {
		DenoiseVisualizeApp app = new DenoiseVisualizeApp();

		app.process();
	}
}
