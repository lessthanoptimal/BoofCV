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

package boofcv;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Valuates performance of different map implementations given primitive types.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@State(Scope.Benchmark)
@Fork(value = 1)
public class BenchmarkPrimitiveMap {
//	@Param({"1000", "10000"})
	@Param({"2000"})
	int total;

	int[] keys;

	Map<Integer, Integer> utilMap = new HashMap<>();
	TIntIntMap troveMap = new TIntIntHashMap();

	@Setup public void setup() {
		Random rand = new Random(3245);

		keys = new int[total];

		for (int i = 0; i < total; i++) {
			keys[i] = rand.nextInt();
			utilMap.put(keys[i], i);
			troveMap.put(keys[i], i);
		}
	}

	@Benchmark public void util_put() {
		utilMap.clear();
		for (int i = 0; i < total; i++) {
			utilMap.put(keys[i], i);
		}
	}

	@Benchmark public void util_get( Blackhole blackhole ) {
		for (int i = 0; i < total; i++) {
			blackhole.consume((int)utilMap.get(keys[i]));
		}
	}

	@Benchmark public void util_forEach(Blackhole blackhole) {
		utilMap.forEach((key,value)-> blackhole.consume(value));
	}

	@Benchmark public void trove_put() {
		troveMap.clear();
		for (int i = 0; i < total; i++) {
			troveMap.put(keys[i], i);
		}
	}

	@Benchmark public void trove_get( Blackhole blackhole ) {
		for (int i = 0; i < total; i++) {
			blackhole.consume(troveMap.get(keys[i]));
		}
	}

	@Benchmark public void trove_forEach(Blackhole blackhole) {
		troveMap.forEachEntry((key,value)->{
			blackhole.consume(value);
			return true;
		});
	}

	public static void main( String[] args ) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BenchmarkPrimitiveMap.class.getSimpleName())
				.warmupTime(TimeValue.seconds(1))
				.measurementTime(TimeValue.seconds(1))
				.build();

		new Runner(opt).run();
	}
}
