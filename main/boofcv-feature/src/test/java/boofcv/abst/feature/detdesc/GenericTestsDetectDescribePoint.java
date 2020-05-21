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

package boofcv.abst.feature.detdesc;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Very basic tests which check for conformance to {@link DetectDescribePoint}.
 *
 * @author Peter Abeles
 */
public abstract class GenericTestsDetectDescribePoint<T extends ImageBase<T>,D extends TupleDesc> {
	int width = 100;
	int height = 120;

	Random rand = new Random(234);

	// expected capabilities
	boolean hasScale;
	boolean hasOrientation;

	T image;
	protected ImageType<T> imageType;
	Class<D> descType;

	// how precise the radius needs to be
	double radiusTol = 0.1;

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

				assertNotNull(desc);
				assertTrue(p.x != 0 && p.y != 0);
				assertTrue(p.x >= 0 && p.y >= 0 && p.x < image.width && p.y < image.height );

				if( radius != 1 )
					numScaleNotOne++;
				if( angle != 0 )
					numOrientationNotZero++;
			}

			if( hasScale )
				assertTrue(numScaleNotOne>0);
			else
				assertEquals(0, numScaleNotOne);

			if( hasOrientation )
				assertTrue(numOrientationNotZero>0);
			else
				assertEquals(0, numOrientationNotZero);
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

		assertEquals(hasScale, alg.hasScale());
	}

	@Test
	public void hasOrientation() {
		DetectDescribePoint<T,D> alg = createDetDesc();

		assertEquals(hasOrientation, alg.hasOrientation());
	}

	@Test
	public void getDescriptorType() {
		DetectDescribePoint<T,D> alg = createDetDesc();

		assertSame(descType, alg.getDescriptionType());
	}

	/**
	 * BVery basic sanity check to see if sets is correctly implemented
	 */
	@Test
	public void sets() {
		fail("implement");
	}

	private void checkIdenticalResponse(DetectDescribePoint<T, D> alg1, DetectDescribePoint<T, D> alg2) {
		int N = alg1.getNumberOfFeatures();
		assertTrue(N > 1);
		assertEquals(N,alg2.getNumberOfFeatures());

		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p1 = alg1.getLocation(i);
			int matched = -1;

			for (int j = 0; j < N; j++) {
				if( p1.isIdentical(alg2.getLocation(j), UtilEjml.EPS) &&
						alg1.getRadius(i) == alg2.getRadius(j) &&
						alg1.getOrientation(i) == alg2.getOrientation(j)) {
					matched = j;
					break;
				}
			}

			assertTrue(matched != -1 );

			D desc1 = alg1.getDescription(i);
			D desc2 = alg2.getDescription(matched);

			for( int j = 0; j < desc1.size(); j++ ) {
				assertEquals(desc1.getDouble(j), desc2.getDouble(j));
			}
		}
	}

	/**
	 * See if a sanity check is performed for color images. The bands must match
	 */
	@Test
	public void failBandMissMatch() {
		if( !(image instanceof ImageMultiBand) ) {
			return;
		}
		ImageMultiBand mb = (ImageMultiBand)image;
		ImageMultiBand bad = (ImageMultiBand)mb.createSameShape();
		bad.setNumberOfBands(mb.getNumBands()+1);

		DetectDescribePoint<T,D> alg = createDetDesc();
		try {
			alg.detect((T) bad);
			fail("Should have thrown an exception");
		} catch( IllegalArgumentException ignore ){}
	}
}
