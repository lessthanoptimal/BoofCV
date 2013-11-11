/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.template;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I16;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public abstract class GeneralTemplateMatchTests<T extends ImageSingleBand> {

	Random rand = new Random(344);

	// image and template being matched
	T image;
	T template;

	// algorithm being evaluated
	TemplateMatchingIntensity<T> alg;

	// if a perfect match is zero additional tests can be done
	boolean isPerfectZero;

	public GeneralTemplateMatchTests(TemplateMatchingIntensity<T> alg, Class<T> imageType) {
		this.alg = alg;

		image = GeneralizedImageOps.createSingleBand(imageType, 30, 40);
		template = GeneralizedImageOps.createSingleBand(imageType, 5, 8);

		GImageMiscOps.fillUniform(template, rand, 50, 100);
	}

	public void allTests() {
		negativeCase();
		singleCase();
		multipleCases();
		subImage();
	}

	@Test
	public void negativeCase() {
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		alg.process(image, template);

		if (isPerfectZero) {
			// there should be no perfect matches
			int x0 = alg.getOffsetX();
			int x1 = image.width - (template.width - x0);
			int y0 = alg.getOffsetY();
			int y1 = image.width - (template.width - x0);

			ImageFloat32 intensity = alg.getIntensity();

			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					if (intensity.get(x, y) == 0)
						fail("There should be no perfect matches");
				}
			}
		}
	}

	/**
	 * There is only a single match
	 */
	@Test
	public void singleCase() {
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		int locationX = 10;
		int locationY = 12;

		setTemplate(locationX, locationY);

		alg.process(image, template);


		checkExpected(new Point2D_I32(locationX, locationY));
	}

	/**
	 * Provide two matches
	 */
	@Test
	public void multipleCases() {
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		Point2D_I32 a = new Point2D_I32(10, 12);
		Point2D_I32 b = new Point2D_I32(20, 16);

		setTemplate(a.x, a.y);
		setTemplate(b.x, b.y);

		alg.process(image, template);

		checkExpected(a, b);
	}

	/**
	 * Provide inputs which are subimages and see if it produces the correct results
	 */
	@Test
	public void subImage() {
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		Point2D_I32 a = new Point2D_I32(10, 12);
		Point2D_I32 b = new Point2D_I32(20, 16);

		setTemplate(a.x, a.y);
		setTemplate(b.x, b.y);

		T subImage = BoofTesting.createSubImageOf(image);
		T subTemplate = BoofTesting.createSubImageOf(template);

		alg.process(subImage, subTemplate);


		checkExpected(a, b);
	}

	private void checkExpected(Point2D_I32... points) {
		// I'm being lazy, update this in the future
		assertFalse(alg.isBorderProcessed());

		// only process the regions which are not considered the border
		int x0 = alg.getOffsetX();
		int x1 = image.width - (template.width - x0);
		int y0 = alg.getOffsetY();
		int y1 = image.height - (template.height - x0);

		ImageFloat32 adjusted = alg.getIntensity().subimage(x0, y0, x1, y1, null);

		// solutions should be local maximums
		NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(2, -Float.MAX_VALUE, 0, true));

		QueueCorner found = new QueueCorner(10);

		extractor.process(adjusted, null,null,null, found);

		assertTrue(found.size >= points.length);

		// search for all the expected matches
		for (Point2D_I32 expected : points) {
			int numMatches = 0;

			for (Point2D_I16 f : found.toList()) {
				if (f.x == expected.x && f.y == expected.y)
					numMatches++;
			}

			assertEquals(1, numMatches);
		}
	}

	private void setTemplate(int x, int y) {
		image.subimage(x, y, x + template.width, y + template.height, null).setTo(template);
	}
}
