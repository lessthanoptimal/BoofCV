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

import boofcv.abst.feature.associate.AssociateDescriptionHashSets;
import boofcv.abst.feature.associate.ScoreAssociateEuclideanSq;
import boofcv.abst.feature.describe.DescribePointAbstract;
import boofcv.abst.scene.SceneRecognition;
import boofcv.abst.tracker.PointTrack;
import boofcv.alg.structure.GenericLookUpSimilarImagesChecks;
import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.structure.FactorySceneReconstruction;
import boofcv.misc.BoofLambdas;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.PackedTupleArray_F32;
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSimilarImagesTrackThenMatch extends GenericLookUpSimilarImagesChecks {
	@Override public <T extends LookUpSimilarImages> T createFullyLoaded() {
		int numFeatures = 11;
		SimilarImagesTrackThenMatch<GrayU8, TupleDesc_F32> alg =
				FactorySceneReconstruction.createTrackThenMatch(null, ImageType.SB_U8);

		alg.initialize(200, 210);

		// We don't want it to actually recognize anything when testing
		alg.recognizer = new DummyRecognizer();

		alg.frames.resize(5);
		for (int i = 0; i < alg.frames.size; i++) {
			SimilarImagesFromTracks.Frame f = alg.frames.get(i);
			f.frameID = "" + i;
			alg.frameMap.put(f.frameID, f);
			f.initActive(numFeatures);
			for (int j = 0; j < i; j++) {
				SimilarImagesFromTracks.Match m = alg.connections.grow();
				m.init(numFeatures);
				SimilarImagesFromTracks.Frame r = alg.frames.get(j);
				f.related.add(r);
				r.related.add(f);
				f.matches.add(m);
				r.matches.add(m);
				m.frameSrc = f;
				m.frameDst = r;
				for (int k = 0; k < m.size(); k++) {
					// Randomize the values make it obvious if the feature src/dst order is respected
					m.src[k] = rand.nextInt();
					m.dst[k] = rand.nextInt();
				}
			}

			alg.frameStartIndexes.add(i*numFeatures);
		}

		// Create descriptions for all the frames to stop exceptions from being thrown
		int totalFeatures = alg.frames.size*numFeatures;
		alg.frameStartIndexes.add(totalFeatures);
		alg.descriptions.reserve(totalFeatures);
		for (int i = 0; i < totalFeatures; i++) {
			alg.descriptions.append(alg.nullDescription);
		}

		return (T)alg;
	}

	/**
	 * Faking the inputs, see if it can attack all frames to the first frame when tracking fails
	 */
	@Test void simpleLoop() {
		int numViews = 6;

		var alg = new SimilarImagesTrackThenMatch<>(
				new DummyDetector(),
				new AssociateDescriptionHashSets<>(FactoryAssociation.greedy(null, new ScoreAssociateEuclideanSq.F32())),
				new DummyRecognizerLoop(numViews),
				() -> new PackedTupleArray_F32(1));

		// Remove any restriction on how many frames need to have past
		alg.minimumRecognizeDistance = 0;

		alg.initialize(10, 20);

		// A dummy image
		GrayU8 image = new GrayU8(10, 20);

		// Create a set of tracks
		int numTracks = 20;
		DogArray<PointTrack> tracks = new DogArray<>(PointTrack::new);
		for (int i = 0; i < numTracks; i++) {
			PointTrack t = tracks.grow();
			t.pixel.setTo(i, 21*(i + 1));
			t.featureId = i;
		}

		// Process a set of fake frames and "track" features across them. This has been designed so that
		// the frame-to-frame tracker will fail but the recognizer will match everything to frame 0
		for (int frameID = 0; frameID < numViews; frameID++) {
			alg.processFrame(image, tracks.toList(), frameID);

			for (int i = 0; i < tracks.size; i++) {
				PointTrack t = tracks.get(i);
				t.pixel.setTo((frameID + 1)*numTracks, 21*(i + 1));
				t.featureId = i + (frameID + 1)*numTracks; // this will prevent the tracker from matching frames
			}
		}

		// Has to be called after frame to frame tracking
		alg.finishedTracking();

		// Storage for association results
		DogArray<AssociatedIndex> pairs = new DogArray<>(AssociatedIndex::new);
		List<String> listImages = alg.getImageIDs();
		List<String> listSimilar = new ArrayList<>();

		// everything should be matched to the first frame
		alg.findSimilar(listImages.get(0), null, listSimilar);
		assertEquals(numViews-1, listSimilar.size());
		for (String similarID : listSimilar) {
			assertTrue(alg.lookupAssociated(similarID, pairs));
			assertEquals(numTracks, pairs.size);
		}

		// Other frames shouldn't be matched with each other
		for (int frameIdx = 1; frameIdx < 6; frameIdx++) {
			alg.findSimilar(listImages.get(frameIdx), null, listSimilar);
			assertEquals(1, listSimilar.size());
		}
	}

	/**
	 * Describes features based on their location
	 */
	static class DummyDetector extends DescribePointAbstract<GrayU8, TupleDesc_F32> {
		public DummyDetector() {
			super(() -> new TupleDesc_F32(1));
		}

		@Override public boolean process( double x, double y, TupleDesc_F32 description ) {
			// each feature has a unique y that's the same across frames. Thus a real association algorithm should
			// work given this descriptor
			description.data[0] = (float)y;
			return true;
		}
	}

	/**
	 * A super hacked recognizer. Figures out the frame from the pixel coordinate and does nothing else of value.
	 */
	static class DummyRecognizer extends FeatureSceneRecognitionAbstract<TupleDesc_F32> {
		@Override public int getTotalWords() {
			return 2;
		}

		@Override
		public boolean query( Features<TupleDesc_F32> query, BoofLambdas.Filter<String> filter,
							  int limit, DogArray<SceneRecognition.Match> matches ) {
			matches.reset();

			// The frame is encoded in the x-coordinate
			int frameID = (int)(query.getPixel(0).x/20);

			if (frameID == 0)
				return false;

			// Everything can see the first frame
			matches.grow().id = "0";
			return true;
		}
	}

	static class DummyRecognizerLoop extends FeatureSceneRecognitionAbstract<TupleDesc_F32> {
		int numViews;

		public DummyRecognizerLoop( int numViews ) {
			this.numViews = numViews;
		}

		@Override public int getTotalWords() {
			return 1;
		}

		@Override
		public boolean query( Features<TupleDesc_F32> query, BoofLambdas.Filter<String> filter,
							  int limit, DogArray<SceneRecognition.Match> matches ) {
			matches.reset();

			// The frame is encoded in the x-coordinate
			int frameID = (int)query.getPixel(0).x;

			if (frameID == 0) {
				for (int i = 1; i < numViews; i++) {
					matches.grow().id = ""+i;
				}
			} else {
				matches.grow().id = "0";
			}

			return true;
		}
	}
}
