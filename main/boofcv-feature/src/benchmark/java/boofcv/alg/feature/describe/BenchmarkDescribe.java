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

package boofcv.alg.feature.describe;

import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GConvertImage;
import boofcv.factory.feature.describe.FactoryDescribeAlgs;
import boofcv.factory.feature.describe.FactoryDescribePointRadiusAngle;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_I32;
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
@SuppressWarnings("unchecked")
public class BenchmarkDescribe<I extends ImageGray<I>, D extends ImageGray<D>, II extends ImageGray<II>> {

	@Param({"800"})
	static int numPoints;

	@Param({"SB_U8", "SB_F32"})
	String imageTypeName;

	Class<I> imageType;

	I gray;
	Planar<I> colorMS;

	Point2D_I32[] pts;
	double[] scales;
	double[] yaws;

	Class<D> derivType;
	Class<II> integralType;

	@Setup public void setup() {
		int width = 640, height = 480;

		var rand = new Random(234234);

		imageType = ImageType.stringToType(imageTypeName, 3).getImageClass();

		derivType = GImageDerivativeOps.getDerivativeType(imageType);
		integralType = GIntegralImageOps.getIntegralType(imageType);

		colorMS = new Planar<>(imageType, width, height, 3);
		GImageMiscOps.fillUniform(colorMS, rand, 0, 100);

		gray = GConvertImage.average(colorMS, gray);

		pts = new Point2D_I32[numPoints];
		scales = new double[numPoints];
		yaws = new double[numPoints];
		int border = 20;
		for (int i = 0; i < numPoints; i++) {
			int x = rand.nextInt(width - border*2) + border;
			int y = rand.nextInt(height - border*2) + border;
			pts[i] = new Point2D_I32(x, y);
			scales[i] = rand.nextDouble()*3 + 1;
			yaws[i] = 2.0*(rand.nextDouble() - 0.5)*Math.PI;
		}
	}

	@Benchmark public void Brief512() {
		DescribePointBrief<I> alg = FactoryDescribeAlgs.brief(FactoryBriefDefinition.gaussian2(new Random(123), 16, 512),
				FactoryBlurFilter.gaussian(imageType, 0, 4));

		alg.setImage(gray);
		TupleDesc_B f = alg.createFeature();
		for (int i = 0; i < pts.length; i++) {
			Point2D_I32 p = pts[i];
			alg.process(p.x, p.y, f);
		}
	}

	@Benchmark public void BriefSO512() {
		int briefRadius = 16;
		DescribePointBriefSO<I> alg = FactoryDescribeAlgs.
				briefso(FactoryBriefDefinition.gaussian2(new Random(123), briefRadius, 512),
						FactoryBlurFilter.gaussian(imageType, 0, 4));

		alg.setImage(gray);
		TupleDesc_B f = alg.createFeature();
		for (int i = 0; i < pts.length; i++) {
			Point2D_I32 p = pts[i];
			alg.process(p.x, p.y, (float)yaws[i], (float)(briefRadius*scales[i]), f);
		}
	}

	@Benchmark public void SURF_F() {
		process(FactoryDescribePointRadiusAngle.<I, II>surfFast(null, imageType));
	}

	@Benchmark public void SURF_F_Color() {
		process(FactoryDescribePointRadiusAngle.surfColorFast(null, ImageType.pl(3, imageType)));
	}

	@Benchmark public void SURF_S() {
		process(FactoryDescribePointRadiusAngle.<I, II>surfStable(null, imageType));
	}

	@Benchmark public void SURF_S_Color() {
		process(FactoryDescribePointRadiusAngle.surfColorStable(null, ImageType.pl(3, imageType)));
	}

	@Benchmark public void SIFT() {
		process(FactoryDescribePointRadiusAngle.sift(null, null, imageType));
	}

	public <PD extends TupleDesc> void process( DescribePointRadiusAngle alg ) {
		if (alg.getImageType().getFamily() == ImageType.Family.GRAY)
			alg.setImage(gray);
		else
			alg.setImage(colorMS);

		PD d = (PD)alg.createDescription();
		for (int i = 0; i < pts.length; i++) {
			Point2D_I32 p = pts[i];
			alg.process(p.x, p.y, yaws[i], scales[i], d);
		}
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkDescribe.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
