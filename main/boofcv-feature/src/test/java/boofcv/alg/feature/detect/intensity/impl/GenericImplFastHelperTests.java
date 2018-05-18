/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.DiscretizedCircle;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class GenericImplFastHelperTests<T extends ImageGray<T>> {

	Class<T> imageType;
	FastHelper<T> alg;
	T image;
	int[] offsets;
	int threshold;

	public GenericImplFastHelperTests(Class<T> imageType, FastHelper<T> alg , int threshold ) {
		this.imageType = imageType;
		this.alg = alg;
		this.threshold = threshold;
		image = GeneralizedImageOps.createSingleBand(imageType,30,40);
		offsets = DiscretizedCircle.imageOffsets(3, image.stride);
	}

	@Test
	public void scoreLower() {
		GImageMiscOps.fill(image, 0);

		alg.setImage(image,offsets);

		// all the same intensity value
		alg.setThreshold(image.getIndex(10,11));
		assertEquals(0,alg.scoreLower(image.getIndex(10,11)),1e-8);

		GeneralizedImageOps.set(image,10,11,30);
		alg.setThreshold(image.getIndex(10,11));

		setCircle(10, 11, 30-threshold-1);
		float valueA = alg.scoreLower(image.getIndex(10,11));

		setCircle(10,11,30-threshold-5);
		float valueB = alg.scoreLower(image.getIndex(10,11));

		assertTrue(valueA < 0 );
		assertTrue(valueB < 0 );
		assertTrue(valueB < valueA);
	}

	@Test
	public void scoreUpper() {
		GImageMiscOps.fill(image, 0);

		alg.setImage(image,offsets);

		// all the same intensity value
		alg.setThreshold(image.getIndex(10,11));
		assertEquals(0,alg.scoreLower(image.getIndex(10,11)),1e-8);

		GeneralizedImageOps.set(image,10,11,30);
		alg.setThreshold(image.getIndex(10,11));

		setCircle(10, 11, 30+threshold+1);
		float valueA = alg.scoreUpper(image.getIndex(10,11));

		setCircle(10, 11, 30+threshold+5);
		float valueB = alg.scoreUpper(image.getIndex(10,11));

		assertTrue(valueA > 0 );
		assertTrue(valueB > 0 );
		assertTrue(valueB > valueA);
	}

	@Test
	public void checkPixelLower() {
		GImageMiscOps.fill(image, 0);

		alg.setImage(image,offsets);
		GeneralizedImageOps.set(image,10,11,30);
		GeneralizedImageOps.set(image,10,12,30-threshold-1);
		GeneralizedImageOps.set(image,10,13,30+threshold+1);
		GeneralizedImageOps.set(image,10,14,30);

		alg.setThreshold(image.getIndex(10,11));

		assertTrue(alg.checkPixel(image.getIndex(10,12)) < 0);
		assertFalse(alg.checkPixel(image.getIndex(10,13)) < 0);
		assertFalse(alg.checkPixel(image.getIndex(10,14)) < 0);
	}

	@Test
	public void checkPixelUpper() {
		GImageMiscOps.fill(image, 0);

		alg.setImage(image,offsets);
		GeneralizedImageOps.set(image,10,11,30);
		GeneralizedImageOps.set(image,10,12,30-threshold-1);
		GeneralizedImageOps.set(image,10,13,30+threshold+1);
		GeneralizedImageOps.set(image,10,14,30);

		alg.setThreshold(image.getIndex(10,11));

		assertFalse(alg.checkPixel(image.getIndex(10, 12))>0);
		assertTrue(alg.checkPixel(image.getIndex(10, 13))>0);
		assertFalse(alg.checkPixel(image.getIndex(10,14))>0);
	}

	private void setCircle( int x , int y , int value ) {
		for( int i = 0; i < offsets.length; i++ ) {
			int offY = offsets[i] / image.stride;
			int offX = offsets[i] - offY * image.stride;

			if( offX > 3 ) {
				offY++;
				offX -= image.stride;
			} else if( offX < -3 ) {
				offY--;
				offX += image.stride;
			}

			GeneralizedImageOps.set(image,x+offX,y+offY,value);
		}
	}
}
