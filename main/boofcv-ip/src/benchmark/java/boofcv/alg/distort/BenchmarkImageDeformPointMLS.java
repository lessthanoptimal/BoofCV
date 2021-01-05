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

package boofcv.alg.distort;

import boofcv.alg.distort.mls.ImageDeformPointMLS_F32;
import boofcv.alg.distort.mls.TypeDeformMLS;
import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point2D_F32;
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
public class BenchmarkImageDeformPointMLS {
	@Param({"5", "1000"})
	public int size;

	ImageDeformPointMLS_F32 affine = new ImageDeformPointMLS_F32(TypeDeformMLS.AFFINE);
	ImageDeformPointMLS_F32 rigid = new ImageDeformPointMLS_F32(TypeDeformMLS.RIGID);
	ImageDeformPointMLS_F32 similarity = new ImageDeformPointMLS_F32(TypeDeformMLS.SIMILARITY);

	@Setup public void configure() {
		addPoints(size, affine);
		addPoints(size, rigid);
		addPoints(size, similarity);
	}

	private void addPoints( int N, ImageDeformPointMLS_F32 alg ) {
		alg.reset();
		alg.configure(N, N, 30, 30);
		Random rand = new Random(2345);
		for (int i = 0; i < N; i++) {
			float sx = rand.nextFloat()*(N - 1);
			float sy = rand.nextFloat()*(N - 1);
			float dx = sx + (rand.nextFloat() - 0.5f)*20f;
			float dy = sy + (rand.nextFloat() - 0.5f)*20f;
			dx = BoofMiscOps.bound(dx, 0, N - 1);
			dy = BoofMiscOps.bound(dy, 0, N - 1);

			alg.add(sx, sy, dx, dy);
		}
		alg.fixate();
	}

	private void apply( ImageDeformPointMLS_F32 alg ) {
		Point2D_F32 out = new Point2D_F32();
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				alg.compute(x, y, out);
			}
		}
	}

	@Benchmark public void affine_distort() {
		apply(affine);
	}

	@Benchmark public void rigid_distort() {
		apply(rigid);
	}

	@Benchmark public void similarity_distort() {
		apply(similarity);
	}

	@Benchmark public void rigid_distort_fixate() {
		rigid.fixate();
		apply(rigid);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkImageDeformPointMLS.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
