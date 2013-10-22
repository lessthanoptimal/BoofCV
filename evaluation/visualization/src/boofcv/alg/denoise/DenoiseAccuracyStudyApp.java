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
import boofcv.abst.transform.wavelet.WaveletTransform;
import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.border.BorderType;
import boofcv.factory.denoise.FactoryDenoiseWaveletAlg;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.transform.wavelet.FactoryWaveletCoiflet;
import boofcv.factory.transform.wavelet.FactoryWaveletDaub;
import boofcv.factory.transform.wavelet.FactoryWaveletHaar;
import boofcv.factory.transform.wavelet.FactoryWaveletTransform;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlCoef_F32;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Loads a sequence of tests images and runs a variety of filters through them.  Prints
 * out the denoising error for each filter and selects the best one for each image.
 *
 * @author Peter Abeles
 */
public class DenoiseAccuracyStudyApp {

	// amount of noise added to the test images
	float noiseSigma = 20;

	Random rand = new Random(2234);
	ImageFloat32 image;
	ImageFloat32 imageNoisy;

	public void process( List<TestItem> filters , List<String> images ) {

		for( String imagePath : images ) {
			System.out.println("-------------------------------------------------");
			System.out.println(imagePath);
			loadImage(imagePath);
			addNoiseToImage();

			ImageFloat32 originalImage = image.clone();
			ImageFloat32 imageDenoised = new ImageFloat32(image.width,image.height);

			System.out.println("  Noise MSE:  "+computeMSE(imageNoisy));

			TestItem best = null;
			double bestScore = Double.MAX_VALUE;

			for( TestItem i : filters ) {
				ImageMiscOps.fill(imageDenoised,0);
				i.filter.process(imageNoisy,imageDenoised);

				if( ImageStatistics.meanDiffSq(image,originalImage) != 0 )
					throw new RuntimeException("Filter modified input image");

				double error = computeMSE(imageDenoised);
				double errorEdge = computeEdgeMSE(imageDenoised);

				System.out.printf("%30s  MSE = %8.3f  Edge MSE = %8.3f\n",i.name,error,errorEdge);

				if( bestScore > error ) {
					bestScore = error;
					best = i;
				}
			}

			System.out.println("Best Filter: "+best.name);
		}


	}


	public static List<TestItem> createStandard(int minLevel , int maxLevel ) {
		List<TestItem> ret = new ArrayList<TestItem>();

		ret.addAll( addSpacial() );
		for( int numLevels = minLevel; numLevels <= maxLevel; numLevels++ ) {
			ret.addAll( createWaveletFilters(FactoryWaveletHaar.<WlCoef_F32>generate(false,32),numLevels,"Haar"));
			ret.addAll( createWaveletFilters(FactoryWaveletDaub.daubJ_F32(4),numLevels,"Daub-4"));
			ret.addAll( createWaveletFilters(FactoryWaveletCoiflet.generate_F32(6),numLevels,"Coiflet-6"));
			ret.addAll( createWaveletFilters(FactoryWaveletDaub.biorthogonal_F32(5, BorderType.WRAP),numLevels,"Biorthogonal-5"));

		}

		return ret;
	}

	protected static List<TestItem> createWaveletFilters( WaveletDescription<WlCoef_F32> waveletDesc ,
														  int numLevels , String waveletName )
	{
		List<TestItem> ret = new ArrayList<TestItem>();

		WaveletTransform<ImageFloat32, ImageFloat32,WlCoef_F32> waveletTran =
				FactoryWaveletTransform.create_F32(waveletDesc,numLevels,0,255);


		FilterImageInterface<ImageFloat32,ImageFloat32> filter;
		filter = new WaveletDenoiseFilter<ImageFloat32>(waveletTran,FactoryDenoiseWaveletAlg.visu(ImageFloat32.class));
		ret.add( new TestItem(filter,"Visu "+waveletName+" L = "+numLevels));
		filter = new WaveletDenoiseFilter<ImageFloat32>(waveletTran, FactoryDenoiseWaveletAlg.bayes(null,ImageFloat32.class));
		ret.add( new TestItem(filter,"Bayes "+waveletName+" L = "+numLevels));
		filter = new WaveletDenoiseFilter<ImageFloat32>(waveletTran,FactoryDenoiseWaveletAlg.sure(ImageFloat32.class));
		ret.add( new TestItem(filter,"Sure "+waveletName+" L = "+numLevels));

		return ret;
	}

	protected static List<TestItem> addSpacial()
	{
		List<TestItem> ret = new ArrayList<TestItem>();

		FilterImageInterface<ImageFloat32,ImageFloat32> filter;
		filter = FactoryBlurFilter.gaussian(ImageFloat32.class,-1,2);
		ret.add( new TestItem(filter,"Gaussian "+2));
		filter = FactoryBlurFilter.gaussian(ImageFloat32.class,-1,3);
		ret.add( new TestItem(filter,"Gaussian "+3));
		filter = FactoryBlurFilter.mean(ImageFloat32.class,2);
		ret.add( new TestItem(filter,"Mean "+2));
		filter = FactoryBlurFilter.mean(ImageFloat32.class,3);
		ret.add( new TestItem(filter,"Mean "+3));
		filter = FactoryBlurFilter.median(ImageFloat32.class,2);
		ret.add( new TestItem(filter,"Median "+2));
		filter = FactoryBlurFilter.median(ImageFloat32.class,3);
		ret.add( new TestItem(filter,"Median "+3));

		return ret;
	}

	private double computeMSE(ImageFloat32 imageInv) {
		return ImageStatistics.meanDiffSq(imageInv,image);
	}

	private double computeEdgeMSE(ImageFloat32 imageInv) {
		ImageFloat32 edge = new ImageFloat32(imageInv.width,imageInv.height);
		LaplacianEdge.process(image,edge);
		PixelMath.abs(edge,edge);
		float max = ImageStatistics.maxAbs(edge);
		PixelMath.divide(edge,max,edge);
		float total = ImageStatistics.sum(edge);

		double error = 0;
		for( int y = 0; y < image.height; y++ ) {
			for( int x = 0; x < image.width; x++ ) {
				double w = edge.get(x,y)/total;
				double e = (image.get(x,y)-imageInv.get(x,y));
				error += (e*e)*w;
			}
		}
		return error;
	}

	private void loadImage( String imagePath ) {
		BufferedImage in = UtilImageIO.loadImage(imagePath);
		image = ConvertBufferedImage.convertFrom(in,(ImageFloat32)null);
	}


	private void addNoiseToImage() {
		imageNoisy = image.clone();
		ImageMiscOps.addGaussian(imageNoisy,rand,noiseSigma,0,255);
	}


	public static void main( String args[] ) {
		DenoiseAccuracyStudyApp app = new DenoiseAccuracyStudyApp();

		String path = "../data/evaluation/standard/";

		List<String> fileNames = new ArrayList<String>();
		fileNames.add(path+"barbara.png");
		fileNames.add(path+"lena512.bmp");
		fileNames.add(path+"peppers256.png");
		fileNames.add(path+"boat.png");
		fileNames.add(path+"house.png");

		app.process(createStandard(2,4),fileNames);
	}

	public static class TestItem
	{
		public FilterImageInterface<ImageFloat32,ImageFloat32> filter;
		public String name;
		public double opsPerSecond;

		public TestItem(FilterImageInterface<ImageFloat32, ImageFloat32> filter, String name) {
			this.filter = filter;
			this.name = name;
		}
	}
}
