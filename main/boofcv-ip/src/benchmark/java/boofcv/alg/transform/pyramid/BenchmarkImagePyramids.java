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

package boofcv.alg.transform.pyramid;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidFloat;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
@SuppressWarnings("unchecked")
public class BenchmarkImagePyramids<T extends ImageGray<T>> {
	static int size = 800;

	@Param({"SB_U8", "SB_F32"})
	String imageTypeName;

	Class<T> imageType;

	T input;

	ConfigDiscreteLevels configD = ConfigDiscreteLevels.levels(4);
	double[] scalesF = new double[]{1, 2, 4, 8};

	PyramidDiscrete<T> pyramidD;
	PyramidFloat<T> pyramidF;

	@Setup public void setup() {
		imageType = ImageType.stringToType(imageTypeName, 3).getImageClass();
		input = GeneralizedImageOps.createImage(imageType, size, size, 1);

		Random rand = new Random(234);
		GImageMiscOps.fillUniform(input, rand, 0, 100);
		Kernel1D kernel;
		if (input.getImageType().getDataType().isInteger())
			kernel = FactoryKernelGaussian.gaussian(Kernel1D_S32.class, -1.0, 2);
		else
			kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, -1.0, 2);
		pyramidD = new PyramidDiscreteSampleBlur<>(kernel, 2, ImageType.single(imageType), true, configD);
		pyramidF = FactoryPyramid.scaleSpacePyramid(scalesF, imageType);
	}

	@Benchmark public void Float() {pyramidF.process(input);}

	@Benchmark public void Discrete() {pyramidD.process(input);}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkImagePyramids.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
