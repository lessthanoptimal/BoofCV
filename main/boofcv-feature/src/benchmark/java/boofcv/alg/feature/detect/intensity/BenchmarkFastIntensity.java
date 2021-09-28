/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.impl.ImplFastCorner12_F32;
import boofcv.alg.feature.detect.intensity.impl.ImplFastCorner12_U8;
import boofcv.alg.feature.detect.intensity.impl.ImplFastCorner9_F32;
import boofcv.alg.feature.detect.intensity.impl.ImplFastCorner9_U8;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unchecked", "rawtypes"})
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkFastIntensity {

	static int imageSize = 1000;

	@State(Scope.Benchmark)
	public static class NaiveState {
		DetectorFastNaive naive9 = new DetectorFastNaive(3, 9, 60);
		GrayU8 input;
		GrayF32 intensity;

		@Setup public void setUp() {
			var rand = new Random(234234);
			input = new GrayU8(imageSize, imageSize);
			intensity = input.createSameShape(GrayF32.class);
			GImageMiscOps.fillUniform(input, rand, 0, 255);
		}
	}

	@State(Scope.Benchmark)
	public static class MainState<T extends ImageGray<T>> {
		@Param({"true", "false"})
		boolean concurrent;

		@Param({"SB_U8", "SB_F32"})
		String imageTypeName;

		T input;
		GrayF32 intensity;

		FastCornerDetector<T> fast9, fast12;

		@Setup public void setup() {
			Class<T> imageType = ImageType.stringToType(imageTypeName, 3).getImageClass();
			var rand = new Random(234234);

			input = GeneralizedImageOps.createSingleBand(imageType, imageSize, imageSize);
			intensity = input.createSameShape(GrayF32.class);

			GImageMiscOps.fillUniform(input, rand, 0, 255);


			if (concurrent) {
				fast9 = imageType == GrayU8.class ?
						new FastCornerDetector_MT(new ImplFastCorner9_U8(60)) :
						new FastCornerDetector_MT(new ImplFastCorner9_F32(60));
				fast12 = imageType == GrayU8.class ?
						new FastCornerDetector_MT(new ImplFastCorner12_U8(60)) :
						new FastCornerDetector_MT(new ImplFastCorner12_F32(60));
			} else {
				fast9 = imageType == GrayU8.class ?
						new FastCornerDetector(new ImplFastCorner9_U8(60)) :
						new FastCornerDetector(new ImplFastCorner9_F32(60));
				fast12 = imageType == GrayU8.class ?
						new FastCornerDetector(new ImplFastCorner12_U8(60)) :
						new FastCornerDetector(new ImplFastCorner12_F32(60));
			}
		}
	}

	// @formatter:off
	@Benchmark public void naive9(NaiveState state) {state.naive9.process(state.input);}
	@Benchmark public void fast9(MainState state) {state.fast9.process(state.input);}
	@Benchmark public void fast12(MainState state) {state.fast12.process(state.input);}
	// @formatter:on

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkFastIntensity.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
