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

package boofcv.alg.fiducial.dots;

import boofcv.abst.distort.FDistort;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.feature.describe.llah.LlahDocument;
import boofcv.alg.feature.describe.llah.LlahHasher;
import boofcv.alg.feature.describe.llah.LlahOperations;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetectorPixel;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.FactoryMultiViewRobust;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.fitting.modelset.ransac.Ransac;
import org.ddogleg.struct.FastQueue;
import org.ejml.UtilEjml;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.fixed.MatrixFeatures_DDF3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestUchiyaMarkerTracker {
	Random rand = BoofTesting.createRandom(0);
	int width = 100;
	int height = 90;

	List<List<Point2D_F64>> documents = new ArrayList<>();

	public TestUchiyaMarkerTracker() {
		for (int i = 0; i < 20; i++) {
			documents.add( UchiyaMarkerGeneratorImage.createRandomMarker(rand,20,90,15));
		}
	}

	/**
	 * Generate an image with a single target. See if it finds that target in the image
	 */
	@Test
	void singleFrame_Easy() {
		int targetID = 2;
		List<Point2D_F64> dots = documents.get(targetID);

		UchiyaMarkerGeneratorImage generator = new UchiyaMarkerGeneratorImage();
		generator.configure(width,height,5);
		generator.setRadius(4);
		generator.render(dots);

		UchiyaMarkerTracker<GrayU8> tracker = createTracker();
		for( var doc : documents ) {
			tracker.llahOps.createDocument(doc);
		}

//		ShowImages.showWindow(generator.getImage(),"Stuff");
//		BoofMiscOps.sleep(10000);

		tracker.process(generator.getImage());

		FastQueue<UchiyaMarkerTracker.Track> tracks = tracker.getCurrentTracks();
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

		UchiyaMarkerGeneratorImage generator = new UchiyaMarkerGeneratorImage();
		generator.configure(width,height,5);
		generator.setRadius(4);
		generator.render(dots);

		var image = new GrayU8(width*2, height*2);

		UchiyaMarkerTracker<GrayU8> tracker = createTracker();
		for( var doc : documents ) {
			tracker.llahOps.createDocument(doc);
		}

		var prevMean = new Point2D_F64();
		var currMean = new Point2D_F64();

		for (int frame = 0; frame < 10; frame++) {
			new FDistort(generator.getImage(), image).affine(1,0,0,1,frame*5,frame).border(255).apply();

//			ShowImages.showWindow(image,"Stuff");
//			BoofMiscOps.sleep(1000);

			tracker.process(image);

			FastQueue<UchiyaMarkerTracker.Track> tracks = tracker.getCurrentTracks();
			assertEquals(1,tracks.size);
			UchiyaMarkerTracker.Track t = tracks.get(0);
			assertEquals(targetID, t.globalDoc.documentID);

			// Make sure the track is moving in the expected way
			UtilPoint2D_F64.mean(t.predicted.toList(),currMean);
			if( frame > 0 ) {
				assertEquals(5.0,currMean.x-prevMean.x, 0.5);
			}
			prevMean.set(currMean);
		}
	}

	@Test
	void performDetection() {
		UchiyaMarkerTracker<GrayU8> tracker = createTracker();
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

		var document = new LlahDocument();
		document.landmarks.copyAll(landmarks,(src,dst)->dst.set(src));

		var observed = new LlahOperations.FoundDocument();
		observed.init(document);


		for (int i = 0; i < landmarks.size(); i++) {
			LlahOperations.DotCount count = new LlahOperations.DotCount();
			count.counts = 1000;
			count.dotIdx = i;
			observed.landmarkToDots.get(i).put(i,count);
		}

		UchiyaMarkerTracker<GrayU8> alg = createTracker();

		assertTrue(alg.fitHomography(dots,observed));
		Homography2D_F64 found = alg.ransac.getModelParameters();

		CommonOps_DDF3.divide(found,found.a33);

		assertTrue(MatrixFeatures_DDF3.isIdentical(doc_to_image, found, UtilEjml.TEST_F64));
	}

	UchiyaMarkerTracker<GrayU8> createTracker() {
		InputToBinary<GrayU8> thresholder = FactoryThresholdBinary.globalOtsu(0,255,1.0,true,GrayU8.class);
		var ellipseDetector = new BinaryEllipseDetectorPixel();
		var ops = new LlahOperations(7,5,new LlahHasher.Affine(100,500000));
		Ransac<Homography2D_F64, AssociatedPair> ransac =
				FactoryMultiViewRobust.homographyRansac(null,new ConfigRansac(100,2.0));
		return new UchiyaMarkerTracker<>(thresholder,ellipseDetector,ops,ransac);
	}
}