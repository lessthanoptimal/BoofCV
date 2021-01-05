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

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import org.ejml.data.DMatrixRMaj;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkHomography extends ArtificialStereoScene {
	static final int NUM_POINTS = 2000;

	List<AssociatedPair> pairs4 = new ArrayList<>();

	DMatrixRMaj H = new DMatrixRMaj(3, 3);

	@Setup public void setup() {
		init(NUM_POINTS, true, false);
	}

	@Benchmark public void DLT_Norm_Minimal() {
		Estimate1ofEpipolar alg = FactoryMultiView.homographyDLT(true);
		processMinimal(alg);
	}

	@Benchmark public void DLT_Minimal() {
		Estimate1ofEpipolar alg = FactoryMultiView.homographyDLT(false);
		processMinimal(alg);
	}

	@Benchmark public void TLS_Minimal() {
		Estimate1ofEpipolar alg = FactoryMultiView.homographyTLS();
		processMinimal(alg);
	}

	@Benchmark public void DLT_Norm_All() {
		Estimate1ofEpipolar alg = FactoryMultiView.homographyDLT(true);
		for (int i = 0; i < 1000; i++) {
			alg.process(pairs, H);
		}
	}

	@Benchmark public void TLS_All() {
		Estimate1ofEpipolar alg = FactoryMultiView.homographyTLS();
		for (int trial = 0; trial < 1000; trial++) {
			alg.process(pairs, H);
		}
	}

	private void processMinimal( Estimate1ofEpipolar alg ) {
		for (int trial = 0; trial < 10; trial++) {
			for (int i = 4; i < pairs.size(); i++) {
				pairs4.clear();
				for (int j = 0; j < 4; j++) {
					pairs4.add(pairs.get(i + j - 4));
				}
				alg.process(pairs4, H);
			}
		}
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkHomography.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
