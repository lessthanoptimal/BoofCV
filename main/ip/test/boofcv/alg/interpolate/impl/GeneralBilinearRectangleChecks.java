/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Peter Abeles
 */
public abstract class GeneralBilinearRectangleChecks<T extends ImageGray> {
	Class<T> imageType;

	Random rand = new Random(0xff34);

	int width = 320;
	int height = 240;

	int regionWidth;
	int regionHeight;
	float tl_x;
	float tl_y;

	protected GeneralBilinearRectangleChecks(Class<T> imageType) {
		this.imageType = imageType;
	}

	protected abstract T createImage( int width , int height );

	public InterpolatePixelS<T> createPixelInterpolate() {
		return FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);
	}

	public InterpolateRectangle<T> createRectangleInterpolate() {
		return FactoryInterpolation.bilinearRectangle(imageType);
	}

	/**
	 * Tell it to copy a region in the center
	 */
	@Test
	public void checkCenter() {
		checkRegion(10, 15, 2.11f, 5.23f);
	}

	/**
	 * See if it handles edge conditions gracefully.  should be barely inside
	 */
	@Test
	public void checkBottomRightEdge() {
		checkRegion(10, 15, 0, 0);
		checkRegion(10, 15, width - 10 - 1, height - 15 - 1);
	}


	@Test(expected=IllegalArgumentException.class)
	public void outsideImageBorder() {
		T img = createImage(width, height);
		InterpolateRectangle<T> interp = createRectangleInterpolate();
		interp.setImage(img);

		GrayF32 out = new GrayF32(20,20);
		interp.region(width-1, height-1, out );
	}

	@Test(expected=IllegalArgumentException.class)
	public void outsideImageBorder1_barely() {
		T img = createImage(20, 25);
		InterpolateRectangle<T> interp = createRectangleInterpolate();
		interp.setImage(img);

		GrayF32 out = new GrayF32(20,25);
		interp.region(-0.1f, -0.1f, out );
	}

	@Test(expected=IllegalArgumentException.class)
	public void outsideImageBorder2_barely() {
		T img = createImage(20, 25);
		InterpolateRectangle<T> interp = createRectangleInterpolate();
		interp.setImage(img);

		GrayF32 out = new GrayF32(20,25);
		interp.region(0.1f, 0.1f, out );
	}


	/**
	 * Should produce identical results when given a sub-image.
	 */
	@Test
	public void checkSubImage() {
		T imgA = createImage(width, height);
		GImageMiscOps.fillUniform(imgA, rand, 0, 200);

		regionWidth = 20;
		regionHeight = 25;

		InterpolateRectangle<T> interpA = createRectangleInterpolate();
		GrayF32 outA = new GrayF32(regionWidth,regionHeight);

		T imgB = BoofTesting.createSubImageOf(imgA);
		InterpolateRectangle<T> interpB = createRectangleInterpolate();
		GrayF32 outB = new GrayF32(regionWidth,regionHeight);

		interpA.setImage(imgA);
		interpB.setImage(imgB);

		interpA.region(5.4f, 8.6f, outA );
		interpB.region(5.4f, 8.6f, outB );

		for (int y = 0; y < regionHeight; y++) {
			for (int x = 0; x < regionWidth; x++) {
				assertTrue("( " + x + " , " + y + " )", outA.get(x, y) == outB.get(x, y));
			}
		}
	}
	/**
	 * Compare region against the value returned by get ImplBilinearPixel_F32
	 */
	public void checkRegion(int regionWidth, int regionHeight, float x, float y) {
		T img = createImage(width, height);
		GImageMiscOps.fillUniform(img, rand, 0, 20);

		this.regionWidth = regionWidth;
		this.regionHeight = regionHeight;
		this.tl_x = x;
		this.tl_y = y;
		BoofTesting.checkSubImage(this, "region", false, img);
	}

	public void region(T img) {
		InterpolatePixelS<T> interpPt = createPixelInterpolate();
		InterpolateRectangle<T> interp = createRectangleInterpolate();
		interp.setImage(img);
		interpPt.setImage(img);

		GrayF32 out = new GrayF32(regionWidth,regionHeight);

		interp.region(tl_x, tl_y, out );

		for (int y = 0; y < regionHeight; y++) {
			for (int x = 0; x < regionWidth; x++) {
				assertEquals("( "+x+" , "+y+" )",interpPt.get(x + tl_x, y + tl_y), out.get(x,y), 1e-4);
			}
		}
	}
}
