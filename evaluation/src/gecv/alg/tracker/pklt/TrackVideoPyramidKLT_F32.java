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
import gecv.alg.tracker.klt.KltConfig;
import gecv.alg.transform.pyramid.ConvolutionPyramid;
import gecv.alg.transform.pyramid.GradientPyramid;
import gecv.alg.transform.pyramid.PyramidUpdater;
import gecv.io.image.SimpleImageSequence;
import gecv.io.wrapper.xuggler.XugglerSimplified;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;

/**
 * @author Peter Abeles
 */
public class TrackVideoPyramidKLT_F32 extends TrackVideoPyramidKLT<ImageFloat32, ImageFloat32>{
	public TrackVideoPyramidKLT_F32(SimpleImageSequence<ImageFloat32> imageSequence,
									PkltManager<ImageFloat32, ImageFloat32> tracker,
									PyramidUpdater<ImageFloat32> pyramidUpdater ,
									GradientPyramid<ImageFloat32,ImageFloat32> updateGradient ) {
		super(imageSequence, tracker , pyramidUpdater, updateGradient);
	}

	public static void main( String args[] ) {

		String fileName;

		if (args.length == 0) {
//			fileName = "/mnt/data/datasets/2010/snow_videos/snow_norail_stabilization.avi";
			fileName = "/mnt/data/datasets/2010/snow_videos/snow_long_drive.avi";
		} else {
			fileName = args[0];
		}
		SimpleImageSequence<ImageFloat32> sequence = new XugglerSimplified<ImageFloat32>(fileName, ImageFloat32.class);

		ImageBase<?> image = sequence.next();

		KltConfig configKLt = new KltConfig();
		configKLt.forbiddenBorder = 0;
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

		int scalingTop = config.computeScalingTop();

		InterpolateRectangle<ImageFloat32> interp = FactoryInterpolation.bilinearRectangle(ImageFloat32.class);

		GeneralCornerIntensity<ImageFloat32,ImageFloat32> intensity =
				new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(
						FactoryCornerIntensity.createKlt(ImageFloat32.class , config.featureRadius));
		CornerExtractor extractor = new WrapperNonMax(
				new FastNonMaxCornerExtractor(config.featureRadius+2,
						config.featureRadius*scalingTop, configKLt.minDeterminant));
		GeneralCornerDetector<ImageFloat32,ImageFloat32> detector =
				new GeneralCornerDetector<ImageFloat32,ImageFloat32>(intensity,extractor,config.maxFeatures);

		GenericPkltFeatSelector<ImageFloat32, ImageFloat32> featureSelector =
				new GenericPkltFeatSelector<ImageFloat32,ImageFloat32>(detector,null);

		ConvolutionPyramid<ImageFloat32> pyrUpdater =
				new ConvolutionPyramid<ImageFloat32>(KernelFactory.gaussian1D_F32(2,true),ImageFloat32.class);

		ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.sobel_F32();

		GradientPyramid<ImageFloat32,ImageFloat32> gradientUpdater =
				new GradientPyramid<ImageFloat32,ImageFloat32>(gradient);

		PkltManager<ImageFloat32,ImageFloat32> manager =
				new PkltManager<ImageFloat32,ImageFloat32>(config,interp,interp,featureSelector);

		TrackVideoPyramidKLT_F32 alg = new TrackVideoPyramidKLT_F32(sequence,manager,
				pyrUpdater,gradientUpdater);

		alg.process();
	}
}
