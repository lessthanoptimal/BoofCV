/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.associate.ScoreAssociateHamming_B;
import boofcv.abst.feature.associate.ScoreAssociateNccFeature;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.misc.Performer;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.feature.*;
import org.ddogleg.struct.FastQueue;

import java.util.Random;


/**
 * Compares different scoring functions.
 *
 * @author Peter Abeles
 */
public class BenchmarkFeatureScore {

	static final long TEST_TIME = 1000;
	static final Random rand = new Random(234234);
	static final int NUM_FEATURES = 2000;

	static final int DOF_TUPLE = 64;
	static final int DOF_BRIEF = 512;

	static final FastQueue<TupleDesc_F64> listA = createSet();
	static final FastQueue<TupleDesc_F64> listB = createSet();

	static final FastQueue<TupleDesc_B> briefA = createBriefSet();
	static final FastQueue<TupleDesc_B> briefB = createBriefSet();

	static final FastQueue<NccFeature> nccA = createNccSet();
	static final FastQueue<NccFeature> nccB = createNccSet();

	public static class General implements Performer {

		ScoreAssociation alg;
		String name;

		public General(String name, ScoreAssociation alg) {
			this.alg = alg;
			this.name = name;
		}

		@Override
		public void process() {
			for( int i = 0; i < listA.size; i++ )
				for( int j = 0; j < listB.size; j++ )
					alg.score(listA.data[i],listB.data[j]);
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public static class Brief extends PerformerBase {

		ScoreAssociateHamming_B scorer = new ScoreAssociateHamming_B();

		@Override
		public void process() {
			for( int i = 0; i < briefA.size; i++ )
				for( int j = 0; j < briefB.size; j++ )
					scorer.score(briefA.data[i],briefB.data[j]);
		}
	}

	public static class Ncc extends PerformerBase {

		ScoreAssociateNccFeature scorer = new ScoreAssociateNccFeature();

		@Override
		public void process() {
			for( int i = 0; i < nccA.size; i++ )
				for( int j = 0; j < nccB.size; j++ )
					scorer.score(nccA.data[i],nccB.data[j]);
		}
	}

	private static FastQueue<TupleDesc_F64> createSet() {
		FastQueue<TupleDesc_F64> ret = new FastQueue<TupleDesc_F64>(10,TupleDesc_F64.class, true) {
			@Override
			protected TupleDesc_F64 createInstance() {
				return new TupleDesc_F64(DOF_TUPLE);
			}
		};
		for( int i = 0; i < NUM_FEATURES; i++ ) {
			TupleDesc_F64 t = ret.grow();
			for( int j = 0; j < DOF_TUPLE; j++ ) {
				t.value[j] = (rand.nextDouble()-0.5)*20;
			}
		}
		return ret;
	}

	private static FastQueue<TupleDesc_B> createBriefSet() {
		FastQueue<TupleDesc_B> ret = new BriefFeatureQueue(DOF_BRIEF);

		for( int i = 0; i < NUM_FEATURES; i++ ) {
			TupleDesc_B t = ret.grow();
			for( int j = 0; j < t.data.length; j++ ) {
				t.data[j] = rand.nextInt();
			}
		}
		return ret;
	}

	private static FastQueue<NccFeature> createNccSet() {
		FastQueue<NccFeature> ret = new NccFeatureQueue(DOF_TUPLE);

		for( int i = 0; i < NUM_FEATURES; i++ ) {
			NccFeature t = ret.grow();
			for( int j = 0; j < t.value.length; j++ ) {
				t.value[j] = (rand.nextDouble()-0.5)*20;
			}
			t.mean = (rand.nextDouble()-0.5)*20;
			t.sigma = (rand.nextDouble()-0.5)*20;
		}
		return ret;
	}

	public static void main( String argsp[ ] ) {
		System.out.println("=========  Profile Description Length "+ DOF_TUPLE +" ========== Num Features "+NUM_FEATURES);
		System.out.println();

		// the "fastest" seems to always be the first one tested
//		ProfileOperation.printOpsPerSec(new General("Correlation", new ScoreAssociateCorrelation()),TEST_TIME);
//		ProfileOperation.printOpsPerSec(new General("Euclidean", new ScoreAssociateEuclidean_F64()),TEST_TIME);
//		ProfileOperation.printOpsPerSec(new General("Euclidean Sq", new ScoreAssociateEuclideanSq_F64()),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Brief(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Ncc(),TEST_TIME);

	}
}
