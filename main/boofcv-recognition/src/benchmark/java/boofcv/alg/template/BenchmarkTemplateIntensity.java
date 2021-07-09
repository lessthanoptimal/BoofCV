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

package boofcv.alg.template;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.template.FactoryTemplateMatching;
import boofcv.factory.template.TemplateScoreType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkTemplateIntensity {

	@Param({"true", "false"})
	public boolean concurrent = false;

	//	@Param({"500","5000"})
	public int size = 1000;

	GrayF32 image_F32 = new GrayF32(1, 1);
	GrayU8 image_U8 = new GrayU8(1, 1);

	GrayF32 template_F32 = new GrayF32(20, 20);
	GrayU8 template_U8 = new GrayU8(20, 20);

	TemplateMatchingIntensity<GrayF32> ssd_F32;
	TemplateMatchingIntensity<GrayU8> ssd_U8;

	TemplateMatchingIntensity<GrayF32> ncc_F32;
	TemplateMatchingIntensity<GrayU8> ncc_U8;

	TemplateMatchingIntensity<GrayF32> sqnorm_F32;
	TemplateMatchingIntensity<GrayU8> sqnorm_U8;

	TemplateMatchingIntensity<GrayF32> correlation_F32;

	@Setup
	public void setup() {
		Random rand = new Random(234);

		BoofConcurrency.USE_CONCURRENT = concurrent;

		image_F32.reshape(size, size);
		image_U8.reshape(size, size);

		GImageMiscOps.fillUniform(image_F32, rand, 0, 200);
		GImageMiscOps.fillUniform(image_U8, rand, 0, 200);

		GImageMiscOps.fillUniform(template_F32, rand, 0, 200);
		GImageMiscOps.fillUniform(template_U8, rand, 0, 200);

		ssd_F32 = FactoryTemplateMatching.createIntensity(TemplateScoreType.SUM_SQUARE_ERROR, GrayF32.class);
		ssd_U8 = FactoryTemplateMatching.createIntensity(TemplateScoreType.SUM_SQUARE_ERROR, GrayU8.class);

		ncc_F32 = FactoryTemplateMatching.createIntensity(TemplateScoreType.NCC, GrayF32.class);
		ncc_U8 = FactoryTemplateMatching.createIntensity(TemplateScoreType.NCC, GrayU8.class);

		sqnorm_F32 = FactoryTemplateMatching.createIntensity(TemplateScoreType.SQUARED_DIFFERENCE_NORMED, GrayF32.class);
		sqnorm_U8 = FactoryTemplateMatching.createIntensity(TemplateScoreType.SQUARED_DIFFERENCE_NORMED, GrayU8.class);

		correlation_F32 = FactoryTemplateMatching.createIntensity(TemplateScoreType.CORRELATION, GrayF32.class);
	}

	@Benchmark
	public void ssd_F32() {
		ssd_F32.setInputImage(image_F32);
		ssd_F32.process(template_F32);
	}

	@Benchmark
	public void ssd_U8() {
		ssd_U8.setInputImage(image_U8);
		ssd_U8.process(template_U8);
	}

	@Benchmark
	public void ncc_F32() {
		ncc_F32.setInputImage(image_F32);
		ncc_F32.process(template_F32);
	}

	@Benchmark
	public void ncc_U8() {
		ncc_U8.setInputImage(image_U8);
		ncc_U8.process(template_U8);
	}

	@Benchmark
	public void sqnorm_F32() {
		sqnorm_F32.setInputImage(image_F32);
		sqnorm_F32.process(template_F32);
	}

	@Benchmark
	public void sqnorm_U8() {
		sqnorm_U8.setInputImage(image_U8);
		sqnorm_U8.process(template_U8);
	}

	@Benchmark
	public void correlation_F32() {
		correlation_F32.setInputImage(image_F32);
		correlation_F32.process(template_F32);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkTemplateIntensity.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
