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
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * @author Peter Abeles
 */
public class TrackVideoPyramidKLT_U8 extends TrackVideoPyramidKLT<ImageUInt8, ImageSInt16>{
	public TrackVideoPyramidKLT_U8(SimpleImageSequence<ImageUInt8> imageSequence,
									PkltManager<ImageUInt8, ImageSInt16> tracker,
									PyramidUpdater<ImageUInt8> pyramidUpdater ,
									GradientPyramid<ImageUInt8, ImageSInt16> updateGradient) {
		super(imageSequence, tracker , pyramidUpdater , updateGradient);
	}

	public static void main( String args[] ) {

		String fileName;

		if (args.length == 0) {
//			fileName = "/mnt/data/datasets/2010/snow_videos/snow_norail_stabilization.avi";
			fileName = "/mnt/data/datasets/2010/snow_videos/snow_long_drive.avi";
		} else {
			fileName = args[0];
		}
		SimpleImageSequence<ImageUInt8> sequence = new XugglerSimplified<ImageUInt8>(fileName, ImageUInt8.class);

		ImageBase<?> image = sequence.next();

		KltConfig configKLt = new KltConfig();
		configKLt.forbiddenBorder = 0;
		configKLt.maxPerPixelError = 25.0f;
		configKLt.maxIterations = 15;
		configKLt.minDeterminant = 0.001f;
		configKLt.minPositionDelta = 0.01f;

		PkltManagerConfig<ImageUInt8, ImageSInt16> config = new PkltManagerConfig<ImageUInt8,ImageSInt16>();
		config.config = configKLt;
		config.typeInput = ImageUInt8.class;
		config.typeDeriv = ImageSInt16.class;
		config.pyramidScaling = new int[]{1,2,2,2};
		config.imgWidth = image.width;
		config.imgHeight = image.height;
		config.minFeatures = 80;
		config.maxFeatures = 100;
		config.featureRadius = 3;

		int scalingTop = config.computeScalingTop();

		InterpolateRectangle<ImageUInt8> interp = FactoryInterpolation.bilinearRectangle(config.typeInput);
		InterpolateRectangle<ImageSInt16> interpD = FactoryInterpolation.bilinearRectangle(config.typeDeriv);

		GeneralCornerIntensity<ImageUInt8,ImageSInt16> intensity =
				new WrapperGradientCornerIntensity<ImageUInt8,ImageSInt16>(
						FactoryCornerIntensity.createKlt(config.typeDeriv,config.featureRadius));
		CornerExtractor extractor = new WrapperNonMax(
				new FastNonMaxCornerExtractor(config.featureRadius+2,
						config.featureRadius*scalingTop, configKLt.minDeterminant));
		GeneralCornerDetector<ImageUInt8,ImageSInt16> detector =
				new GeneralCornerDetector<ImageUInt8,ImageSInt16>(intensity,extractor,config.maxFeatures);

		GenericPkltFeatSelector<ImageUInt8, ImageSInt16> featureSelector =
				new GenericPkltFeatSelector<ImageUInt8,ImageSInt16>(detector,null);

		ConvolutionPyramid<ImageUInt8> pyrUpdater =
				new ConvolutionPyramid<ImageUInt8>(KernelFactory.gaussian1D_I32(2),config.typeInput);

		ImageGradient<ImageUInt8,ImageSInt16> gradient = FactoryDerivative.sobel_I8();

		GradientPyramid<ImageUInt8,ImageSInt16> gradientUpdater =
				new GradientPyramid<ImageUInt8,ImageSInt16>(gradient);

		PkltManager<ImageUInt8,ImageSInt16> manager =
				new PkltManager<ImageUInt8,ImageSInt16>(config,interp,interpD,featureSelector);

		TrackVideoPyramidKLT_U8 alg = new TrackVideoPyramidKLT_U8(sequence,manager,pyrUpdater,gradientUpdater);

		alg.process();
	}
}
