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

package gecv.alg.detect.intensity;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.abst.detect.point.FactoryCornerDetector;
import gecv.abst.detect.point.GeneralFeatureDetector;
import gecv.abst.filter.derivative.FactoryDerivative;
import gecv.abst.filter.derivative.ImageGradient;
import gecv.abst.filter.derivative.ImageHessian;
import gecv.core.image.GeneralizedImageOps;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

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

	public static class Detector<T extends ImageBase, D extends ImageBase> extends PerformerBase {
		GeneralFeatureDetector<T,D> alg;
		ImageGradient<T,D> gradient;
		ImageHessian<D> hessian;

		D derivX,derivY;
		D derivXX,derivYY,derivXY;

		T input;

		public Detector(GeneralFeatureDetector<T,D> alg, T input , Class<T> imageType , Class<D> derivType) {
			this.alg = alg;
			this.input = input;
			gradient = FactoryDerivative.sobel(imageType,derivType);
			hessian = FactoryDerivative.hessianSobel(derivType);

			derivX = GeneralizedImageOps.createImage(derivType,imgWidth,imgHeight);
			derivY = GeneralizedImageOps.createImage(derivType,imgWidth,imgHeight);
			derivXX = GeneralizedImageOps.createImage(derivType,imgWidth,imgHeight);
			derivYY = GeneralizedImageOps.createImage(derivType,imgWidth,imgHeight);
			derivXY = GeneralizedImageOps.createImage(derivType,imgWidth,imgHeight);
		}

		@Override
		public void process() {

			if( includeGradient ) {
				if( alg.getRequiresGradient() || alg.getRequiresHessian() ) {
					gradient.process(input,derivX,derivY);
				}

				if( alg.getRequiresHessian()) {
					hessian.process(derivX,derivY,derivXX,derivYY,derivXY);
				}
			}

			alg.process(input,derivX, derivY,derivXX,derivYY,derivXY);
		}
	}

	public static void benchmark(GeneralFeatureDetector alg, String name) {
		ImageBase input = imageType == ImageFloat32.class ? image_F32 : image_I8;
		double opsPerSec = ProfileOperation.profileOpsPerSec(new Detector(alg,input,imageType,derivType), TEST_TIME, false);

		System.out.printf("%30s ops/sec = %6.2f\n", name, opsPerSec);
	}

	public static GeneralFeatureDetector<?,?> createMedian() {
		return FactoryCornerDetector.createMedian(windowRadius,1,maxFeatures,imageType);
	}

	public static GeneralFeatureDetector<?,?> createFast12() {
		return FactoryCornerDetector.createFast(windowRadius,30,maxFeatures,imageType);
	}

	public static GeneralFeatureDetector<?,?> createHarris() {
		return FactoryCornerDetector.createHarris(windowRadius,1,maxFeatures,derivType);
	}

	public static GeneralFeatureDetector<?,?> createKitRos() {
		return FactoryCornerDetector.createKitRos(windowRadius,1,maxFeatures,derivType);
	}

	public static GeneralFeatureDetector<?,?> createKlt() {
		return FactoryCornerDetector.createKlt(windowRadius,1,maxFeatures,derivType);
	}

	public static void main(String args[]) {

		FileImageSequence sequence = new FileImageSequence("data/indoors01.jpg", "data/outdoors01.jpg", "data/particles01.jpg");

		while (sequence.next()) {
			image_I8 = sequence.getImage_I8();
			image_F32 = sequence.getImage_F32();

			imgWidth = image_I8.getWidth();
			imgHeight = image_I8.getHeight();

			System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
			System.out.println("           " + sequence.getName());
			System.out.println();


			imageType = ImageFloat32.class;
			derivType = ImageFloat32.class;

			benchmark(createKlt(), "KLT F32");
			benchmark(createFast12(), "Fast F32");
			benchmark(createHarris(), "Harris F32");
			benchmark(createKitRos(), "Kit Ros F32");
			benchmark(createMedian(), "Median F32");

			imageType = ImageUInt8.class;
			derivType = ImageSInt16.class;

			benchmark(createKlt(), "KLT U8");
			benchmark(createFast12(), "Fast U8");
			benchmark(createHarris(), "Harris U8");
			benchmark(createKitRos(), "Kit Ros U8");
			benchmark(createMedian(), "Median U8");

			System.out.println();
		}
	}
}
