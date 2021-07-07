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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxLimiter;
import boofcv.alg.feature.detect.interest.SiftDetector.SiftPoint;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.feature.detect.selector.FactorySelectLimit;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestSiftDetector extends BoofStandardJUnit {
	SiftScaleSpace ss = new SiftScaleSpace(-1, 5, 3, 1.6);

	/**
	 * Tests the ability to detect a single square feature at multiple scales and color
	 */
	@Test void process() {
		int c_x = 40, c_y = 42;

		SiftDetector alg = createDetector();
		for (int radius : new int[]{2, 5}) {
			int width = radius*2 + 1;
			for (boolean white : new boolean[]{true, false}) {
				GrayF32 input = new GrayF32(80, 70);
				if (white) {
					GImageMiscOps.fillRectangle(input, 200, c_x - radius, c_y - radius, width, width);
				} else {
					GImageMiscOps.fill(input, 200);
					GImageMiscOps.fillRectangle(input, 0, c_x - radius, c_y - radius, width, width);
				}

				ss.process(input);
				alg.process(ss);

				List<SiftPoint> detections = alg.getDetections();
				assertTrue(detections.size() > 0);

				boolean found = false;
				for (int i = 0; i < detections.size(); i++) {
					ScalePoint p = detections.get(i);
					if (p.pixel.distance(c_x, c_y) <= 0.2) {
						assertEquals(radius*1.25, p.scale, 0.5);
						assertEquals(p.white, white);
						found = true;
					}
				}
				assertTrue(found);
			}
		}
	}

	@Test void isScaleSpaceExtremum() {
		GrayF32 upper = new GrayF32(30, 40);
		GrayF32 current = new GrayF32(30, 40);
		GrayF32 lower = new GrayF32(30, 40);

		SiftDetector alg = createDetector();
		alg.dogLower = lower;
		alg.dogTarget = current;
		alg.dogUpper = upper;

		for (float sign : new float[]{-1, 1}) {
			assertTrue(alg.isScaleSpaceExtremum(15, 16, 100, sign));

			upper.set(15, 16, sign*100);
			assertFalse(alg.isScaleSpaceExtremum(15, 16, 100, sign));
			upper.set(15, 16, 0);
			lower.set(15, 16, sign*100);
			assertFalse(alg.isScaleSpaceExtremum(15, 16, 100, sign));
		}
	}

	/**
	 * Interpolation shouldn't change the location since its equal on all sides
	 */
	@Test void processFeatureCandidate_NoChange() {
		GrayF32 upper = new GrayF32(30, 40);
		GrayF32 current = new GrayF32(30, 40);
		GrayF32 lower = new GrayF32(30, 40);

		SiftDetector alg = createDetector();
		alg.pixelScaleToInput = 2.0;
		alg.sigmaLower = 4;
		alg.sigmaTarget = 5;
		alg.sigmaUpper = 6;
		alg.dogLower = lower;
		alg.dogTarget = current;
		alg.dogUpper = upper;
		alg.derivXX.setImage(current);
		alg.derivXY.setImage(current);
		alg.derivYY.setImage(current);

		for (float sign : new float[]{-1, 1}) {
			alg.detectionsAll.reset();
			current.set(15, 16, sign*100);
			alg.createDetection(15, 16, 100, sign > 0);

			ScalePoint p = alg.getDetections().get(0);
			assertEquals(15*2, p.pixel.x, 1e-8);
			assertEquals(16*2, p.pixel.y, 1e-8);
			assertEquals(5, p.scale, 1e-8);
		}
	}

	/**
	 * The feature intensity is no longer symmetric. See if the interpolated peak moves in the expected direction
	 * away from the pixel level peak.
	 */
	@Test void processFeatureCandidate_Shift() {
		GrayF32 upper = new GrayF32(30, 40);
		GrayF32 current = new GrayF32(30, 40);
		GrayF32 lower = new GrayF32(30, 40);

		SiftDetector alg = createDetector();
		alg.pixelScaleToInput = 2.0;
		alg.sigmaLower = 4;
		alg.sigmaTarget = 5;
		alg.sigmaUpper = 6;
		alg.dogLower = lower;
		alg.dogTarget = current;
		alg.dogUpper = upper;
		alg.derivXX.setImage(current);
		alg.derivXY.setImage(current);
		alg.derivYY.setImage(current);

		int x = 15, y = 16;
		for (float sign : new float[]{-1, 1}) {
			alg.detectionsAll.reset();
			current.set(x, y - 1, sign*90);
			current.set(x, y, sign*100);
			current.set(x, y + 1, sign*80);
			current.set(x - 1, y, sign*90);
			current.set(x + 1, y, sign*80);
			upper.set(x, y, sign*80);
			lower.set(x, y, sign*90);

			alg.createDetection(15, 16, 100, sign > 0);

			ScalePoint p = alg.getDetections().get(0);
			// make sure it is close
			assertTrue(Math.abs(x*2 - p.pixel.x) < 2);
			assertTrue(Math.abs(y*2 - p.pixel.y) < 2);
			assertTrue(Math.abs(5 - p.scale) < 2);

			// see if its shifted in the correct direction
			assertTrue(x*2 > p.pixel.x);
			assertTrue(y*2 > p.pixel.y);
			assertTrue(5 > p.scale);

			// do a test just for scale since the code branches depending on the sign
			upper.set(x, y, sign*90);
			lower.set(x, y, sign*80);

			alg.detectionsAll.reset();
			alg.createDetection(15, 16, 100, sign > 0);
			assertTrue(Math.abs(5 - p.scale) < 2);
			assertTrue(5 < p.scale);
		}
	}

	@Test void isEdge() {
		// create an edge
		GrayF32 input = new GrayF32(100, 120);
		GImageMiscOps.fillRectangle(input, 100, 0, 0, 50, 120);

		SiftDetector alg = createDetector();

		alg.derivXX.setImage(input);
		alg.derivXY.setImage(input);
		alg.derivYY.setImage(input);

		assertTrue(alg.isEdge(50, 50));

		// now have it detect something that isn't an edge
		GImageMiscOps.fill(input, 0);
		input.set(50, 50, 100);
		assertFalse(alg.isEdge(50, 50));
	}

	private SiftDetector createDetector() {

		NonMaxLimiter nonmax = FactoryFeatureExtractor.nonmaxLimiter(
				new ConfigExtract(1, 0, 1, true, true, true),
				ConfigSelectLimit.selectBestN(), 1000);
		FeatureSelectLimitIntensity<ScalePoint> selectorAll = FactorySelectLimit.intensity(ConfigSelectLimit.selectBestN());
		return new SiftDetector(selectorAll, 10, nonmax);
	}

	/**
	 * Sees if fewer points are selected when max per level is adjusted
	 */
	@Test void respondsToMaxPerLevel() {
		// no limit to detection
		var detector = createDetector();
		detector.getExtractor().setMaxTotalFeatures(-1);

		var input = new GrayF32(100, 120);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		ss.process(input);
		detector.process(ss);
		int countUnlimited = detector.getDetections().size();

		// hardly limit it
		detector.getExtractor().setMaxTotalFeatures(5);
		ss.process(input);
		detector.process(ss);
		int countLimited = detector.getDetections().size();
		assertTrue(countLimited >= 5);
		assertTrue(countLimited*2 < countUnlimited);
	}

	/**
	 * Will not exceed when the max total is set
	 */
	@Test void strictEnforcesMaxTotal() {
		// no limit to detection
		var detector = createDetector();

		var input = new GrayF32(100, 120);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		ss.process(input);
		detector.process(ss);
		int countUnlimited = detector.getDetections().size();
		assertTrue(countUnlimited >= 30);

		// force it to be a smaller number
		detector.maxFeaturesAll = countUnlimited/2;
		ss.process(input);
		detector.process(ss);
		int countLimited = detector.getDetections().size();
		assertEquals(countUnlimited/2, countLimited);

		// force it to be a larger number
		detector.maxFeaturesAll = countUnlimited*2;
		ss.process(input);
		detector.process(ss);
		countLimited = detector.getDetections().size();
		assertEquals(countUnlimited, countLimited);
	}
}
