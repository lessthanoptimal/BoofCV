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
import gecv.abst.detect.extract.WrapperFastNonMax;
import gecv.abst.detect.extract.WrapperNonMaxCandidate;
import gecv.alg.detect.extract.FastNonMaxCornerExtractor;
import gecv.alg.detect.extract.NonMaxCornerCandidateExtractor;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;

import java.util.Random;

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
	static ImageInt8 image_I8;
	static ImageInt16 derivX_I16;
	static ImageInt16 derivY_I16;

	static Random rand = new Random(234);

	public static class DetectorGradient_F32 extends PerformerBase {
		CornerDetectorGradient<ImageFloat32> alg;

		public DetectorGradient_F32(CornerDetectorGradient<ImageFloat32> alg) {
			this.alg = alg;
		}

		@Override
		public void process() {
			alg.process(derivX_F32, derivY_F32);
		}
	}

	public static class DetectorImage_F32 extends PerformerBase {
		CornerDetectorImage<ImageFloat32> alg;

		public DetectorImage_F32(CornerDetectorImage<ImageFloat32> alg) {
			this.alg = alg;
		}

		@Override
		public void process() {
			alg.process(image_F32);
		}
	}

	public static void benchmark(CornerDetectorGradient<ImageFloat32> alg, String name) {
		double opsPerSec = ProfileOperation.profileOpsPerSec(new DetectorGradient_F32(alg), TEST_TIME);

		System.out.printf("%30s ops/sec = %6.2f\n", name, opsPerSec);
	}

	public static void benchmark(CornerDetectorImage<ImageFloat32> alg, String name) {
		double opsPerSec = ProfileOperation.profileOpsPerSec(new DetectorImage_F32(alg), TEST_TIME);

		System.out.printf("%30s ops/sec = %6.2f\n", name, opsPerSec);
	}

	public static CornerDetectorImage<ImageFloat32> createFast12_F32() {
		FastCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createFast12_F32(imgWidth, imgHeight, 30, 12);

		return createFastDetector(alg);
	}

	public static CornerDetectorGradient<ImageFloat32> createHarris_F32() {
		HarrisCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createHarris_F32(imgWidth, imgHeight, windowRadius, 0.04f);

		return createGradientDetector(alg);
	}

	public static CornerDetectorGradient<ImageFloat32> createKitRos_F32() {
		KitRosCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createKitRos_F32(imgWidth, imgHeight, windowRadius);

		return createGradientDetector(alg);
	}

	public static CornerDetectorGradient<ImageFloat32> createKlt_F32() {
		KltCornerIntensity<ImageFloat32> alg = FactoryCornerIntensity.createKlt_F32(imgWidth, imgHeight, windowRadius);

		return createGradientDetector(alg);
	}

	private static CornerDetectorGradient<ImageFloat32> createGradientDetector(GradientCornerIntensity<ImageFloat32> alg) {
		CornerIntensityGradient<ImageFloat32> intensity = new WrapperGradientCornerIntensity<ImageFloat32>(alg);
		CornerExtractor extractor = new WrapperFastNonMax(new FastNonMaxCornerExtractor(windowRadius, windowRadius, 1));
		return new CornerDetectorGradient<ImageFloat32>(intensity, extractor, imgWidth * imgHeight / (windowRadius * windowRadius));
	}

	private static CornerDetectorImage<ImageFloat32> createFastDetector(FastCornerIntensity<ImageFloat32> alg) {
		CornerIntensityImage<ImageFloat32> intensity = new WrapperFastCornerIntensity<ImageFloat32>(alg);
		CornerExtractor extractorCandidate = new WrapperNonMaxCandidate(new NonMaxCornerCandidateExtractor(windowRadius, 1));

		CornerDetectorImage<ImageFloat32> ret = new CornerDetectorImage<ImageFloat32>(intensity, extractorCandidate, imgWidth * imgHeight / (windowRadius * windowRadius));
		ret.setCandidateCorners(alg.getCandidates());
		return ret;
	}

	public static void main(String args[]) {

		derivX_F32 = new ImageFloat32(imgWidth, imgHeight);
		derivY_F32 = new ImageFloat32(imgWidth, imgHeight);
		derivX_I16 = new ImageInt16(imgWidth, imgHeight);
		derivY_I16 = new ImageInt16(imgWidth, imgHeight);

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
			derivX_I16.reshape(imgWidth, imgHeight);
			derivY_I16.reshape(imgWidth, imgHeight);

			GradientSobel.process_F32(image_F32, derivX_F32, derivY_F32);
			GradientSobel.process_I8(image_I8, derivX_I16, derivY_I16);


			benchmark(createKlt_F32(), "KLT F32");
			benchmark(createFast12_F32(), "Fast F32");
			benchmark(createHarris_F32(), "Harris F32");
			benchmark(createKitRos_F32(), "Kit Ros F32");

			System.out.println();
		}
	}
}
