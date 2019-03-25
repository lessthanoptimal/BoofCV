/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.struct.FastQueue;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;


/**
 * @author Peter Abeles
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@Fork(value=1)
public class BenchmarkAssociationSpeedRandom {

	@Param({"true","false"})
	boolean concurrent;

	@Param({"5","100"})
	int DOF = 50;

	int NUM_FEATURES = 1000;

	Random rand = new Random(234234);
	FastQueue<TupleDesc_F64> listA = createSet(rand);
	FastQueue<TupleDesc_F64> listB = createSet(rand);

	ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.scoreEuclidean(TupleDesc_F64.class,true);

	AssociateDescription<TupleDesc_F64> greedy;
	AssociateDescription<TupleDesc_F64> greedyBackwards;
	AssociateDescription<TupleDesc_F64> kdtree;
	AssociateDescription<TupleDesc_F64> forest;

	@Setup
	public void setup() {
		BoofConcurrency.USE_CONCURRENT = concurrent;

		greedy = FactoryAssociation.greedy(score, Double.MAX_VALUE, false);
		greedyBackwards = FactoryAssociation.greedy(score, Double.MAX_VALUE, true);
		kdtree = FactoryAssociation.kdtree(null,DOF,500);
		forest = FactoryAssociation.kdRandomForest(null,DOF,500,15,5,1233445565);

//		kdtree.setSource(listA);
	}

	@Benchmark
	public void greedy() {
		greedy.setSource(listA);
		greedy.setDestination(listB);
		greedy.associate();
	}

	@Benchmark
	public void greedyBackwards() {
		greedyBackwards.setSource(listA);
		greedyBackwards.setDestination(listB);
		greedyBackwards.associate();
	}

	@Benchmark
	public void forest() {
		forest.setSource(listA);
		forest.setDestination(listB);
		forest.associate();
	}

//	@Benchmark
//	public void kdtree_fixed_src() {
////		kdtree.setSource(listA);
//		kdtree.setDestination(listB);
//		kdtree.associate();
//	}

	private FastQueue<TupleDesc_F64> createSet( Random rand ) {
		FastQueue<TupleDesc_F64> ret = new FastQueue<>(10, TupleDesc_F64.class, () -> new TupleDesc_F64(DOF));

		for( int i = 0; i < NUM_FEATURES; i++ ) {
			TupleDesc_F64 t = ret.grow();
			for( int j = 0; j < DOF; j++ ) {
				t.value[j] = (rand.nextDouble()-0.5)*20;
			}
		}
		return ret;
	}

}
