/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.aztec;

import boofcv.abst.distort.FDistort;
import boofcv.alg.fiducial.aztec.AztecCode.Structure;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.image.ShowImages;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import static boofcv.alg.fiducial.aztec.AztecCode.Structure.COMPACT;
import static boofcv.alg.fiducial.aztec.AztecCode.Structure.FULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestAztecFinderPatternDetector {
	/** Easy test to see if it can detect a single well defined target */
	@Test void detectSingle() {
		detectSingle(COMPACT);
		detectSingle(FULL);
	}

	void detectSingle( Structure structure ) {
		// Create an image with a compact aztec code as input
		AztecCode marker = new AztecEncoder().setStructure(structure).addUpper("FOO").fixate();

		GrayU8 image = AztecGenerator.renderImage(5, 0, marker);
		GrayU8 binary = ThresholdImageOps.threshold(image, null, 100, true);

		double centerPixel = image.width/2.0;

		// Detector finder patterns
		AztecFinderPatternDetector<GrayU8> detector = createAlg();
		detector.process(image, binary);

		// There should be one marker with one layer
		DogArray<AztecPyramid> found = detector.getFound();
		assertEquals(1, found.size);

		if (structure == COMPACT) {
			assertEquals(1, found.get(0).layers.size);
		} else {
			assertEquals(2, found.get(0).layers.size);
			assertEquals(0.0, found.get(0).layers.get(1).center.distance(centerPixel, centerPixel), 1e-8);
		}
		assertEquals(0.0, found.get(0).layers.get(0).center.distance(centerPixel, centerPixel), 1e-8);
	}

	/** make sure it's rotation invariant. plus multiple calls to same detector */
	@Test void detectSingle_rotate() {
		detectSingle_rotate(COMPACT);
		detectSingle_rotate(FULL);
	}

	void detectSingle_rotate( Structure structure ) {
		// Create an image with a compact aztec code as input
		AztecCode marker = new AztecEncoder().setStructure(structure).addUpper("FOO").fixate();

		GrayU8 markerImage = AztecGenerator.renderImage(5, 0, marker);
		GrayU8 image = new GrayU8(markerImage.width + 100, markerImage.height + 100);
		GrayU8 binary = image.createSameShape();

		AztecFinderPatternDetector<GrayU8> detector = createAlg();
		for (int i = 0; i < 10; i++) {
			double angle = Math.PI*i/9.0;
			new FDistort(markerImage, image).rotate(angle).border(255).apply();
			ThresholdImageOps.threshold(image, binary, 100, true);

			detector.process(image, binary);

			DogArray<AztecPyramid> found = detector.getFound();
			if (found.isEmpty()) {
				ShowImages.showBlocking(image, "ASD " + i, 10_000, true);
			}
			assertEquals(1, found.size);

			if (structure == COMPACT) {
				assertEquals(1, found.get(0).layers.size);
			} else {
				assertEquals(2, found.get(0).layers.size);
			}
		}
	}

	@Test void detectMultiple() {
		// Create an image with two markers
		AztecCode marker = new AztecEncoder().setStructure(COMPACT).addUpper("FOO").fixate();
		GrayU8 markerImage = AztecGenerator.renderImage(5, 0, marker);
		GrayU8 image = new GrayU8(markerImage.width*2 + 20, markerImage.height);
		ImageMiscOps.copy(0, 0, 0, 0, markerImage.width, markerImage.height, markerImage, image);
		ImageMiscOps.copy(0, 0, markerImage.width + 20, 0, markerImage.width, markerImage.height, markerImage, image);

		GrayU8 binary = image.createSameShape();
		ThresholdImageOps.threshold(image, binary, 100, true);

		// Process the image
		AztecFinderPatternDetector<GrayU8> detector = createAlg();
		detector.process(image, binary);
		DogArray<AztecPyramid> found = detector.getFound();

		// Just see if it detected both
		assertEquals(2, found.size);
	}

	@Test void scoreTemplate() {
		int s = 20;
		int w = s*9;
		var image = new GrayU8(w, w);
		for (int i = 1; i < 5; i += 1) {
			int offset = i*s;
			int width = w-offset*2;
			int color = i%2==0?0:255;
			GImageMiscOps.fillRectangle(image, color, offset, offset, width, width);
		}

		Polygon2D_F64 polygon = new Polygon2D_F64(4);
		polygon.get(0).setTo(0,0);
		polygon.get(1).setTo(w,0);
		polygon.get(2).setTo(w,w);
		polygon.get(3).setTo(0,w);

		AztecFinderPatternDetector<GrayU8> detector = createAlg();
		detector.getInterpolate().setImage(image);

		// Perfect scenario
		assertEquals(1.0, detector.scoreTemplate(polygon, 100, 9));

		// Wrong assumed level
		assertTrue(detector.scoreTemplate(polygon, 100, 5) < 0.25);

		// damage the target
		GImageMiscOps.fillRectangle(image, 0, s, s, 2*s, s);
		double found = detector.scoreTemplate(polygon, 100, 9);
		assertTrue(found >= 0.95 && found < 1.0);
	}

	private AztecFinderPatternDetector<GrayU8> createAlg() {
		var config = new ConfigPolygonDetector(4, 4);
		return new AztecFinderPatternDetector<>(FactoryShapeDetector.polygon(config, GrayU8.class));
	}
}