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

package boofcv.alg.geo;

import boofcv.alg.geo.triangulate.PixelDepthLinearMetric;
import boofcv.alg.geo.triangulate.Triangulate2ViewsGeometricMetric;
import boofcv.alg.geo.triangulate.TriangulateMetricLinearDLT;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkTriangulate extends ArtificialStereoScene {
	@Param({"20", "2000"})
	public int numPoints;

	private final Point4D_F64 found4 = new Point4D_F64();
	private final Point3D_F64 found3 = new Point3D_F64();

	private final TriangulateMetricLinearDLT dlt = new TriangulateMetricLinearDLT();
	private final Triangulate2ViewsGeometricMetric view2 = new Triangulate2ViewsGeometricMetric();
	private final PixelDepthLinearMetric pixelDepth = new PixelDepthLinearMetric();

	@Setup public void setup() {
		init(numPoints, false, false);
	}

	@Benchmark public void dlt() {
		for (int i = 0; i < numPoints; i++)
			dlt.triangulate(pairs.get(i).p1, pairs.get(i).p2, motion, found4);
	}

	@Benchmark public void view2() {
		for (int i = 0; i < numPoints; i++)
			view2.triangulate(pairs.get(i).p1, pairs.get(i).p2, motion, found3);
	}

	@Benchmark public void depth() {
		for (int i = 0; i < numPoints; i++)
			pixelDepth.depth2View(pairs.get(i).p1, pairs.get(i).p2, motion);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkTriangulate.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
