/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detdesc;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Very basic tests which check for conformance to {@link DetectDescribePoint}.
 *
 * @author Peter Abeles
 */
public abstract class GenericTestsDetectDescribePoint<T extends ImageBase,D extends TupleDesc> {
	int width = 100;
	int height = 120;

	Random rand = new Random(234);

	// expected capabilities
	boolean hasScale;
	boolean hasOrientation;

	T image;
	protected ImageType<T> imageType;
	Class<D> descType;

	protected GenericTestsDetectDescribePoint(boolean hasScale, boolean hasOrientation,
											  ImageType<T> imageType, Class<D> descType) {
		this.hasScale = hasScale;
		this.hasOrientation = hasOrientation;
		this.imageType = imageType;
		this.descType = descType;

		image = imageType.createImage(width,height);
		GImageMiscOps.fillUniform(image, rand, 0, 100);
	}

	public void allTests() {
		detectFeatures();
		checkSubImage();
		hasScale();
		hasOrientation();
		checkMultipleCalls();
	}

	public abstract DetectDescribePoint<T,D> createDetDesc();


	/**
	 * Detects features inside the image and checks to see if it is in compliance of its reported capabilities
	 */
	@Test
	public void detectFeatures() {
		DetectDescribePoint<T,D> alg = createDetDesc();

		for( int imageIndex = 0; imageIndex < 10; imageIndex++ ) {
			int numScaleNotOne = 0;
			int numOrientationNotZero = 0;

			GImageMiscOps.fillUniform(image, rand, 0, 100);
			alg.detect(image);

			int N = alg.getNumberOfFeatures();
			assertTrue(N>5);

			for( int i = 0; i < N; i++ ) {
				Point2D_F64 p = alg.getLocation(i);
				double radius = alg.getRadius(i);
				double angle = alg.getOrientation(i);
				D desc = alg.getDescription(i);

				for( int j = 0; j < desc.size(); j++ ) {
					assertTrue( !Double.isNaN(desc.getDouble(j)) && !Double.isInfinite(desc.getDouble(j)));
				}

				assertTrue(desc!=null);
				assertTrue(p.x != 0 && p.y != 0);
				assertTrue(p.x+" "+p.y,p.x >= 0 && p.y >= 0 && p.x < image.width && p.y < image.height );

				if( radius != 1 )
					numScaleNotOne++;
				if( angle != 0 )
					numOrientationNotZero++;
			}

			if( hasScale )
				assertTrue(numScaleNotOne>0);
			else
				assertTrue(numScaleNotOne==0);

			if( hasOrientation )
				assertTrue(numOrientationNotZero>0);
			else
				assertTrue(numOrientationNotZero==0);
		}
	}

	/**
	 * Make sure sub-images are correctly handled by having it process one and see if it produces the
	 * same results
	 */
	@Test
	public void checkSubImage() {
		DetectDescribePoint<T,D> alg1 = createDetDesc();
		DetectDescribePoint<T,D> alg2 = createDetDesc();

		T imageSub = BoofTesting.createSubImageOf(image);

		alg1.detect(image);
		alg2.detect(imageSub);

		checkIdenticalResponse(alg1, alg2);
	}

	/**
	 * Make sure everything has been reset correctly and that multiple calls to the same input
	 * produce the same output
	 */
	@Test
	public void checkMultipleCalls() {
		DetectDescribePoint<T,D> alg1 = createDetDesc();
		DetectDescribePoint<T,D> alg2 = createDetDesc();

		// call twice
		alg1.detect(image);
		alg1.detect(image);
		// call once
		alg2.detect(image);

		checkIdenticalResponse(alg1, alg2);
	}

	@Test
	public void hasScale() {
		DetectDescribePoint<T,D> alg = createDetDesc();

		assertTrue(hasScale == alg.hasScale());
	}

	@Test
	public void hasOrientation() {
		DetectDescribePoint<T,D> alg = createDetDesc();

		assertTrue(hasOrientation == alg.hasOrientation());
	}

	@Test
	public void getDescriptorType() {
		DetectDescribePoint<T,D> alg = createDetDesc();

		assertTrue(descType==alg.getDescriptionType());
	}

	private void checkIdenticalResponse(DetectDescribePoint<T, D> alg1, DetectDescribePoint<T, D> alg2) {
		int N = alg1.getNumberOfFeatures();
		assertTrue(N > 1);
		assertEquals(N,alg2.getNumberOfFeatures());

		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p1 = alg1.getLocation(i);
			Point2D_F64 p2 = alg2.getLocation(i);

			assertTrue(p1.isIdentical(p2,1e-16));
			assertTrue(alg1.getRadius(i) == alg2.getRadius(i));
			assertTrue(alg1.getOrientation(i) == alg2.getOrientation(i));

			D desc1 = alg1.getDescription(i);
			D desc2 = alg2.getDescription(i);

			for( int j = 0; j < desc1.size(); j++ ) {
				assertTrue(desc1.getDouble(j) == desc2.getDouble(j));
			}
		}
	}
}
