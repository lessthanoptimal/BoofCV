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

import boofcv.abst.feature.associate.AssociateDescriptionAbstract;
import boofcv.abst.feature.associate.AssociateDescriptionHashSets;
import boofcv.abst.feature.detdesc.DetectDescribePointAbstract;
import boofcv.abst.scene.SceneRecognition;
import boofcv.alg.structure.GenericLookUpSimilarImagesChecks;
import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.misc.BoofLambdas;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSimilarImagesSceneRecognition extends GenericLookUpSimilarImagesChecks {

	int numViews = 5;
	int numFeaturesPerView = 11;

	/**
	 * Populates internal data structures with a scene. Used when checking contract of accessors
	 */
	@Override public <T extends LookUpSimilarImages> T createFullyLoaded() {
		var config = new ConfigSimilarImagesSceneRecognition();
		// Needed to do this because the default assumes a certain number of image features
		config.minimumSimilar.setRelative(0.1, 0.0);

		// Create the algorithm from a factory since it's so much easier
		SimilarImagesSceneRecognition<GrayU8, TupleDesc_F32> alg =
				FactorySceneReconstruction.createSimilarImages(config, ImageType.SB_U8);

		alg.recognizer = new HelperRecognizer(numViews);
		alg.asscociator = new AssociateDescriptionHashSets<>(new HelperAssociate(numFeaturesPerView));

		alg.imageFeatureStartIndexes.resize(numViews*2);

		for (int viewIdx = 0; viewIdx < numViews; viewIdx++) {
			for (int i = 0; i < numFeaturesPerView; i++) {
				alg.pixels.append(new Point2D_F64(1, 1));
				// encode the view into the descriptor so that the recognition helper knows which images to return
				TupleDesc_F32 desc = alg.detector.createDescription();
				desc.data[0] = viewIdx;
				alg.descriptions.append(desc);
			}
			alg.imageFeatureStartIndexes.data[viewIdx*2] = numFeaturesPerView*viewIdx;
			alg.imageFeatureStartIndexes.data[viewIdx*2 + 1] = numFeaturesPerView;

			alg.imageIDs.add(viewIdx + "");
			alg.imageToIndex.put(viewIdx + "", viewIdx);
		}
		return (T)alg;
	}

	/**
	 * Simple scenario which exercises everything all at once
	 */
	@Test void simpleAllTogether() {
		var config = new ConfigSimilarImagesSceneRecognition();
		// Needed to do some hacking since the defaults assume there are a bunch of image features
		config.minimumSimilar.setRelative(0.1, 0.0);
		config.recognizeNister2006.minimumDepthFromRoot = 0;

		SimilarImagesSceneRecognition<GrayU8, TupleDesc_F32> alg =
				FactorySceneReconstruction.createSimilarImages(config, ImageType.SB_U8);

		alg.detector = new HelperDetector();

		for (int i = 0; i < 5; i++) {
			alg.addImage("" + i, new GrayU8(50, 10));
		}
		alg.fixate();

		// For all these other functions just check to see if something got populated
		DogArray_I32 words = new DogArray_I32();
		alg.lookupImageWords("3", words);
		assertTrue(words.size > 0);

		var features = new DogArray<>(Point2D_F64::new);
		alg.lookupPixelFeats("1", features);
		assertTrue(features.size > 0);

		// Look up similar images. All but the query view should be similar
		List<String> similarImages = new ArrayList<>();
		alg.findSimilar("0", ( a ) -> true, similarImages);
		assertTrue(similarImages.size() > 0);

		var pairs = new DogArray<>(AssociatedIndex::new);
		alg.lookupAssociated(similarImages.get(0), pairs);
		assertTrue(features.size > 0);
	}

	@Test void lookupImageWords() {
		SimilarImagesSceneRecognition<GrayU8, TupleDesc_F32> alg = createFullyLoaded();

		// Return known words
		alg.recognizer = new FeatureSceneRecognitionAbstract<>() {
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

	/**
	 * All images are matched to every other image, including itself.
	 */
	static class HelperRecognizer extends FeatureSceneRecognitionAbstract<TupleDesc_F32> {
		int numViews;

		public HelperRecognizer( int numViews ) {
			this.numViews = numViews;
		}

		@Override
		public boolean query( Features<TupleDesc_F32> query,
							  @Nullable BoofLambdas.Filter<String> filter,
							  int limit, DogArray<SceneRecognition.Match> matches ) {
			matches.reset();

			for (int i = 0; i < Math.min(limit, numViews); i++) {
				String id = "" + i;
				if (filter != null && !filter.keep(id))
					continue;
				matches.grow().id = id;
			}

			return true;
		}

		@Override public int getTotalWords() {
			return 1;
		}
	}

	/**
	 * Matches every feature up with every feature
	 */
	static class HelperAssociate extends AssociateDescriptionAbstract<TupleDesc_F32> {
		int numFeatures;

		public HelperAssociate( int numFeatures ) {
			this.numFeatures = numFeatures;
		}

		@Override public FastAccess<AssociatedIndex> getMatches() {
			var matches = new DogArray<>(AssociatedIndex::new);
			for (int i = 0; i < numFeatures; i++) {
				matches.grow().setTo(i, i);
			}
			return matches;
		}

		@Override public Class<TupleDesc_F32> getDescriptionType() {
			return TupleDesc_F32.class;
		}

		@Override public DogArray_I32 getUnassociatedSource() {
			return new DogArray_I32();
		}

		@Override public DogArray_I32 getUnassociatedDestination() {
			return new DogArray_I32();
		}
	}
}
