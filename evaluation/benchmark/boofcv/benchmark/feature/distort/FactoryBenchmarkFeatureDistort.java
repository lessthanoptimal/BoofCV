/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.benchmark.feature.distort;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.filter.basic.GGrayImageOps;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.benchmark.feature.orientation.UtilOrientationBenchmark;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.affine.Affine2D_F32;


/**
 * Creates different distort feature benchmarks.
 *
 * @author Peter Abeles
 */
public class FactoryBenchmarkFeatureDistort {

	private static final long SEED = 234243;

	public static <T extends ImageSingleBand>
	BenchmarkFeatureDistort<T> noise( Class<T> imageType ) {
		double sigmas[]=new double[]{1,2,4,8,12,16,20,40};
		return new AddNoise<T>(SEED,sigmas,imageType);
	}

	public static <T extends ImageSingleBand>
	BenchmarkFeatureDistort<T> intensity( Class<T> imageType ) {
		double gammas[]=new double[]{0.25,0.5,0.8,1,1.5,2,4};
		return new Intensity<T>(SEED,gammas,imageType);
	}

	public static <T extends ImageSingleBand>
	BenchmarkFeatureDistort<T> rotate(  Class<T> imageType ) {
		double thetas[]=UtilOrientationBenchmark.makeSample(0,2.0*Math.PI,20);
		return new Rotation<T>(SEED,thetas,imageType);
	}

	public static <T extends ImageSingleBand>
	BenchmarkFeatureDistort<T> scale( Class<T> imageType ) {
		double ratio[]=new double[]{0.5,0.75,1,1.25,1.5,2,3,4,8};
		return new Scale<T>(SEED,ratio,imageType);
	}

	private static class AddNoise<T extends ImageSingleBand>
			extends BenchmarkFeatureDistort<T>
	{
		protected AddNoise(long randomSeed, double[] variable,  Class<T> imageType) {
			super(randomSeed, variable, "Sigma", imageType);
		}

		@Override
		protected void distortImage(T image, T distortedImage ,double sigma) {
			distortedImage.reshape(image.width,image.height);
			distortedImage.setTo(image);
			GImageMiscOps.addGaussian(distortedImage, rand, sigma, 0, 255);
		}

		@Override
		protected DistortParam createDistortParam(double variable) {
			return new DistortParam(0,1);
		}
	}

	private static class Intensity<T extends ImageSingleBand>
			extends BenchmarkFeatureDistort<T>
	{
		protected Intensity(long randomSeed, double[] variable, Class<T> imageType) {
			super(randomSeed, variable, "Gamma", imageType);
		}

		@Override
		protected void distortImage(T image, T distortedImage ,double gamma) {
			distortedImage.reshape(image.width,image.height);
			GGrayImageOps.stretch(image,gamma,gamma,255,distortedImage);
		}

		@Override
		protected DistortParam createDistortParam(double variable) {
			return new DistortParam(0,1);
		}
	}

	private static class Rotation<T extends ImageSingleBand>
			extends BenchmarkFeatureDistort<T>
	{
		protected Rotation(long randomSeed, double[] variable, Class<T> imageType) {
			super(randomSeed, variable, "radians", imageType);
		}

		@Override
		protected void distortImage(T image, T distortedImage ,double theta) {
			distortedImage.reshape(image.width,image.height);

			DistortImageOps.rotate(image,distortedImage,TypeInterpolate.BILINEAR,(float)theta);
		}

		@Override
		protected DistortParam createDistortParam(double variable) {
			return new DistortParam(variable,1);
		}
	}

	private static class Scale<T extends ImageSingleBand>
			extends BenchmarkFeatureDistort<T>
	{
		protected Scale(long randomSeed, double[] variable, Class<T> imageType) {
			super(randomSeed, variable, "ScaleFactor", imageType);
		}

		@Override
		protected void distortImage(T image, T distortedImage ,double scale) {
			distortedImage.reshape(image.width,image.height);

			Affine2D_F32 initToImage = StabilityEvaluatorPoint.createScale((float)scale,image.width,image.height);
			Affine2D_F32 imageToInit = initToImage.invert(null);
			PixelTransformAffine_F32 affine = new PixelTransformAffine_F32(imageToInit);
			InterpolatePixelS<T> interp = FactoryInterpolation.createPixelS(0, 255, TypeInterpolate.BILINEAR, imageType);
			ImageDistort<T,T> distorter = FactoryDistort.distort(false,interp, FactoryImageBorder.value(imageType, 0), imageType);
			distorter.setModel(affine);
			distorter.apply(image,distortedImage);
		}

		@Override
		protected DistortParam createDistortParam(double scale) {
			return new DistortParam(0,scale);
		}
	}
}
