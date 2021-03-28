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

package boofcv.alg.similar;

import boofcv.abst.feature.detdesc.DetectDescribePointAbstract;
import boofcv.alg.structure.GenericLookUpSimilarImagesChecks;
import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSimilarImagesSceneRecognition extends GenericLookUpSimilarImagesChecks {

	/**
	 * Populates internal data structures with a scene. Used when checking contract of accessors
	 */
	@Override public <T extends LookUpSimilarImages> T createFullyLoaded() {
		var config = new ConfigSimilarImagesSceneRecognition();

		// Create the algorithm from a factory since it's so much easier
		SimilarImagesSceneRecognition<GrayU8, TupleDesc_F32> alg =
				FactorySceneReconstruction.createSimilarImages(config, ImageType.SB_U8);

		int numViews = 5;
		int numFeaturesPerView = 11;
		int totalFeatures = numViews*numFeaturesPerView;

		alg.imageFeatureStartIndexes.resize(numViews*2);

		for (int viewIdx = 0; viewIdx < numViews; viewIdx++) {
			for (int i = 0; i < numFeaturesPerView; i++) {
				alg.pixels.append(new Point2D_F64(1, 1));
				alg.descriptions.append(alg.detector.createDescription());
			}
			alg.imageFeatureStartIndexes.data[viewIdx*2] = numFeaturesPerView*viewIdx;
			alg.imageFeatureStartIndexes.data[viewIdx*2 + 1] = numFeaturesPerView;

			alg.imageIDs.add(viewIdx + "");
			alg.imageToIndex.put(viewIdx + "", viewIdx);
			alg.imageShapes.grow().setTo(30, 40);
			DogArray_I32 pairIndexes = alg.imageToPairIndexes.grow();

			for (int connView = 0; connView < viewIdx; connView++) {
				int pairIndex = alg.pairedImages.size;
				pairIndexes.add(pairIndex);
				alg.imageToPairIndexes.get(connView).add(pairIndex);

				SimilarImagesSceneRecognition.PairInfo pairs = alg.pairedImages.grow();
				pairs.src = connView;
				pairs.dst = viewIdx;

				for (int featIter = 0; featIter < numFeaturesPerView; featIter++) {
					pairs.associated.grow().setTo(rand.nextInt(totalFeatures), rand.nextInt(totalFeatures));
				}
			}
		}
		return (T)alg;
	}

	/**
	 * Simple scenario which exercises everything all at once
	 */
	@Test void simpleAllTogether() {
		var config = new ConfigSimilarImagesSceneRecognition();
		SimilarImagesSceneRecognition<GrayU8, TupleDesc_F32> alg =
				FactorySceneReconstruction.createSimilarImages(config, ImageType.SB_U8);

		alg.detector = new HelperDetector();

		for (int i = 0; i < 5; i++) {
			alg.addImage("" + i, new GrayU8(50, 10));
		}
		alg.fixate();

		ImageDimension shape = new ImageDimension();
		alg.lookupShape("" + 2, shape);
		assertEquals(50, shape.width);
		assertEquals(10, shape.height);

		// For all these other functions just check to see if something got populated
		DogArray_I32 words = new DogArray_I32();
		alg.lookupImageWords("" + 3, words);
		assertTrue(words.size > 0);

		var features = new DogArray<>(Point2D_F64::new);
		alg.lookupPixelFeats(""+1, features);
		assertTrue(features.size > 0);

		var pairs = new DogArray<>(AssociatedIndex::new);
		alg.lookupMatches(""+0,""+1, pairs);
		assertTrue(features.size > 0);
	}

	@Test void saveImagePairInfo() {
		SimilarImagesSceneRecognition<GrayU8, TupleDesc_F32> alg = createFullyLoaded();
		// clear previously added pair information
		alg.imageToPairIndexes.forEach(DogArray_I32::reset);
		alg.pairedImages.reset();

		// Add a couple of pairs
		alg.saveImagePairInfo(0, 1);
		alg.saveImagePairInfo(1, 2);

		// Check the data structures to see if they were updated correctly
		assertEquals(2, alg.pairedImages.size);
		assertEquals(1, alg.imageToPairIndexes.get(0).size);
		assertEquals(2, alg.imageToPairIndexes.get(1).size);
		assertEquals(1, alg.imageToPairIndexes.get(2).size);

		assertEquals(0, alg.imageToPairIndexes.get(0).get(0));
		assertEquals(1, alg.imageToPairIndexes.get(1).get(1));
		assertEquals(1, alg.imageToPairIndexes.get(2).get(0));

		assertEquals(1, alg.pairedImages.get(1).src);
		assertEquals(2, alg.pairedImages.get(1).dst);
	}

	@Test void lookupImageWords() {
		SimilarImagesSceneRecognition<GrayU8, TupleDesc_F32> alg = createFullyLoaded();

		// Return known words
		alg.recognizer = new MockFeatureSceneRecognition<>() {
			int counter = 1;

			@Override public int lookupWord( TupleDesc_F32 description ) {return counter++;}
		};

		// Look up the words
		var words = new DogArray_I32();
		words.resize(5); // make sure it's reset
		alg.lookupImageWords(1 + "", words);

		assertEquals(11, words.size);
		for (int i = 0; i < words.size; i++) {
			assertEquals(i + 1, words.get(i));
		}
	}

	/**
	 * Simulates image feature detections to run much faster
	 */
	class HelperDetector extends DetectDescribePointAbstract<GrayU8, TupleDesc_F32> {
		int imageCount = 0;

		@Override public TupleDesc_F32 getDescription( int index ) {
			var desc = new TupleDesc_F32(64);
			for (int i = 0; i < desc.size(); i++) {
				desc.data[i] = imageCount + index + i;
			}
			return desc;
		}

		@Override public Point2D_F64 getLocation( int featureIndex ) {
			return new Point2D_F64(rand.nextDouble(), rand.nextGaussian());
		}

		@Override public void detect( GrayU8 input ) {imageCount++;}

		@Override public TupleDesc_F32 createDescription() {return new TupleDesc_F32(64);}

		@Override public int getNumberOfFeatures() {return 10 + imageCount;}
	}
}
