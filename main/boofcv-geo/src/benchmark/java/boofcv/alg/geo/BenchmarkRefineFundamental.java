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

import boofcv.abst.geo.RefineEpipolar;
import boofcv.factory.geo.EpipolarError;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

import static boofcv.factory.geo.FactoryMultiView.fundamentalRefine;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkRefineFundamental extends ArtificialStereoScene {
	static final int NUM_POINTS = 1000;

	@Param({"true", "false"})
	static boolean FUNDAMENTAL;

	protected DMatrixRMaj initialF;
	protected DMatrixRMaj refinement = new DMatrixRMaj(3, 3);

	@Setup public void setup() {
		init(NUM_POINTS, FUNDAMENTAL, false);

		DMatrixRMaj E = MultiViewOps.createEssential(motion.R,motion.T,null);
		DMatrixRMaj F =	MultiViewOps.createFundamental(E,K);

		initialF = FUNDAMENTAL ? F : E;
		RandomMatrices_DDRM.addUniform(initialF,-0.05,0.05,rand);
	}

	// @formatter:off
	@Benchmark public void LS_Sampson() {process(fundamentalRefine(1e-5, 30, EpipolarError.SAMPSON));}
	@Benchmark public void LS_Simple() {process(fundamentalRefine(1e-5, 30, EpipolarError.SIMPLE));}
	@Benchmark public void QN_Simple() {process(new QuasiNewtonFundamentalSampson(1e-5,30));}
	// @formatter:on

	public void process(RefineEpipolar alg) {
		alg.fitModel(pairs, initialF, refinement);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkRefineFundamental.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
