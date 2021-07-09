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

package boofcv.alg.template;

import boofcv.BoofTesting;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I16;
import georegression.struct.point.Point2D_I32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public abstract class GeneralTemplateMatchTests<T extends ImageGray<T>> extends BoofStandardJUnit {

	// image and template being matched
	T image;
	T template;
	T mask;

	// algorithm being evaluated
	TemplateMatchingIntensity<T> alg;

	// if a perfect match is zero additional tests can be done
	boolean isPerfectZero;

	protected GeneralTemplateMatchTests(TemplateMatchingIntensity<T> alg, Class<T> imageType) {
		this.alg = alg;

		image = GeneralizedImageOps.createSingleBand(imageType, 30, 40);
		template = GeneralizedImageOps.createSingleBand(imageType, 5, 8);
		mask = GeneralizedImageOps.createSingleBand(imageType, 5, 8);

		GImageMiscOps.fillUniform(template, rand, 50, 60);
	}

	protected GeneralTemplateMatchTests(TemplateIntensityImage.EvaluatorMethod<T> method, Class<T> imageType) {
		this(new TemplateIntensityImage<>(method),imageType);
	}

	/**
	 * Makes sure the template border is calculated correctly
	 */
	@Test
	void border_nomask() {
		alg.setInputImage(image);
		alg.process(template);

		assertEquals(template.width/2,alg.getBorderX0());
		assertEquals(template.height/2,alg.getBorderY0());
		assertEquals(template.width-template.width/2,alg.getBorderX1());
		assertEquals(template.height-template.height/2,alg.getBorderY1());
	}

	@Test
	void border_Mask() {
		alg.setInputImage(image);
		alg.process(template, mask);

		assertEquals(template.width/2,alg.getBorderX0());
		assertEquals(template.height/2,alg.getBorderY0());
		assertEquals(template.width-template.width/2,alg.getBorderX1());
		assertEquals(template.height-template.height/2,alg.getBorderY1());
	}

	@Test
	void negativeCase_nomask() {
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		alg.setInputImage(image);
		alg.process(template);

		if (isPerfectZero) {
			// there should be no perfect matches
			int x0 = alg.getBorderX0();
			int x1 = image.width - (template.width - x0);
			int y0 = alg.getBorderY0();
			int y1 = image.width - (template.width - x0);

			GrayF32 intensity = alg.getIntensity();

			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					if (intensity.get(x, y) == 0)
						fail("There should be no perfect matches");
				}
			}
		}
	}

	@Test
	void negativeCase_Mask() {
		GImageMiscOps.fillUniform(image, rand, 0, 200);
		GImageMiscOps.fill(mask,1);

		alg.setInputImage(image);
		alg.process(template,mask);

		if (isPerfectZero) {
			// there should be no perfect matches
			int x0 = alg.getBorderX0();
			int x1 = image.width - (template.width - x0);
			int y0 = alg.getBorderY0();
			int y1 = image.width - (template.width - x0);

			GrayF32 intensity = alg.getIntensity();

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
	void singleCase() {
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		int locationX = 10;
		int locationY = 12;

		setTemplate(locationX, locationY);

		alg.setInputImage(image);
		alg.process(template);
		checkExpected(false, new Point2D_I32(locationX, locationY));

		// uniform mask should produce identical results
		GImageMiscOps.fill(mask,1);
		alg.process(template, mask);
		checkExpected(false, new Point2D_I32(locationX, locationY));
	}

	@Test
	void same_size_img_template() {
		BoofConcurrency.USE_CONCURRENT=false;
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		alg.setInputImage(image);
		alg.process(image);
		checkExpected(true, new Point2D_I32(0, 0));

		T mask = this.mask.createNew(image.width,image.height);
		GImageMiscOps.fill(mask,1);
		alg.process(image,mask);
		checkExpected(true, new Point2D_I32(0, 0));

	}

	/**
	 * Input image has uniform intensity. See if bad stuff happens
	 */
	@Test
	void uniformImage() {
		uniformImage(0);
		uniformImage(30);
	}
	void uniformImage(float value) {
		GImageMiscOps.fill(image,value);

		setTemplate(5, 7);


		alg.setInputImage(image);
		alg.process(template);

		// Sanity check to see if anything really bad happened
		GrayF32 intensity = alg.getIntensity();
		for (int i = 0; i < intensity.totalPixels(); i++) {
			assertFalse(UtilEjml.isUncountable(intensity.data[i]));
		}

		// Same test with a mask
		GImageMiscOps.fill(mask,1);
		alg.process(template,mask);
		intensity = alg.getIntensity();
		for (int i = 0; i < intensity.totalPixels(); i++) {
			assertFalse(UtilEjml.isUncountable(intensity.data[i]));
		}
	}

	/**
	 * The mask is filled with zeros and even though there is a perfect match it shouldn't be found
	 */
	@Test
	void zeroMask() {
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		int locationX = 10;
		int locationY = 12;

		setTemplate(locationX, locationY);

		GImageMiscOps.fill(mask,0);
		GImageMiscOps.fill(alg.getIntensity(),0);
		alg.setInputImage(image);
		alg.process(template, mask);
		assertEquals(0, ImageStatistics.maxAbs(alg.getIntensity()),1e-4f);
	}

	/**
	 * Provide two matches
	 */
	@Test
	void multipleCases() {
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		Point2D_I32 a = new Point2D_I32(10, 12);
		Point2D_I32 b = new Point2D_I32(20, 16);

		setTemplate(a.x, a.y);
		setTemplate(b.x, b.y);

		alg.setInputImage(image);
		alg.process(template);
		checkExpected(false, a, b);

		// uniform mask should produce identical results
		GImageMiscOps.fill(mask,1);
		alg.process(template, mask);
		checkExpected(false, a, b);
	}

	/**
	 * If the mask is correctly applied then two matches will be found inside the image. Otherwise just one.
	 */
	@Test
	void maskDifferentiate() {
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		int x=10,y=12,tw=15,th=15;

		T template = image.createNew(tw,th);
		GImageMiscOps.fillUniform(template, rand, 0, 200);
		GImageMiscOps.fillBorder(template,150,2);

		image.subimage(x-tw/2,y-th/2,x-tw/2+tw,y-th/2+th).setTo(template);

//		ShowImages.showWindow(image,"foo",true);
//		BoofMiscOps.sleep(10000);

		GImageMiscOps.fillBorder(template,20,2); // change the template's border so that it won't match
		GImageMiscOps.fillBorder(template,50,1); // change the template's border so that it won't match

		alg.setInputImage(image);
		alg.process(template);

		float valueNoMask = fractionBest(alg.isMaximize(),alg.getIntensity(),x,y);
		float averageNoMask = fractionAverage(alg.isMaximize(),alg.getIntensity(),x,y);

		T mask = image.createNew(tw,th);
		double v = image.getImageType().getDataType().isInteger() ? 100 : 1;
		GImageMiscOps.fill(mask,v);
		GImageMiscOps.fillBorder(mask,0,2); // ignore the border

		alg.setInputImage(image);
		alg.process(template,mask);

		float valueMask = fractionBest(alg.isMaximize(),alg.getIntensity(), x, y);
		float averageMask = fractionAverage(alg.isMaximize(),alg.getIntensity(), x, y);

		// this score is designed to reduce the affect of the change in template "size"
		float scoreNoMask = valueNoMask/averageNoMask;
		float scoreMask = valueMask/averageMask;

		assertTrue(valueMask >= valueNoMask );
		// when masked it should be better at differentiating it from background noise
		assertTrue(scoreMask*0.9 > scoreNoMask );
	}

	float fractionBest(boolean maximize, GrayF32 intensity , int x , int y ) {
		float min = ImageStatistics.min(intensity);
		float max = ImageStatistics.max(intensity);

		float value = intensity.get(x,y);

		if( maximize )
			return (value-min)/(max-min);
		else
			return (max-value)/(max-min);
	}

	float fractionAverage(boolean maximize, GrayF32 intensity , int x , int y ) {
		float min = ImageStatistics.min(intensity);
		float max = ImageStatistics.max(intensity);
		float average = (float)ImageStatistics.mean(intensity);

		if( maximize )
			return (average-min)/(max-min);
		else
			return (max-average)/(max-min);
	}

	/**
	 * Provide inputs which are subimages and see if it produces the correct results
	 */
	@Test
	void subImage() {
		GImageMiscOps.fillUniform(image, rand, 0, 200);

		Point2D_I32 a = new Point2D_I32(10, 12);
		Point2D_I32 b = new Point2D_I32(20, 16);

		setTemplate(a.x, a.y);
		setTemplate(b.x, b.y);

		T subImage = BoofTesting.createSubImageOf(image);
		T subTemplate = BoofTesting.createSubImageOf(template);

		alg.setInputImage(subImage);
		alg.process(subTemplate);
		checkExpected(false, a, b);

		// uniform mask should produce identical results
		T subMask = BoofTesting.createSubImageOf(mask);
		GImageMiscOps.fill(subMask,1);
		alg.setInputImage(subImage);
		alg.process(subTemplate,subMask);
		checkExpected(false, a, b);
	}

	private void checkExpected(boolean strict, Point2D_I32... points) {
		// I'm being lazy, update this in the future
		assertFalse(alg.isBorderProcessed());

		// only process the regions which are not considered the border
		int x0 = alg.getBorderX0();
		int y0 = alg.getBorderY0();
		int x1 = alg.getBorderX1();
		int y1 = alg.getBorderY1();

		// solutions should be local maximums
		NonMaxSuppression extractor;
		ConfigExtract config = new ConfigExtract(2, -Float.MAX_VALUE, 0, true);
		if( !alg.isMaximize() ) {
			config.detectMaximums = false;
			config.detectMinimums = true;
		}
		extractor = FactoryFeatureExtractor.nonmax(config);

		QueueCorner found = new QueueCorner(10);

		GrayF32 intensity = alg.getIntensity().subimage(x0,y0,image.width-x1+1,image.height-y1+1);

		if( alg.isMaximize() )
			extractor.process(intensity, null,null,null, found);
		else
			extractor.process(intensity, null,null,found, null);

		assertTrue(found.size >= points.length);

		// search for all the expected matches
		for (Point2D_I32 expected : points) {
			int numMatches = 0;

			for (Point2D_I16 f : found.toList()) {
				if ( expected.distance(f.x,f.y) == 0.0 )
					numMatches++;
			}

			assertEquals(1, numMatches);
		}

		if( strict )
			assertEquals(points.length,found.size);
	}

	private void setTemplate(int x, int y) {
		image.subimage(x, y, x + template.width, y + template.height, null).setTo(template);
	}
}
