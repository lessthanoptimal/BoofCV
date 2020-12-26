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

package boofcv.alg.feature.describe;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseScaleGradient;
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
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkSurfDescribeOps<T extends ImageGray<T>>
{
	@Param({"SB_S32", "SB_F32"})
	String imageTypeName;

	Class<T> imageType;
	
	T input;

	// parameters for region gradient
	double tl_x = 100.0;
	double tl_y = 120.3;
	double period = 1.2;
	int regionSize = 20;
	double kernelWidth = 5;
	double[] derivX = new double[ regionSize*regionSize ];
	double[] derivY = new double[ regionSize*regionSize ];

	@Setup public void setup() {
		int width = 640, height = 480;

		var rand = new Random(234234);

		imageType = ImageType.stringToType(imageTypeName, 3).getImageClass();
		input = GeneralizedImageOps.createSingleBand(imageType,width,height);
		GImageMiscOps.fillUniform(input, rand, 0, 1);

	}

	@Benchmark public void Not_Haar() {
		SurfDescribeOps.gradient(input, tl_x, tl_y, period, regionSize,
				kernelWidth, false, derivX, derivY);
	}

	@Benchmark public void Haar() {
		SurfDescribeOps.gradient(input, tl_x , tl_y , period, regionSize,
				kernelWidth,true,derivX,derivY);
	}

	@Benchmark public void Not_Haar_Border() {
		SparseScaleGradient<T,?> g = SurfDescribeOps.createGradient(false,imageType);
		g.setWidth(kernelWidth);
		g.setImage(input);
		sampleBorder(g);
	}

	@Benchmark public void Haar_Border() {
		SparseScaleGradient<T,?> g = SurfDescribeOps.createGradient(true,imageType);
		g.setWidth(kernelWidth);
		g.setImage(input);
		sampleBorder(g);
	}

	/**
	 * Sample the gradient using SparseImageGradient instead of the completely
	 * unrolled code
	 */
	public void sampleBorder(SparseScaleGradient<T,?> g) {
		for (int iter = 0; iter < 500; iter++) {
			double tl_x = this.tl_x + 0.5;
			double tl_y = this.tl_y + 0.5;

			int j = 0;
			for( int y = 0; y < regionSize; y++ ) {
				for( int x = 0; x < regionSize; x++ , j++) {
					int xx = (int)(tl_x + x * period);
					int yy = (int)(tl_y + y * period);

					GradientValue deriv = g.compute(xx,yy);
					derivX[j] = deriv.getX();
					derivY[j] = deriv.getY();
				}
			}
		}
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkSurfDescribeOps.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
