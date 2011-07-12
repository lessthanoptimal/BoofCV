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

package gecv.alg.detect.corner;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.abst.detect.corner.*;
import gecv.abst.detect.extract.CornerExtractor;
import gecv.abst.detect.extract.WrapperNonMax;
import gecv.abst.detect.extract.WrapperNonMaxCandidate;
import gecv.alg.detect.extract.FastNonMaxCornerExtractor;
import gecv.alg.detect.extract.NonMaxCornerCandidateExtractor;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.alg.filter.derivative.HessianFromGradient;
import gecv.alg.filter.derivative.HessianThree;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * Benchmark which scores the whole corner detection processing stack.
 *
 * @author Peter Abeles
 */
public class BenchmarkCornerRuntime {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int windowRadius = 2;
	static long TEST_TIME = 1000;

	static ImageFloat32 image_F32;
	static ImageFloat32 derivX_F32;
	static ImageFloat32 derivY_F32;
	static ImageFloat32 derivXX_F32;
	static ImageFloat32 derivYY_F32;
	static ImageFloat32 derivXY_F32;

	static ImageUInt8 image_I8;
	static ImageSInt16 derivX_I16;
	static ImageSInt16 derivY_I16;
	static ImageSInt16 derivXX_I16;
	static ImageSInt16 derivYY_I16;
	static ImageSInt16 derivXY_I16;

	static int maxFeatures = imgWidth * imgHeight / (windowRadius * windowRadius);

	// should it include the gradient calculation in the benchmark?
	static boolean includeGradient = true;

	public static class Detector_F32 extends PerformerBase {
		GeneralCornerDetector<ImageFloat32,ImageFloat32> alg;

		public Detector_F32(GeneralCornerDetector<ImageFloat32,ImageFloat32> alg) {
			this.alg = alg;
		}

		@Override
		public void process() {

			if( includeGradient ) {
				if( alg.getRequiresGradient() ) {
					GradientSobel.process(image_F32, derivX_F32, derivY_F32, true);
				}

				if( alg.getRequiresHessian()) {
					HessianFromGradient.hessianThree(derivX_F32,derivY_F32,derivXX_F32,derivYY_F32,derivXY_F32,true);
//					HessianThree.process(image_F32,derivXX_F32,derivYY_F32,derivXY_F32,true);
				}
			}

			alg.process(image_F32,derivX_F32, derivY_F32,derivXX_F32,derivYY_F32,derivXY_F32);
		}
	}

	public static void benchmark(GeneralCornerDetector<ImageFloat32,ImageFloat32> alg, String name) {
		double opsPerSec = ProfileOperation.profileOpsPerSec(new Detector_F32(alg), TEST_TIME);

		System.out.printf("%30s ops/sec = %6.2f\n", name, opsPerSec);
	}

	public static GeneralCornerDetector<ImageFloat32,ImageFloat32> createMedian_F32() {
		WrapperMedianCornerIntensity<ImageFloat32, ImageFloat32> alg;
		alg = WrapperMedianCornerIntensity.create(ImageFloat32.class,imgWidth,imgHeight,2);
		CornerExtractor extractor = new WrapperNonMax(new FastNonMaxCornerExtractor(windowRadius,windowRadius, 1));

		return new GeneralCornerDetector<ImageFloat32,ImageFloat32>(alg,extractor,maxFeatures);
	}

	public static GeneralCornerDetector<ImageFloat32,ImageFloat32> createFast12_F32() {
		FastCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createFast12( ImageFloat32.class , 30, 12);

		return createFastDetector(alg);
	}

	public static GeneralCornerDetector<ImageFloat32,ImageFloat32> createHarris_F32() {
		HarrisCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createHarris( ImageFloat32.class , windowRadius, 0.04f);

		return createGradientDetector(alg);
	}

	public static GeneralCornerDetector<ImageFloat32,ImageFloat32> createKitRos_F32() {
		KitRosCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createKitRos( ImageFloat32.class );

		return createDetector(alg);
	}

	public static GeneralCornerDetector<ImageFloat32,ImageFloat32> createKlt_F32() {
		KltCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createKlt( ImageFloat32.class , windowRadius);

		return createGradientDetector(alg);
	}

	private static GeneralCornerDetector<ImageFloat32,ImageFloat32> createGradientDetector(GradientCornerIntensity<ImageFloat32> alg) {
		GeneralCornerIntensity<ImageFloat32,ImageFloat32> intensity = new WrapperGradientCornerIntensity<ImageFloat32,ImageFloat32>(alg);
		CornerExtractor extractor = new WrapperNonMax(new FastNonMaxCornerExtractor(windowRadius,windowRadius, 1));

		return new GeneralCornerDetector<ImageFloat32,ImageFloat32>(intensity, extractor, maxFeatures);
	}

	private static GeneralCornerDetector<ImageFloat32,ImageFloat32> createDetector(KitRosCornerIntensity<ImageFloat32> alg) {
		GeneralCornerIntensity<ImageFloat32,ImageFloat32> intensity = new WrapperKitRosCornerIntensity<ImageFloat32,ImageFloat32>(alg);
		CornerExtractor extractor = new WrapperNonMax(new FastNonMaxCornerExtractor(windowRadius,windowRadius, 1));

		return new GeneralCornerDetector<ImageFloat32,ImageFloat32>(intensity, extractor, maxFeatures);
	}

	private static GeneralCornerDetector<ImageFloat32,ImageFloat32> createFastDetector(FastCornerIntensity<ImageFloat32> alg) {
		GeneralCornerIntensity<ImageFloat32,ImageFloat32> intensity = new WrapperFastCornerIntensity<ImageFloat32,ImageFloat32>(alg);
		CornerExtractor extractorCandidate = new WrapperNonMaxCandidate(new NonMaxCornerCandidateExtractor(windowRadius, 1));

		return new GeneralCornerDetector<ImageFloat32,ImageFloat32>(intensity, extractorCandidate, maxFeatures);
	}

	public static void main(String args[]) {

		derivX_F32 = new ImageFloat32(imgWidth, imgHeight);
		derivY_F32 = new ImageFloat32(imgWidth, imgHeight);
		derivXX_F32 = new ImageFloat32(imgWidth, imgHeight);
		derivYY_F32 = new ImageFloat32(imgWidth, imgHeight);
		derivXY_F32 = new ImageFloat32(imgWidth, imgHeight);

		derivX_I16 = new ImageSInt16(imgWidth, imgHeight);
		derivY_I16 = new ImageSInt16(imgWidth, imgHeight);
		derivXX_I16 = new ImageSInt16(imgWidth, imgHeight);
		derivYY_I16 = new ImageSInt16(imgWidth, imgHeight);
		derivXY_I16 = new ImageSInt16(imgWidth, imgHeight);

		FileImageSequence sequence = new FileImageSequence("data/indoors01.jpg", "data/outdoors01.jpg", "data/particles01.jpg");

		while (sequence.next()) {
			image_I8 = sequence.getImage_I8();
			image_F32 = sequence.getImage_F32();

			imgWidth = image_I8.getWidth();
			imgHeight = image_I8.getHeight();

			System.out.println("=========  Profile Image Size " + imgWidth + " x " + imgHeight + " ==========");
			System.out.println("           " + sequence.getName());
			System.out.println();

			derivX_F32.reshape(imgWidth, imgHeight);
			derivY_F32.reshape(imgWidth, imgHeight);
			derivXX_F32.reshape(imgWidth, imgHeight);
			derivYY_F32.reshape(imgWidth, imgHeight);
			derivXY_F32.reshape(imgWidth, imgHeight);
			derivX_I16.reshape(imgWidth, imgHeight);
			derivY_I16.reshape(imgWidth, imgHeight);
			derivXX_I16.reshape(imgWidth, imgHeight);
			derivYY_I16.reshape(imgWidth, imgHeight);
			derivXY_I16.reshape(imgWidth, imgHeight);

			GradientSobel.process(image_F32, derivX_F32, derivY_F32, true);
			GradientSobel.process(image_I8, derivX_I16, derivY_I16, true);
			HessianThree.process(image_F32,derivXX_F32,derivYY_F32,derivXY_F32,true);
			HessianThree.process(image_I8,derivXX_I16,derivYY_I16,derivXY_I16,true);

			benchmark(createKlt_F32(), "KLT F32");
			benchmark(createFast12_F32(), "Fast F32");
			benchmark(createHarris_F32(), "Harris F32");
			benchmark(createKitRos_F32(), "Kit Ros F32");
			benchmark(createMedian_F32(), "Median F32");

			System.out.println();
		}
	}
}
