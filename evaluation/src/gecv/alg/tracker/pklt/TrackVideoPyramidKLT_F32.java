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

package gecv.alg.tracker.pklt;

import gecv.abst.detect.corner.GeneralCornerDetector;
import gecv.abst.detect.corner.GeneralCornerIntensity;
import gecv.abst.detect.corner.WrapperGradientCornerIntensity;
import gecv.abst.detect.extract.CornerExtractor;
import gecv.abst.detect.extract.WrapperNonMax;
import gecv.abst.filter.derivative.FactoryDerivative;
import gecv.abst.filter.derivative.ImageGradient;
import gecv.alg.detect.corner.FactoryCornerIntensity;
import gecv.alg.detect.extract.FastNonMaxCornerExtractor;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.alg.interpolate.FactoryInterpolation;
import gecv.alg.interpolate.InterpolateRectangle;
import gecv.alg.pyramid.ConvolutionPyramid_F32;
import gecv.alg.tracker.klt.KltConfig;
import gecv.io.image.SimpleImageSequence;
import gecv.io.wrapper.xuggler.XugglerSimplified;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class TrackVideoPyramidKLT_F32 extends TrackVideoPyramidKLT<ImageFloat32, ImageFloat32>{
	public TrackVideoPyramidKLT_F32(SimpleImageSequence<ImageFloat32> imageSequence,
									PkltManager<ImageFloat32, ImageFloat32> tracker ) {
		super(imageSequence, tracker );
	}

	public static void main( String args[] ) {

		String fileName;

		if (args.length == 0) {
			fileName = "/mnt/data/datasets/2010/snow_videos/snow_norail_stabilization.avi";
		} else {
			fileName = args[0];
		}
		SimpleImageSequence<ImageFloat32> sequence = new XugglerSimplified<ImageFloat32>(fileName, ImageFloat32.class);

		ImageBase<?> image = sequence.next();

		KltConfig configKLt = new KltConfig();
		configKLt.forbiddenBorder = 2;
		configKLt.maxPerPixelError = 25.0f;
		configKLt.maxIterations = 15;
		configKLt.minDeterminant = 0.001f;
		configKLt.minPositionDelta = 0.01f;

		PkltManagerConfig<ImageFloat32,ImageFloat32> config = new PkltManagerConfig<ImageFloat32,ImageFloat32>();
		config.config = configKLt;
		config.typeInput = ImageFloat32.class;
		config.typeDeriv = ImageFloat32.class;
		config.pyramidScaling = new int[]{1,2,2,2};
		config.imgWidth = image.width;
		config.imgHeight = image.height;
		config.minFeatures = 80;
		config.maxFeatures = 100;
		config.featureRadius = 3;

		InterpolateRectangle<ImageFloat32> interp = FactoryInterpolation.bilinearRectangle_F32();

		GeneralCornerIntensity<ImageFloat32,ImageFloat32> intensity =
				new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(
						FactoryCornerIntensity.createKlt_F32(image.width, image.height, config.featureRadius));
		CornerExtractor extractor = new WrapperNonMax(
				new FastNonMaxCornerExtractor(config.featureRadius+2, config.featureRadius*8, configKLt.minDeterminant));
		GeneralCornerDetector<ImageFloat32,ImageFloat32> detector =
				new GeneralCornerDetector<ImageFloat32,ImageFloat32>(intensity,extractor,config.maxFeatures);

		GenericPkltFeatSelector<ImageFloat32, ImageFloat32> featureSelector =
				new GenericPkltFeatSelector<ImageFloat32,ImageFloat32>(detector,null);

		ConvolutionPyramid_F32 pyrUpdater = new ConvolutionPyramid_F32(KernelFactory.gaussian1D_F32(2,true));

		ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.sobel_F32();

		PkltManager<ImageFloat32,ImageFloat32> manager =
				new PkltManager<ImageFloat32,ImageFloat32>(config,interp,interp,
						gradient,featureSelector,pyrUpdater);

		TrackVideoPyramidKLT_F32 alg = new TrackVideoPyramidKLT_F32(sequence,manager);

		alg.process();
	}
}
