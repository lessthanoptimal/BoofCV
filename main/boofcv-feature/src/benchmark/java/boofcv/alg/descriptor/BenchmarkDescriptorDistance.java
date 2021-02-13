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

package boofcv.alg.descriptor;

import boofcv.struct.feature.TupleDesc_B;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ResultOfMethodCallIgnored") @BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkDescriptorDistance {

	static int NUM_FEATURES = 10000;

	List<TupleDesc_B> binaryA = new ArrayList<>();
	List<TupleDesc_B> binaryB = new ArrayList<>();
	HammingTable16 table = new HammingTable16();

	@Setup public void setup() {
		Random rand = new Random(234234);
		binaryA = new ArrayList<>();
		binaryB = new ArrayList<>();
		for (int i = 0; i < NUM_FEATURES; i++) {
			binaryA.add(randomFeature(rand));
			binaryB.add(randomFeature(rand));
		}
	}

	@Benchmark public void hammingTable() {
		for (int i = 0; i < binaryA.size(); i++) {
			tableScore(binaryA.get(i), binaryB.get(i));
		}
	}

	private int tableScore( TupleDesc_B a, TupleDesc_B b ) {
		int score = 0;

		for (int i = 0; i < a.data.length; i++) {
			int dataA = a.data[i];
			int dataB = b.data[i];

			score += table.lookup((short)dataA, (short)dataB);
			score += table.lookup((short)(dataA >> 16), (short)(dataB >> 16));
		}

		return score;
	}

	@Benchmark public void equationOld() {
		for (int i = 0; i < binaryA.size(); i++) {
			ExperimentalDescriptorDistance.hamming(binaryA.get(i), binaryB.get(i));
		}
	}

	@Benchmark public void equation() {
		for (int i = 0; i < binaryA.size(); i++) {
			DescriptorDistance.hamming(binaryA.get(i), binaryB.get(i));
		}
	}

	private TupleDesc_B randomFeature( Random rand ) {
		TupleDesc_B feat = new TupleDesc_B(512);
		for (int j = 0; j < feat.data.length; j++) {
			feat.data[j] = rand.nextInt();
		}
		return feat;
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkDescriptorDistance.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
