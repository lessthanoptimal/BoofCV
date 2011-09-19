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

package boofcv.alg.denoise;

import boofcv.abst.denoise.WaveletDenoiseFilter;
import boofcv.abst.filter.FilterImageInterface;
import boofcv.abst.wavelet.WaveletTransform;
import boofcv.alg.denoise.wavelet.DenoiseBayesShrink_F32;
import boofcv.alg.denoise.wavelet.DenoiseSureShrink_F32;
import boofcv.alg.denoise.wavelet.DenoiseVisuShrink_F32;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.transform.wavelet.FactoryWaveletCoiflet;
import boofcv.factory.transform.wavelet.FactoryWaveletDaub;
import boofcv.factory.transform.wavelet.FactoryWaveletHaar;
import boofcv.factory.transform.wavelet.FactoryWaveletTransform;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.ProcessImage;
import boofcv.gui.SelectAlgorithmImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Random;


/**
 * Displays the results of denoising an image.
 *
 * @author Peter Abeles
 */
// TODO make custom panel
	// select display image
	// noise level
	// number of levels
	// statistics for denoised and noisy image
// todo add non-wavelet filters
public class DenoiseVisualizeApp<T extends ImageBase,W extends WlCoef>
	extends SelectAlgorithmImagePanel implements ProcessImage
{

	// amount of noise added to the test images
	float noiseSigma = 15;
	int numLevels = 3;

	Random rand = new Random(2234);

	DenoiseWavelet<T> denoiser;
	WaveletDescription<W> waveletDesc;

	ListDisplayPanel gui = new ListDisplayPanel();

	Class<T> imageType;
	T input;
	T noisy;
	T output;

	boolean processedImage = false;

	public DenoiseVisualizeApp( Class<T> imageType ) {
		super(2);

		this.imageType = imageType;

		addAlgorithm(0,"BayesShrink",new DenoiseBayesShrink_F32());
		addAlgorithm(0,"VisuShrink",new DenoiseVisuShrink_F32());
		addAlgorithm(0,"SureShrink",new DenoiseSureShrink_F32());

		addAlgorithm(1,"Daub 4",FactoryWaveletDaub.daubJ_F32(4));
		addAlgorithm(1,"Haar", FactoryWaveletHaar.generate(false,32));
		addAlgorithm(1,"Coiflet 6",FactoryWaveletCoiflet.generate_F32(6));
// todo something is clearly wrong with biorthogonal.  comment out so it doesn't appear in the applet
//		addAlgorithm(1,"Biorthogonal 5", FactoryWaveletDaub.biorthogonal_F32(5,borderType));

		input = GeneralizedImageOps.createImage(imageType,1,1);
		noisy = GeneralizedImageOps.createImage(imageType,1,1);
		output = GeneralizedImageOps.createImage(imageType,1,1);

		setMainGUI(gui);
	}

	public void process( BufferedImage image ) {
		input.reshape(image.getWidth(),image.getHeight());
		noisy.reshape(input.width,input.height);
		output.reshape(input.width,input.height);
		ConvertBufferedImage.convertFrom(image,input,imageType);

		// add noise to the image
		noisy.setTo(input);
		GeneralizedImageOps.addGaussian(noisy,rand,noiseSigma);
		GPixelMath.boundImage(noisy,0,255);

		doRefreshAll();
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		denoiser = (DenoiseWavelet<T>)cookies[0];
		waveletDesc = (WaveletDescription<W>)cookies[1];

		performDenoising();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		switch( indexFamily ) {
			case 0:
				denoiser = (DenoiseWavelet<T>)cookie;
				break;

			case 1:
				waveletDesc = (WaveletDescription<W>)cookie;
				break;
		}

		performDenoising();
	}

	private void performDenoising() {
		WaveletTransform<T, T,W> waveletTran = FactoryWaveletTransform.create(waveletDesc,numLevels);
		FilterImageInterface<T,T> filter = new WaveletDenoiseFilter<T>(waveletTran,denoiser);

		filter.process(noisy,output);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.reset();
				gui.addImage(ConvertBufferedImage.convertTo(output,null),"De-noised");
				gui.addImage(ConvertBufferedImage.convertTo(noisy,null),"Noisy");
				gui.addImage(ConvertBufferedImage.convertTo(input,null),"Original");
				processedImage = true;
			}});
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getImageManager();

		BufferedImage image = manager.loadImage(index);
		if( image != null ) {
			process(image);
		}
	}

//	private double computeMSE(ImageFloat32 imageInv) {
//		return ImageTestingOps.computeMeanSquaredError(imageInv,image);
//	}
//
//	private void loadImage( String imagePath ) {
//		BufferedImage in = UtilImageIO.loadImage(imagePath);
//		image = ConvertBufferedImage.convertFrom(in,(ImageFloat32)null);
//		if( image == null ) {
//			throw new RuntimeException("Couldn't load image: "+imagePath);
//		}
//	}
//
//	private void addNoiseToImage() {
//		imageNoisy = image.clone();
//		ImageTestingOps.addGaussian(imageNoisy,rand,noiseSigma);
//		PixelMath.boundImage(imageNoisy,0,255);
//	}


	public static void main( String args[] ) {
		DenoiseVisualizeApp app = new DenoiseVisualizeApp(ImageFloat32.class);

		ImageListManager manager = new ImageListManager();
		manager.add("lena","data/standard/lena512.bmp");
		manager.add("barbara","data/standard/barbara.png");
		manager.add("boat","data/standard/boat.png");
		manager.add("fingerprint","data/standard/fingerprint.png");

		app.setImageManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Image Noise Removal");

		System.out.println("Done");
	}
}
