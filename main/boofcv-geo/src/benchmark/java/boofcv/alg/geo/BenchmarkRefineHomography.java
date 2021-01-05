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

import boofcv.factory.geo.EpipolarError;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

import static boofcv.factory.geo.FactoryMultiView.homographyRefine;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkRefineHomography extends ArtificialStereoScene {
	@Param({"20", "2000"})
	public int numPoints = 2000;

	protected DMatrixRMaj estimateH = new DMatrixRMaj(3, 3);
	DMatrixRMaj refinedH = new DMatrixRMaj(3, 3);

	ModelFitter<DMatrixRMaj, AssociatedPair> simple;
	ModelFitter<DMatrixRMaj, AssociatedPair> sampson;

	@Setup public void setup() {
		init(numPoints, true, true);

		DMatrixRMaj perfectH = MultiViewOps.createHomography(motion.R, motion.T, 1.0, new Vector3D_F64(0, 0, 1), K);
		estimateH.setTo(perfectH);
		RandomMatrices_DDRM.addUniform(estimateH, -0.05, 0.05, rand);

		double tol = 1e-16;
		int MAX_ITER = 200;
		simple = homographyRefine(tol, MAX_ITER, EpipolarError.SIMPLE);
		sampson = homographyRefine(tol, MAX_ITER, EpipolarError.SAMPSON);
	}

	// @formatter:off
	@Benchmark public void simple() {process(simple);}
	@Benchmark public void sampson() {process(sampson);}
	// @formatter:on

	void process( ModelFitter<DMatrixRMaj, AssociatedPair> alg ) {alg.fitModel(pairs, estimateH, refinedH);}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkRefineHomography.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
