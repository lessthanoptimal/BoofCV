/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.lines;

import boofcv.abst.feature.detect.line.DetectLine;
import boofcv.abst.feature.detect.line.DetectLineSegment;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughGradient;
import boofcv.factory.feature.detect.line.ConfigLineRansac;
import boofcv.factory.feature.detect.line.FactoryDetectLine;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
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
public class BenchmarkDetectLines<T extends ImageGray<T>, D extends ImageGray<D>> {
	@Param({"true","false"})
	public boolean concurrent;

	@Param({"1000"})
	public int width;

	T input;
	Class<T> imageType;

	DetectLine<T> houghFoot;
	DetectLine<T> houghPolar;
	DetectLine<T> houghFootSub;

	DetectLineSegment<T> detectorSegment;

	public BenchmarkDetectLines() {
		this((Class)GrayU8.class);
	}

	public BenchmarkDetectLines( Class<T> imageType ) {
		this.imageType = imageType;
		input = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
	}

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;

		// fill it with a few rectangles so that there are some lines
		input.reshape(width,width);
		GImageMiscOps.fill(input,0);
		GImageMiscOps.fillRectangle(input,100,10,15,width/4,width/4);
		GImageMiscOps.fillRectangle(input,100,width/2,width/2+15,width/4,width/4);
		GImageMiscOps.fillRectangle(input,100,width/2,0,width/8,width/8);
		GImageMiscOps.addUniform(input,new Random(234),0,20);

		houghFoot = FactoryDetectLine.houghLineFoot(null,null, imageType);
		houghPolar = FactoryDetectLine.houghLinePolar((ConfigHoughGradient)null,null, imageType);
		houghFootSub = FactoryDetectLine.houghLineFootSub(null, imageType);
		detectorSegment = FactoryDetectLine.lineRansac(new ConfigLineRansac(40, 30, 2.36, true), imageType);
	}

	@Benchmark public void gradientHoughFoot() {
		houghFoot.detect(input);
	}

	@Benchmark public void gradientHoughPolar() {
		houghPolar.detect(input);
	}

	@Benchmark public void gradientHoughFootSub() {
		houghFootSub.detect(input);
	}

	@Benchmark public void segment() {
		detectorSegment.detect(input);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkDetectLines.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
