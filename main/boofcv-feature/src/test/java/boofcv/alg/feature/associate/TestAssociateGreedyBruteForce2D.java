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

import boofcv.abst.feature.associate.ScoreAssociateEuclidean_F64;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.struct.ConfigLength;
import boofcv.struct.feature.TupleDesc_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestAssociateGreedyBruteForce2D extends GenericAssociateGreedyChecks {

	ScoreAssociation<TupleDesc_F64> score = new ScoreAssociateEuclidean_F64();

	@Override
	protected AssociateGreedyBase<TupleDesc_F64> createAlgorithm() {
		var alg = new AssociateGreedyBruteForce2D<>(score, new AssociateImageDistanceEuclideanSq());
		// it should now be equivalent
		alg.maxDistanceLength.setTo(ConfigLength.fixed(Double.MAX_VALUE));
		alg.init(100, 100);
		return alg;
	}

	@Override
	protected void associate( AssociateGreedyBase<TupleDesc_F64> _alg,
							  FastAccess<TupleDesc_F64> src,
							  FastAccess<TupleDesc_F64> dst ) {
		var alg = (AssociateGreedyBruteForce2D<TupleDesc_F64>)_alg;

		// Dummy Values
		var locSrc = new DogArray<>(Point2D_F64::new);
		var locDst = new DogArray<>(Point2D_F64::new);

		for (int i = 0; i < src.size; i++) {locSrc.grow();}
		for (int i = 0; i < dst.size; i++) {locDst.grow();}

		alg.setSource(locSrc, src);
		alg.setDestination(locDst, dst);
		alg.associate();
	}

	@Test void isMaxDistanceRespected() {
		var descSrc = createData(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		var descDst = createData(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);

		var locSrc = new DogArray<>(Point2D_F64::new);
		var locDst = new DogArray<>(Point2D_F64::new);

		locSrc.resize(descSrc.size);
		locDst.resize(descDst.size);

		double d = 10.0;
		for (int i = 0; i < 4; i++) {
			locDst.get(i).setTo(d, 0);
		}

		var alg = new AssociateGreedyBruteForce2D<>(score, new AssociateImageDistanceEuclideanSq());
		alg.setMaxFitError(0.1); // limit what it can be matched to to make testing easier
		alg.setSource(locSrc, descSrc);
		alg.setDestination(locDst, descDst);

		// very clear separation
		alg.maxDistanceUnits = d*d/2;
		alg.associate();
		assertEquals(6, countMatches(alg.getPairs()));

		// everything should be matched
		alg.maxDistanceUnits = d*d*2;
		alg.associate();
		assertEquals(10, countMatches(alg.getPairs()));

		// test that threshold is inclusive
		alg.maxDistanceUnits = d*d;
		alg.associate();
		assertEquals(10, countMatches(alg.getPairs()));
	}

	private int countMatches( DogArray_I32 pairs ) {
		int total = 0;
		for (int i = 0; i < pairs.size; i++) {
			if (pairs.data[i] >= 0)
				total++;
		}
		return total;
	}
}
