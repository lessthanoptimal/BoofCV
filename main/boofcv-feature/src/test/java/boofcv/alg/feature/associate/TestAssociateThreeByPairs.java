/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.descriptor.UtilFeature;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.AssociatedTripleIndex;
import boofcv.struct.feature.TupleDesc_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestAssociateThreeByPairs {
	@Test
	void perfect() {
		FastQueue<TupleDesc_F64> featuresA = UtilFeature.createQueueF64(1);
		FastQueue<TupleDesc_F64> featuresB = UtilFeature.createQueueF64(1);
		FastQueue<TupleDesc_F64> featuresC = UtilFeature.createQueueF64(1);
		GrowQueue_I32 featuresSetA = new GrowQueue_I32();
		GrowQueue_I32 featuresSetB = new GrowQueue_I32();
		GrowQueue_I32 featuresSetC = new GrowQueue_I32();

		featuresB.grow().set(234234234);
		featuresC.grow().set(2344234);
		featuresC.grow().set(99234234);

		for (int i = 0; i < 10; i++) {
			featuresA.grow().set(i);
			featuresB.grow().set(i);
			featuresC.grow().set(i);
		}

		// there is only one set
		featuresSetA.resize(featuresA.size);
		featuresSetA.fill(0);
		featuresSetB.resize(featuresB.size);
		featuresSetB.fill(0);
		featuresSetC.resize(featuresC.size);
		featuresSetC.fill(0);

		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.defaultScore(TupleDesc_F64.class);
		AssociateDescription<TupleDesc_F64> associate = FactoryAssociation.greedy(new ConfigAssociateGreedy(true,1e-8),score);

		AssociateThreeByPairs<TupleDesc_F64> alg = new AssociateThreeByPairs<>(associate,TupleDesc_F64.class);

		alg.initialize(1);
		alg.setFeaturesA(featuresA, featuresSetA);
		alg.setFeaturesB(featuresB, featuresSetB);
		alg.setFeaturesC(featuresC, featuresSetC);

		alg.associate();

		FastQueue<AssociatedTripleIndex> matches = alg.getMatches();

		assertEquals(10,matches.size);

		for (int i = 0; i < 10; i++) {
			AssociatedTripleIndex a = matches.get(i);
			assertEquals(i,a.a);
			assertEquals(i+1,a.b);
			assertEquals(i+2,a.c);
		}
	}

	/**
	 * A->B is good. B->C is good. C->A exceeds error margin
	 */
	@Test
	void failOnCtoA() {
		FastQueue<TupleDesc_F64> featuresA = UtilFeature.createQueueF64(1);
		FastQueue<TupleDesc_F64> featuresB = UtilFeature.createQueueF64(1);
		FastQueue<TupleDesc_F64> featuresC = UtilFeature.createQueueF64(1);
		GrowQueue_I32 featuresSetA = new GrowQueue_I32();
		GrowQueue_I32 featuresSetB = new GrowQueue_I32();
		GrowQueue_I32 featuresSetC = new GrowQueue_I32();

		featuresB.grow().set(234234234);
		featuresC.grow().set(2344234);
		featuresC.grow().set(99234234);

		for (int i = 0; i < 10; i++) {
			featuresA.grow().set(i);
			featuresB.grow().set(i+0.1);
			featuresC.grow().set(i+0.2);
		}

		// there is only one set
		featuresSetA.resize(featuresA.size);
		featuresSetA.fill(0);
		featuresSetB.resize(featuresB.size);
		featuresSetB.fill(0);
		featuresSetC.resize(featuresC.size);
		featuresSetC.fill(0);

		double maxError = 0.1*0.1+0.00000001;
		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.defaultScore(TupleDesc_F64.class);
		AssociateDescription<TupleDesc_F64> associate = FactoryAssociation.greedy(new ConfigAssociateGreedy(true,maxError),score);

		AssociateThreeByPairs<TupleDesc_F64> alg = new AssociateThreeByPairs<>(associate,TupleDesc_F64.class);

		alg.setFeaturesA(featuresA, featuresSetA);
		alg.setFeaturesB(featuresB, featuresSetB);
		alg.setFeaturesC(featuresC, featuresSetC);

		alg.associate();

		FastQueue<AssociatedTripleIndex> matches = alg.getMatches();

		assertEquals(0,matches.size);
	}

	/**
	 * A->B is good. B->C is bad.
	 */
	@Test
	void failOnBtoC() {
		FastQueue<TupleDesc_F64> featuresA = UtilFeature.createQueueF64(1);
		FastQueue<TupleDesc_F64> featuresB = UtilFeature.createQueueF64(1);
		FastQueue<TupleDesc_F64> featuresC = UtilFeature.createQueueF64(1);
		GrowQueue_I32 featuresSetA = new GrowQueue_I32();
		GrowQueue_I32 featuresSetB = new GrowQueue_I32();
		GrowQueue_I32 featuresSetC = new GrowQueue_I32();

		featuresB.grow().set(234234234);
		featuresC.grow().set(2344234);
		featuresC.grow().set(99234234);

		for (int i = 0; i < 10; i++) {
			featuresA.grow().set(i);
			featuresB.grow().set(i+0.1);
			featuresC.grow().set(i+0.22);
		}

		// there is only one set
		featuresSetA.resize(featuresA.size);
		featuresSetA.fill(0);
		featuresSetB.resize(featuresB.size);
		featuresSetB.fill(0);
		featuresSetC.resize(featuresC.size);
		featuresSetC.fill(0);


		double maxError = 0.1*0.1+0.00000001;
		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.defaultScore(TupleDesc_F64.class);
		AssociateDescription<TupleDesc_F64> associate = FactoryAssociation.greedy(new ConfigAssociateGreedy(true,maxError),score);

		AssociateThreeByPairs<TupleDesc_F64> alg = new AssociateThreeByPairs<>(associate,TupleDesc_F64.class);

		alg.setFeaturesA(featuresA, featuresSetA);
		alg.setFeaturesB(featuresB, featuresSetB);
		alg.setFeaturesC(featuresC, featuresSetC);

		alg.associate();

		FastQueue<AssociatedTripleIndex> matches = alg.getMatches();

		assertEquals(0,matches.size);
	}

	/**
	 * A->B is bad.
	 */
	@Test
	void failOnAtoB() {
		FastQueue<TupleDesc_F64> featuresA = UtilFeature.createQueueF64(1);
		FastQueue<TupleDesc_F64> featuresB = UtilFeature.createQueueF64(1);
		FastQueue<TupleDesc_F64> featuresC = UtilFeature.createQueueF64(1);
		GrowQueue_I32 featuresSetA = new GrowQueue_I32();
		GrowQueue_I32 featuresSetB = new GrowQueue_I32();
		GrowQueue_I32 featuresSetC = new GrowQueue_I32();

		featuresB.grow().set(234234234);
		featuresC.grow().set(2344234);
		featuresC.grow().set(99234234);

		for (int i = 0; i < 10; i++) {
			featuresA.grow().set(i);
			featuresB.grow().set(i+0.12);
			featuresC.grow().set(i+0.3);
		}

		// there is only one set
		featuresSetA.resize(featuresA.size);
		featuresSetA.fill(0);
		featuresSetB.resize(featuresB.size);
		featuresSetB.fill(0);
		featuresSetC.resize(featuresC.size);
		featuresSetC.fill(0);

		double maxError = 0.1*0.1+0.00000001;
		ScoreAssociation<TupleDesc_F64> score = FactoryAssociation.defaultScore(TupleDesc_F64.class);
		AssociateDescription<TupleDesc_F64> associate = FactoryAssociation.greedy(new ConfigAssociateGreedy(true,maxError),score);

		AssociateThreeByPairs<TupleDesc_F64> alg = new AssociateThreeByPairs<>(associate,TupleDesc_F64.class);

		alg.setFeaturesA(featuresA, featuresSetA);
		alg.setFeaturesB(featuresB, featuresSetB);
		alg.setFeaturesC(featuresC, featuresSetC);

		alg.associate();

		FastQueue<AssociatedTripleIndex> matches = alg.getMatches();

		assertEquals(0,matches.size);
	}
}