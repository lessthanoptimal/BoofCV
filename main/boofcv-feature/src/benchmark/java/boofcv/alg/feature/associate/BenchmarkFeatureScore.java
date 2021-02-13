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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.*;
import boofcv.struct.feature.*;
import org.ddogleg.struct.DogArray;
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
public class BenchmarkFeatureScore {
	@Param({"2000"})
	static int NUM_FEATURES = 2000;

	static final int DOF_TUPLE = 64;
	static final int DOF_BRIEF = 512;

	DogArray<TupleDesc_F64> listA, listB;
	DogArray<TupleDesc_B> briefA, briefB;
	DogArray<NccFeature> nccA, nccB;

	@Setup public void setup() {
		Random rand = new Random(234234);

		listA = createSet(rand);
		listB = createSet(rand);

		briefA = createBriefSet(rand);
		briefB = createBriefSet(rand);

		nccA = createNccSet(rand);
		nccB = createNccSet(rand);
	}

	@Benchmark public void hamming() {
		var scorer = new ScoreAssociateHamming_B();
		for (int i = 0; i < briefA.size; i++)
			for (int j = 0; j < briefB.size; j++)
				scorer.score(briefA.data[i], briefB.data[j]);
	}

	@Benchmark public void ncc() {
		var scorer = new ScoreAssociateNccFeature();
		for (int i = 0; i < nccA.size; i++)
			for (int j = 0; j < nccB.size; j++)
				scorer.score(nccA.data[i], nccB.data[j]);
	}

	@Benchmark public void sad() {
		var scorer = new ScoreAssociateSad.F64();
		for (int i = 0; i < listA.size; i++)
			for (int j = 0; j < listB.size; j++)
				scorer.score(listA.data[i], listB.data[j]);
	}

	@Benchmark public void euclidean_sq() {
		var scorer = new ScoreAssociateEuclideanSq.F64();
		for (int i = 0; i < listA.size; i++)
			for (int j = 0; j < listB.size; j++)
				scorer.score(listA.data[i], listB.data[j]);
	}

	@Benchmark public void correlation() {
		var scorer = new ScoreAssociateCorrelation();
		for (int i = 0; i < listA.size; i++)
			for (int j = 0; j < listB.size; j++)
				scorer.score(listA.data[i], listB.data[j]);
	}

	private DogArray<TupleDesc_F64> createSet( Random rand ) {
		DogArray<TupleDesc_F64> ret = new DogArray<>(() -> new TupleDesc_F64(DOF_TUPLE));

		for (int i = 0; i < NUM_FEATURES; i++) {
			TupleDesc_F64 t = ret.grow();
			for (int j = 0; j < DOF_TUPLE; j++) {
				t.data[j] = (rand.nextDouble() - 0.5)*20;
			}
		}
		return ret;
	}

	private DogArray<TupleDesc_B> createBriefSet( Random rand ) {
		DogArray<TupleDesc_B> ret = new BriefFeatureQueue(DOF_BRIEF);

		for (int i = 0; i < NUM_FEATURES; i++) {
			TupleDesc_B t = ret.grow();
			for (int j = 0; j < t.data.length; j++) {
				t.data[j] = rand.nextInt();
			}
		}
		return ret;
	}

	private DogArray<NccFeature> createNccSet( Random rand ) {
		DogArray<NccFeature> ret = new NccFeatureQueue(DOF_TUPLE);

		for (int i = 0; i < NUM_FEATURES; i++) {
			NccFeature t = ret.grow();
			for (int j = 0; j < t.data.length; j++) {
				t.data[j] = (rand.nextDouble() - 0.5)*20;
			}
			t.mean = (rand.nextDouble() - 0.5)*20;
			t.sigma = (rand.nextDouble() - 0.5)*20;
		}
		return ret;
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkFeatureScore.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
