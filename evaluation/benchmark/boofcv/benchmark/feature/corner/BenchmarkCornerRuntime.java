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

package boofcv.benchmark.feature.corner;

import boofcv.abst.feature.detect.interest.ConfigFast;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.evaluation.FileImageSequence;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

/**
 * Benchmark which scores the whole corner detection processing stack.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class BenchmarkCornerRuntime {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int windowRadius = 2;
	static long TEST_TIME = 1000;

	static ImageFloat32 image_F32;

	static ImageUInt8 image_I8;

	static Class imageType;
	static Class derivType;

	static int maxFeatures = imgWidth * imgHeight / (windowRadius * windowRadius);

	// should it include the gradient calculation in the benchmark?
	static boolean includeGradient = true;

	public static class Detector<T extends ImageSingleBand, D extends ImageSingleBand> extends PerformerBase {
		GeneralFeatureDetector<T, D> alg;
		ImageGradient<T, D> gradient;
		ImageHessian<D> hessian;

		D derivX, derivY;
		D derivXX, derivYY, derivXY;

		T input;

		public Detector(GeneralFeatureDetector<T, D> alg, T input, Class<T> imageType, Class<D> derivType) {
			this.alg = alg;
			this.input = input;
			gradient = FactoryDerivative.sobel(imageType, derivType);
			hessian = FactoryDerivative.hessianSobel(derivType);

			derivX = GeneralizedImageOps.createSingleBand(derivType, imgWidth, imgHeight);
			derivY = GeneralizedImageOps.createSingleBand(derivType, imgWidth, imgHeight);
			derivXX = GeneralizedImageOps.createSingleBand(derivType, imgWidth, imgHeight);
			derivYY = GeneralizedImageOps.createSingleBand(derivType, imgWidth, imgHeight);
			derivXY = GeneralizedImageOps.createSingleBand(derivType, imgWidth, imgHeight);
		}

		@Override
		public void process() {

			if (includeGradient) {
				if (alg.getRequiresGradient() || alg.getRequiresHessian()) {
					gradient.process(input, derivX, derivY);
				}

				if (alg.getRequiresHessian()) {
					hessian.process(derivX, derivY, derivXX, derivYY, derivXY);
				}
			}

			alg.process(input, derivX, derivY, derivXX, derivYY, derivXY);
		}
	}

	public static void benchmark(GeneralFeatureDetector alg, String name) {
		ImageSingleBand input = imageType == ImageFloat32.class ? image_F32 : image_I8;
		double opsPerSec = ProfileOperation.profileOpsPerSec(new Detector(alg, input, imageType, derivType), TEST_TIME, false);

		System.out.printf("%30s ops/sec = %6.2f\n", name, opsPerSec);
	}

	public static GeneralFeatureDetector<?, ?> createMedian() {
		return FactoryDetectPoint.createMedian(new ConfigGeneralDetector(maxFeatures,windowRadius, 1), imageType);
	}

	public static GeneralFeatureDetector<?, ?> createFast12() {
		return FactoryDetectPoint.createFast(
				new ConfigFast(30,9),new ConfigGeneralDetector(maxFeatures,windowRadius,30), imageType);
	}

	public static GeneralFeatureDetector<?, ?> createHarris() {
		return FactoryDetectPoint.createHarris(new ConfigGeneralDetector(maxFeatures,windowRadius,1), false, derivType);
	}

	public static GeneralFeatureDetector<?, ?> createKitRos() {
		return FactoryDetectPoint.createKitRos(new ConfigGeneralDetector(maxFeatures,windowRadius, 1), derivType);
	}

	public static GeneralFeatureDetector<?, ?> createShiTomasi() {
		return FactoryDetectPoint.createShiTomasi(new ConfigGeneralDetector(maxFeatures,windowRadius,1), false,  derivType);
	}

	public static void main(String args[]) {
		String pre = "../data/evaluation/";

		FileImageSequence<ImageUInt8> sequence_U8 = new FileImageSequence<ImageUInt8>(ImageUInt8.class, "indoors01.jpg", "outdoors01.jpg", "particles01.jpg");
		FileImageSequence<ImageFloat32> sequence_F32 = new FileImageSequence<ImageFloat32>(ImageFloat32.class, "indoors01.jpg", "outdoors01.jpg", "particles01.jpg");

		sequence_U8.setPrefix(pre);
		sequence_F32.setPrefix(pre);

		while (sequence_U8.next() && sequence_F32.next()) {
			image_I8 = sequence_U8.getImage();
			image_F32 = sequence_F32.getImage();

			imgWidth = image_I8.getWidth();
			imgHeight = image_I8.getHeight();

			System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
			System.out.println("           " + sequence_U8.getName());
			System.out.println();


			imageType = ImageFloat32.class;
			derivType = ImageFloat32.class;

			benchmark(createShiTomasi(), "KLT F32");
			benchmark(createFast12(), "Fast F32");
			benchmark(createHarris(), "Harris F32");
			benchmark(createKitRos(), "Kit Ros F32");
			benchmark(createMedian(), "Median F32");

			imageType = ImageUInt8.class;
			derivType = ImageSInt16.class;

			benchmark(createShiTomasi(), "KLT U8");
			benchmark(createFast12(), "Fast U8");
			benchmark(createHarris(), "Harris U8");
			benchmark(createKitRos(), "Kit Ros U8");
			benchmark(createMedian(), "Median U8");

			System.out.println();
		}
	}
}
