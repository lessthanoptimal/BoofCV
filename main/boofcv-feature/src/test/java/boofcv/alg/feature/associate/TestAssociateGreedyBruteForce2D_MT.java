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
import boofcv.struct.ConfigLength;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestAssociateGreedyBruteForce2D_MT extends BoofStandardJUnit {

	final static int width = 100;
	final static int height = 120;

	@Test void compare() {
		compare(false, 1.0);
		compare(true, 1.0);
		compare(false, 0.1);
		compare(true, 0.1);
	}

	void compare( boolean backwards, double ratioTest ) {
		DogArray<TupleDesc_F64> descSrc = createData(200);
		DogArray<TupleDesc_F64> descDst = createData(200);
		DogArray<Point2D_F64> pointsSrc = createPoints(200);
		DogArray<Point2D_F64> pointsDst = createPoints(200);


		var distance = new AssociateImageDistanceEuclideanSq();

		var sequentialAlg = new AssociateGreedyBruteForce2D<>(new ScoreAssociateEuclidean_F64(), distance);
		sequentialAlg.init(width, height);
		sequentialAlg.maxDistanceLength.setTo(ConfigLength.relative(0.25, 6));
		sequentialAlg.backwardsValidation = backwards;
		sequentialAlg.setRatioTest(ratioTest);
		sequentialAlg.setMaxFitError(0.5);
		sequentialAlg.setSource(pointsSrc, descSrc);
		sequentialAlg.setDestination(pointsDst, descDst);
		sequentialAlg.associate();

		var parallelAlg = new AssociateGreedyBruteForce2D_MT<>(new ScoreAssociateEuclidean_F64(), distance);
		parallelAlg.init(width, height);
		parallelAlg.maxDistanceLength.setTo(ConfigLength.relative(0.25, 6));
		parallelAlg.backwardsValidation = backwards;
		parallelAlg.setRatioTest(ratioTest);
		parallelAlg.setMaxFitError(0.5);
		parallelAlg.setSource(pointsSrc, descSrc);
		parallelAlg.setDestination(pointsDst, descDst);
		parallelAlg.associate();

		int[] pairs0 = sequentialAlg.getPairs().data;
		int[] pairs1 = parallelAlg.getPairs().data;
		double[] quality0 = sequentialAlg.getFitQuality().data;
		double[] quality1 = parallelAlg.getFitQuality().data;

		assertEquals(pairs0.length, pairs1.length);

		for (int i = 0; i < pairs0.length; i++) {
			assertEquals(pairs0[i], pairs1[i]);
			assertEquals(quality0[i], quality1[i]);
		}
	}

	public static DogArray<TupleDesc_F64> createData( int count ) {
		Random rand = new Random(234);
		DogArray<TupleDesc_F64> ret = new DogArray<>(count, () -> new TupleDesc_F64(1));
		for (int i = 0; i < count; i++) {
			ret.grow().setTo(rand.nextDouble()*10);
		}
		return ret;
	}

	public static DogArray<Point2D_F64> createPoints( int count ) {
		Random rand = new Random(234);
		DogArray<Point2D_F64> ret = new DogArray<>(count, Point2D_F64::new);
		for (int i = 0; i < count; i++) {
			ret.grow().setTo(rand.nextDouble()*width, rand.nextDouble()*height);
		}
		return ret;
	}
}