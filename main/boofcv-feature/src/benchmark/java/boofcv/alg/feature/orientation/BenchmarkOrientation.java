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

package boofcv.alg.feature.orientation;

import boofcv.BoofTesting;
import boofcv.abst.feature.orientation.*;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static boofcv.factory.feature.orientation.FactoryOrientationAlgs.*;
import static java.lang.Math.PI;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkOrientation<I extends ImageGray<I>, D extends ImageGray<D>> {

	@Param({"SB_U8", "SB_F32"})
	String imageTypeName;

	@Param({"300"})
	public int size;

	int NUM_POINTS = 1000;
	static int RADIUS = 6;
	static double OBJECt_TO_SCALE = 1.0/2.0;

	I image;
	D derivX;
	D derivY;
	ImageGray ii;

	Point2D_I32[] pts;
	double[] radiuses;

	Class<I> imageType;
	Class<D> derivType;
	Class integralType;

	ConfigAverageIntegral confAverageIIW = new ConfigAverageIntegral();
	ConfigSlidingIntegral confSlidingIIW = new ConfigSlidingIntegral();

	@Setup public void setup() {
		Random rand = new Random(BoofTesting.BASE_SEED);

		imageType = ImageType.stringToType(imageTypeName, 0).getImageClass();
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);

		integralType = GrayF32.class == imageType ? GrayF32.class : GrayS32.class;

		image = GeneralizedImageOps.createSingleBand(imageType, size, size);
		ii = GeneralizedImageOps.createSingleBand(integralType, size, size);
		derivX = GeneralizedImageOps.createSingleBand(derivType, size, size);
		derivY = GeneralizedImageOps.createSingleBand(derivType, size, size);

		GImageMiscOps.fillUniform(image, rand, 0, 100);
		GIntegralImageOps.transform(image, ii);

		ImageGradient<I, D> gradient = FactoryDerivative.sobel(imageType, derivType);
		gradient.process(image, derivX, derivY);

		pts = new Point2D_I32[NUM_POINTS];
		radiuses = new double[NUM_POINTS];
		int border = 6;
		for (int i = 0; i < NUM_POINTS; i++) {
			int x = rand.nextInt(size - border*2) + border;
			int y = rand.nextInt(size - border*2) + border;
			pts[i] = new Point2D_I32(x, y);
			radiuses[i] = rand.nextDouble()*100 + 10;
		}

		confAverageIIW.weightSigma = -1;
		confSlidingIIW.weightSigma = -1;
	}

	// @formatter:off
	@Benchmark public void Average() {gradient(average(OBJECt_TO_SCALE, RADIUS, false, derivType));}
	@Benchmark public void Average_W() {gradient(average(OBJECt_TO_SCALE, RADIUS, true, derivType));}
	@Benchmark public void Histogram() {gradient(histogram(0.5, 15, RADIUS, false, derivType));}
	@Benchmark public void Histogram_W() {gradient(histogram(0.5, 15, RADIUS, true, derivType));}
	@Benchmark public void Sliding() {gradient(sliding(OBJECt_TO_SCALE, 15, PI/3.0, RADIUS, false, derivType));}
	@Benchmark public void Sliding_W() {gradient(sliding(OBJECt_TO_SCALE, 15, PI/3.0, RADIUS, true, derivType));}

	@Benchmark public void SIFT() {image(FactoryOrientation.sift(null, null, imageType));}
	@Benchmark public void No_Gradient() {image(nogradient(OBJECt_TO_SCALE, RADIUS, imageType));}

	@Benchmark public void Image_II() {integral(image_ii(1.0/2.0, RADIUS, 1, 4, 0, integralType));}
	@Benchmark public void Image_II_W() {integral(image_ii(1.0/2.0, RADIUS, 1, 4, -1, integralType));}
	@Benchmark public void Average_II() {integral(average_ii(null, integralType));}
	@Benchmark public void Average_II_W() {integral(average_ii(confAverageIIW, integralType));}
	@Benchmark public void Sliding_II() {integral(sliding_ii(null, integralType));}
	@Benchmark public void Sliding_II_W() {integral(sliding_ii(confSlidingIIW, integralType));}
	// @formatter:on

	void gradient( OrientationGradient<D> alg ) {
		alg.setImage(derivX, derivY);
		for (int i = 0; i < pts.length; i++) {
			Point2D_I32 p = pts[i];
			alg.setObjectRadius(radiuses[i]);
			alg.compute(p.x, p.y);
		}
	}

	void image( OrientationImage<I> alg ) {
		alg.setImage(image);
		for (int i = 0; i < pts.length; i++) {
			Point2D_I32 p = pts[i];
			alg.setObjectRadius(radiuses[i]);
			alg.compute(p.x, p.y);
		}
	}

	void integral( OrientationIntegral alg ) {
		alg.setImage(ii);
		for (int i = 0; i < pts.length; i++) {
			Point2D_I32 p = pts[i];
			alg.setObjectRadius(radiuses[i]);
			alg.compute(p.x, p.y);
		}
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkOrientation.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
