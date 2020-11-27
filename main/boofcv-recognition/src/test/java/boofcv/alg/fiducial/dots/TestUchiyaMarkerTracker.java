/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.dots;

import boofcv.alg.feature.describe.llah.LlahDocument;
import boofcv.alg.feature.describe.llah.LlahHasher;
import boofcv.alg.feature.describe.llah.LlahOperations;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.affine.AffinePointOps_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.fixed.MatrixFeatures_DDF3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestUchiyaMarkerTracker extends BoofStandardJUnit {
	List<List<Point2D_F64>> documents = new ArrayList<>();

	public TestUchiyaMarkerTracker() {
		for (int i = 0; i < 20; i++) {
			documents.add( RandomDotMarkerGeneratorImage.createRandomMarker(rand,20,120,120,5));
		}
	}

	/**
	 * Generate an image with a single target. See if it finds that target in the image
	 */
	@Test
	void singleFrame_Easy() {
		int targetID = 2;
		List<Point2D_F64> dots = documents.get(targetID);

		UchiyaMarkerTracker tracker = createTracker();
		for( var doc : documents ) {
			tracker.llahOps.createDocument(doc);
		}

		tracker.process(dots);

		DogArray<UchiyaMarkerTracker.Track> tracks = tracker.getCurrentTracks();
		assertEquals(1,tracks.size);
		UchiyaMarkerTracker.Track t = tracks.get(0);
		assertEquals(targetID, t.globalDoc.documentID);
	}

	/**
	 * Give it an image sequence and see if it can track the target. Only translation
	 */
	@Test
	void sequence_Easy() {
		int targetID = 2;
		List<Point2D_F64> dots = documents.get(targetID);

		UchiyaMarkerTracker tracker = createTracker();
		for( var doc : documents ) {
			tracker.llahOps.createDocument(doc);
		}

		var prevMean = new Point2D_F64();
		var currMean = new Point2D_F64();

		for (int frame = 0; frame < 10; frame++) {
			List<Point2D_F64> copied = new ArrayList<>();
			var affine = new Affine2D_F64(1,0,0,1,frame*5,frame);
			for (int i = 0; i < dots.size(); i++) {
				Point2D_F64 c = new Point2D_F64();
				AffinePointOps_F64.transform(affine,dots.get(i),c);
				copied.add(c);
			}
			tracker.process(copied);

			DogArray<UchiyaMarkerTracker.Track> tracks = tracker.getCurrentTracks();
			assertEquals(1,tracks.size);
			UchiyaMarkerTracker.Track t = tracks.get(0);
			assertEquals(targetID, t.globalDoc.documentID);

			// Make sure the track is moving in the expected way
			UtilPoint2D_F64.mean(t.predicted.toList(),currMean);
			if( frame > 0 ) {
				assertEquals(5.0,currMean.x-prevMean.x, 0.5);
			}
			prevMean.setTo(currMean);
		}
	}

	@Test
	void performDetection() {
		UchiyaMarkerTracker tracker = createTracker();
		for( var doc : documents ) {
			tracker.llahOps.createDocument(doc);
		}

		// Add all the definitions into a single set of detections by adding a huge offset between them
		List<Point2D_F64> observations = new ArrayList<>();

		// All but the last two. Just a sanity check
		for (int i = 0; i < documents.size()-2; i++) {
			double offset = i*500;
			for( var p : documents.get(i) ) {
				observations.add( new Point2D_F64(p.x+offset,p.y));
			}
		}

		tracker.performDetection(observations);

		assertEquals(documents.size()-2, tracker.currentTracks.size);

		// check to see if the predicted landmark locations are correct
		for( var track : tracker.currentTracks.toList() ) {
			List<Point2D_F64> docObs = documents.get(track.globalDoc.documentID);
			double offset = track.globalDoc.documentID*500;

			for (int i = 0; i < track.predicted.size; i++) {
				double expectedX = docObs.get(i).x + offset;
				double expectedY = docObs.get(i).y;

				assertEquals(0,track.predicted.get(i).distance(expectedX, expectedY), 0.01);
			}
		}
	}

	@Test
	void fitHomography() {
		var doc_to_image = new Homography2D_F64(2,0,10,0,1.5,-5.6,0,0,1);

		List<Point2D_F64> landmarks = UtilPoint2D_F64.random(-1,1,10,rand);
		List<Point2D_F64> dots = new ArrayList<>();
		for( var m : landmarks ) {
			var d = new Point2D_F64();
			HomographyPointOps_F64.transform(doc_to_image,m,d);
			dots.add(d);
		}
		// Shuffle the order to test to see if the correct book keeping is being done
		List<Point2D_F64> dotsShuffled = new ArrayList<>(dots);
		Collections.shuffle(dotsShuffled, rand);

		var document = new LlahDocument();
		document.landmarks.copyAll(landmarks,(src,dst)->dst.setTo(src));

		var observed = new LlahOperations.FoundDocument();
		observed.init(document);


		for (int i = 0; i < landmarks.size(); i++) {
			observed.landmarkHits.data[i] = 1000;
			observed.landmarkToDots.data[i] = dotsShuffled.indexOf(dots.get(i));
		}

		UchiyaMarkerTracker alg = createTracker();

		assertTrue(alg.fitHomography(dotsShuffled,observed));
		Homography2D_F64 found = alg.ransac.getModelParameters();

		CommonOps_DDF3.divide(found,found.a33);

		assertTrue(MatrixFeatures_DDF3.isIdentical(doc_to_image, found, UtilEjml.TEST_F64));
	}

	public static UchiyaMarkerTracker createTracker() {
		var ops = new LlahOperations(7,5,new LlahHasher.Affine(100,500000));
		Ransac<Homography2D_F64, AssociatedPair> ransac =
				FactoryMultiViewRobust.homographyRansac(null,new ConfigRansac(100,1.0));
		return new UchiyaMarkerTracker(ops,ransac);
	}
}
