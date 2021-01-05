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
import boofcv.factory.geo.EnumEssential;
import boofcv.factory.geo.EnumFundamental;
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
public class BenchmarkFundamentalMinimal {
	static final int NUM_POINTS = 2000;

	static List<AssociatedPair> pairs;

	static DMatrixRMaj found = new DMatrixRMaj(3, 3);

	@State(Scope.Benchmark) public static class PixelState {
		@Setup public void setup() {
			var scene = new ArtificialStereoScene();
			scene.init(NUM_POINTS, true, false);
			pairs = scene.pairs;
		}
	}

	@State(Scope.Benchmark) public static class NormState {
		@Setup public void setup() {
			var scene = new ArtificialStereoScene();
			scene.init(NUM_POINTS, false, false);
			pairs = scene.pairs;
		}
	}

	@Benchmark public double Pixel_Linear_8( PixelState s ) {
		return minimal(FactoryMultiView.fundamental_1(EnumFundamental.LINEAR_8, 0));
	}

	@Benchmark public double Pixel_Linear_7( PixelState s ) {
		return minimal(FactoryMultiView.fundamental_1(EnumFundamental.LINEAR_7, 1));
	}

	@Benchmark public double Norm_Linear_8( NormState s ) {
		return minimal(FactoryMultiView.essential_1(EnumEssential.LINEAR_8, 0));
	}

	@Benchmark public double Norm_Linear_7( NormState s ) {
		return minimal(FactoryMultiView.essential_1(EnumEssential.LINEAR_7, 1));
	}

	@Benchmark public double Norm_Nister_5( NormState s ) {
		return minimal(FactoryMultiView.essential_1(EnumEssential.NISTER_5, 1));
	}

	private double minimal( Estimate1ofEpipolar alg ) {
		int setSize = alg.getMinimumPoints();
		List<AssociatedPair> set = new ArrayList<>();
		for (int i = setSize; i < pairs.size(); i++) {
			set.clear();
			for (int j = 0; j < setSize; j++) {
				set.add(pairs.get(i + j - setSize));
			}
			alg.process(set, found);
		}
		return found.get(0, 0);
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkFundamentalMinimal.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
