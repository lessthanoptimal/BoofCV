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

import gecv.abst.detect.corner.GeneralCornerIntensity;
import gecv.abst.detect.corner.WrapperGradientCornerIntensity;
import gecv.abst.detect.extract.CornerExtractor;
import gecv.abst.detect.extract.WrapperNonMax;
import gecv.alg.detect.corner.FactoryCornerIntensity;
import gecv.alg.detect.extract.FastNonMaxCornerExtractor;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.alg.interpolate.FactoryInterpolation;
import gecv.alg.interpolate.InterpolateRectangle;
import gecv.alg.pyramid.ConvolutionPyramid_F32;
import gecv.alg.pyramid.PyramidUpdater;
import gecv.alg.tracker.klt.KltConfig;
import gecv.alg.tracker.klt.KltTracker;
import gecv.io.image.SimpleImageSequence;
import gecv.io.wrapper.xuggler.XugglerSimplified;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.pyramid.ImagePyramid;
import gecv.struct.pyramid.ImagePyramid_F32;

/**
 * @author Peter Abeles
 */
public class TrackVideoPyramidKLT_F32 extends TrackVideoPyramidKLT<ImageFloat32, ImageFloat32>{
	public TrackVideoPyramidKLT_F32(SimpleImageSequence<ImageFloat32> imageSequence,
									PyramidKltTracker<ImageFloat32, ImageFloat32> tracker,
									PyramidKltFeatureSelector<ImageFloat32, ImageFloat32> featureSelector,
									PyramidUpdater<ImageFloat32> pyramidUpdater) {
		super(imageSequence, tracker, featureSelector, pyramidUpdater);
	}

	@Override
	protected ImagePyramid<ImageFloat32> createPyramid(int width, int height, int... scales) {
		ImagePyramid_F32 ret = new ImagePyramid_F32(width,height,true);
		ret.setScaling(scales);
		derivX = new ImageFloat32[ scales.length ];
		derivY = new ImageFloat32[ scales.length ];

		for( int i = 0; i < scales.length; i++ ) {
			int scale = ret.getScalingAtLayer(i);
			int layerWidth = width/scale;
			int layerHeight = height/scale;

			derivX[i] = new ImageFloat32(layerWidth,layerHeight);
			derivY[i] = new ImageFloat32(layerWidth,layerHeight);
		}

		return ret;
	}

	@Override
	protected void computeDerivatives(ImageFloat32 input, ImageFloat32 derivX, ImageFloat32 derivY) {
		GradientSobel.process(input,derivX,derivY, true);
	}

	public static void main( String args[] ) {

		String fileName;

		if (args.length == 0) {
			fileName = "/home/pja/uav_video.avi";
		} else {
			fileName = args[0];
		}
		SimpleImageSequence<ImageFloat32> sequence = new XugglerSimplified<ImageFloat32>(fileName, ImageFloat32.class);

		ImageBase<?> image = sequence.next();

		// todo this needs to get pushed down into base class
		int maxFeatures = 200;
		int minFeatures = 100;
		int featureRadius = 2;

		KltConfig config = new KltConfig();
		config.forbiddenBorder = 2;
		config.maxError = 0.01f;
		config.maxIterations = 20;
		config.minDeterminant = 0.01f;
		config.minPositionDelta = 0.01f;

		int imgWidth = image.width;
		int imgHeight = image.height;

		InterpolateRectangle<ImageFloat32> interp = FactoryInterpolation.bilinearRectangle_F32();

		KltTracker<ImageFloat32, ImageFloat32> kltTracker = new KltTracker<ImageFloat32, ImageFloat32>(interp,interp,config);
		PyramidKltTracker<ImageFloat32, ImageFloat32> pyrTracker = new PyramidKltTracker<ImageFloat32, ImageFloat32>(kltTracker);

		GeneralCornerIntensity<ImageFloat32,ImageFloat32> intensity =
				new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(
						FactoryCornerIntensity.createKlt_F32(imgWidth, imgHeight, featureRadius));
		CornerExtractor extractor = new WrapperNonMax(new FastNonMaxCornerExtractor(featureRadius + 10, featureRadius + 10, 0.1f));
		
		PyramidKltFeatureSelector<ImageFloat32, ImageFloat32> featureSelector =
				new GenericPkltFeatSelector<ImageFloat32,ImageFloat32>(maxFeatures,intensity,extractor,pyrTracker);

		ConvolutionPyramid_F32 pyrUpdater = new ConvolutionPyramid_F32(KernelFactory.gaussian1D_F32(2,true));

		TrackVideoPyramidKLT_F32 alg = new TrackVideoPyramidKLT_F32(sequence,pyrTracker,featureSelector,pyrUpdater);

		alg.process();
	}
}
