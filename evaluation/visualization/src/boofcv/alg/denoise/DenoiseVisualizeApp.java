/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
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
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.transform.wavelet.WaveletTransform;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.denoise.FactoryDenoiseWaveletAlg;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.transform.wavelet.FactoryWaveletCoiflet;
import boofcv.factory.transform.wavelet.FactoryWaveletDaub;
import boofcv.factory.transform.wavelet.FactoryWaveletHaar;
import boofcv.factory.transform.wavelet.FactoryWaveletTransform;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;


/**
 * Displays the results of denoising an image.
 *
 * @author Peter Abeles
 */
public class DenoiseVisualizeApp<T extends ImageSingleBand,D extends ImageSingleBand,W extends WlCoef>
	extends SelectAlgorithmAndInputPanel implements DenoiseInfoPanel.Listener
{

	// amount of noise added to the test images
	float noiseSigma;
	int numLevels;
	// config for blurring
	int blurRadius;

	Random rand = new Random(2234);

	// selected spacial filter
	BlurFilter<T> filter;
	// selected wavelet filter
	DenoiseWavelet<T> denoiser;
	WaveletDescription<W> waveletDesc;
	List<WaveletDescription> waveletList = new ArrayList<WaveletDescription>();

	JPanel gui = new JPanel();
	DenoiseInfoPanel info = new DenoiseInfoPanel();
	ImagePanel imagePanel = new ImagePanel();

	Class<T> imageType;
	T input;
	T noisy;
	T output;

	D deriv;

	Vector<BufferedImage> images = new Vector<BufferedImage>();

	boolean processedImage = false;

	public DenoiseVisualizeApp( Class<T> imageType ) {
		super(1);

		this.imageType = imageType;

		addAlgorithm(0,"BayesShrink", FactoryDenoiseWaveletAlg.bayes(null,imageType));
		addAlgorithm(0,"SureShrink",FactoryDenoiseWaveletAlg.sure(imageType));
		addAlgorithm(0,"VisuShrink",FactoryDenoiseWaveletAlg.visu(imageType));
		FilterImageInterface<T,T> filter;
		filter = FactoryBlurFilter.gaussian(imageType,-1,1);
		addAlgorithm(0,"Gaussian",filter);
		filter = FactoryBlurFilter.mean(imageType,1);
		addAlgorithm(0,"Mean",filter);
		filter = FactoryBlurFilter.median(imageType,1);
		addAlgorithm(0,"Median",filter);

		info.addWaveletName("Daub 4");
		waveletList.add(FactoryWaveletDaub.daubJ_F32(4));
		info.addWaveletName("Coiflet 6");
		waveletList.add(FactoryWaveletCoiflet.generate_F32(6));
		info.addWaveletName("Haar");
		waveletList.add(FactoryWaveletHaar.generate(false, 32));
		// todo something is clearly wrong with biorthogonal.  comment out so it doesn't appear in the applet
//		info.addWaveletName("Biorthogonal 5");
//		waveletList.add(FactoryWaveletDaub.biorthogonal_F32(5, BorderType.WRAP));
		waveletDesc = waveletList.get(0);

		input = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		noisy = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		output = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);
		deriv = GeneralizedImageOps.createSingleBand(derivType, 1, 1);

		gui.setLayout(new BorderLayout());
		gui.add(info,BorderLayout.WEST);
		gui.add(imagePanel,BorderLayout.CENTER);
		info.setListener(this);

		// get initial values
		noiseSigma = info.getNoiseSigma();
		blurRadius = info.getBlurRadius();
		numLevels = info.getWaveletLevel();

		setMainGUI(gui);
	}

	public void process( BufferedImage image ) {
		input.reshape(image.getWidth(),image.getHeight());
		noisy.reshape(input.width,input.height);
		output.reshape(input.width,input.height);
		deriv.reshape(input.width,input.height);

		ConvertBufferedImage.convertFromSingle(image, input, imageType);

		// add noise to the image
		noisy.setTo(input);
		GImageMiscOps.addGaussian(noisy, rand, noiseSigma, 0, 255);
		GPixelMath.boundImage(noisy,0,255);
		// compute edge image for weighted error
		GImageDerivativeOps.laplace(input,deriv);
		GPixelMath.abs(deriv,deriv);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				images.clear();
				images.add(ConvertBufferedImage.convertTo(output,null,true));
				images.add(ConvertBufferedImage.convertTo(noisy,null,true));
				images.add(ConvertBufferedImage.convertTo(input,null,true));
				info.reset();
				doRefreshAll();
			}});
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		// tod adjust menus?
		if( cookies[0] instanceof DenoiseWavelet ) {
			denoiser = (DenoiseWavelet<T>)cookies[0];
			filter = null;
		} else {
			denoiser = null;
			filter = (BlurFilter<T>)cookies[0];
		}

		performDenoising();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		switch( indexFamily ) {
			case 0:
				if( cookie instanceof DenoiseWavelet ) {
					denoiser = (DenoiseWavelet<T>)cookie;
					filter = null;
				} else {
					filter = (BlurFilter<T>)cookie;
					denoiser = null;
				}
				break;
		}

		performDenoising();
	}

	private synchronized void performDenoising() {
		if( denoiser != null ) {
			WaveletTransform<T, T,W> waveletTran =
					FactoryWaveletTransform.create(imageType,waveletDesc,numLevels,0,255);
			FilterImageInterface<T,T> filter = new WaveletDenoiseFilter<T>(waveletTran,denoiser);

			filter.process(noisy,output);
		} else {
			filter.setRadius(blurRadius);
			filter.process(noisy,output);
		}

		final double algError = computeError((ImageFloat32)output,(ImageFloat32)input);
		final double algErrorEdge = computeWeightedError((ImageFloat32)output,(ImageFloat32)input,(ImageFloat32)deriv);
		final double noiseError = computeError((ImageFloat32)noisy,(ImageFloat32)input);
		final double noiseErrorEdge = computeWeightedError((ImageFloat32)noisy,(ImageFloat32)input,(ImageFloat32)deriv);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				info.setWaveletActive(denoiser!=null);
				ConvertBufferedImage.convertTo(output,images.get(0),true);
				ConvertBufferedImage.convertTo(noisy,images.get(1),true);
				info.setError(algError,algErrorEdge,noiseError,noiseErrorEdge);
				imagePanel.repaint();
				info.repaint();
				processedImage = true;
			}});
	}

	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if( image != null ) {
			process(image);
		}
	}

	@Override
	public void noiseChange(float sigma) {
		this.noiseSigma = sigma;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				noisy.setTo(input);
				GImageMiscOps.addGaussian(noisy,rand,noiseSigma,0,255);
				GPixelMath.boundImage(noisy,0,255);
				performDenoising();
			}});
	}

	@Override
	public void imageChange(final int which) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				BufferedImage b = images.get(which);
				imagePanel.setBufferedImage(images.get(which));
				imagePanel.setPreferredSize(new Dimension(b.getWidth(),b.getHeight()));
				gui.validate();
				imagePanel.repaint();
			}});
	}

	@Override
	public void waveletChange(int which , int level ) {
		waveletDesc = waveletList.get(which);
		this.numLevels = level;
		performDenoising();
	}

	@Override
	public void noiseChange(int radius) {
		this.blurRadius = radius;
		performDenoising();
	}

	// todo push to what ops? Also what is this error called again?
	public static double computeError(ImageFloat32 imgA, ImageFloat32 imgB ) {
		final int h = imgA.getHeight();
		final int w = imgA.getWidth();

		double total = 0;

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				double difference =  Math.abs(imgA.get(x,y)-imgB.get(x,y));
				total += difference;
			}
		}

		return total / (w*h);
	}

	// todo push to what ops?
	public static double computeWeightedError(ImageFloat32 imgA, ImageFloat32 imgB ,
											  ImageFloat32 imgWeight ) {
		final int h = imgA.getHeight();
		final int w = imgA.getWidth();

		double total = 0;
		double totalWeight = 0;

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				float weight = imgWeight.get(x,y);
				double difference =  Math.abs(imgA.get(x,y)-imgB.get(x,y));
				total += difference*weight;
				totalWeight += weight;
			}
		}

		return total / totalWeight;
	}

	public static void main( String args[] ) {
		DenoiseVisualizeApp app = new DenoiseVisualizeApp(ImageFloat32.class);

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("lena","../data/evaluation/standard/lena512.bmp"));
		inputs.add(new PathLabel("barbara","../data/evaluation/standard/barbara.png"));
		inputs.add(new PathLabel("boat","../data/evaluation/standard/boat.png"));
		inputs.add(new PathLabel("fingerprint","../data/evaluation/standard/fingerprint.png"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Image Noise Removal");

		System.out.println("Done");
	}


}
