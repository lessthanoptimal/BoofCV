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

package boofcv.alg.scene;

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestClassifierKNearestNeighborsBow extends BoofStandardJUnit {

	public final static int NUM_WORDS = 3;
	public final static int FEATURES_IN_IMAGE = 46;

	@Test void basicTest() {
		DummyNN nn = new DummyNN();
		DummyDense features = new DummyDense();
		DummyToWord toWords = new DummyToWord();

		List<HistogramScene> memory = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			memory.add(new HistogramScene(NUM_WORDS));
		}

		ClassifierKNearestNeighborsBow bow = new ClassifierKNearestNeighborsBow(nn,features,toWords);
		bow.setNumNeighbors(6);
		bow.setClassificationData(memory, 3);

		assertEquals(2, bow.classify(new GrayU8(2, 3)));

		assertTrue(nn.setPoints);
		assertEquals(1, toWords.numReset);
		assertEquals(FEATURES_IN_IMAGE, toWords.numAddFeature);
		assertEquals(1, toWords.numProcess);


		// call one more time and see if stuff blows up
		assertEquals(2, bow.classify(new GrayU8(2, 3)));

		assertTrue(nn.setPoints);
		assertEquals(2, toWords.numReset);
		assertEquals(FEATURES_IN_IMAGE*2, toWords.numAddFeature);
		assertEquals(2, toWords.numProcess);
	}


	protected class DummyNN implements NearestNeighbor<HistogramScene> {

		public boolean setPoints = false;

		@Override
		public void setPoints(List<HistogramScene> points, boolean index) {
			setPoints = true;
		}

		@Override
		public Search<HistogramScene> createSearch() {
			return new InternalSearch();
		}

		public class InternalSearch implements NearestNeighbor.Search<HistogramScene> {

			@Override
			public boolean findNearest(HistogramScene point, double maxDistance, NnData<HistogramScene> result) {
				throw new RuntimeException("Wasn't expecting this to be called!");
			}

			@Override
			public void findNearest(HistogramScene point, double maxDistance, int numNeighbors,
									DogArray<NnData<HistogramScene>> result) {
				assertTrue(result.size() == 0);

				for (int i = 0; i < numNeighbors; i++) {
					NnData<HistogramScene> d = result.grow();
					d.point = new HistogramScene(NUM_WORDS);
					d.point.type = 2;
				}
			}
		}
	}

	protected class DummyDense implements DescribeImageDense {

		List<TupleDesc> descriptions = new ArrayList<>();

		@Override
		public void process(ImageBase input) {
			descriptions.clear();
			for (int i = 0; i < FEATURES_IN_IMAGE; i++) {
				descriptions.add(createDescription());
			}
		}

		@Override
		public List getDescriptions() {
			return descriptions;
		}

		@Override
		public List<Point2D_I32> getLocations() {
			return null;
		}

		@Override
		public ImageType getImageType() {
			return null;
		}

		@Override
		public TupleDesc createDescription() {
			return new TupleDesc_F64(5);
		}

		@Override
		public Class getDescriptionType() {
			return TupleDesc_F64.class;
		}
	}

	protected class DummyToWord implements FeatureToWordHistogram {

		int numReset = 0;
		int numAddFeature = 0;
		int numProcess = 0;

		@Override
		public void reset() {
			numReset++;
		}

		@Override
		public void addFeature(TupleDesc feature) {
			numAddFeature++;
		}

		@Override
		public void process() {
			numProcess++;
		}

		@Override
		public double[] getHistogram() {
			return new double[]{0.5,0.3,0.2};
		}

		@Override
		public int getTotalWords() {
			return NUM_WORDS;
		}
	}
}
