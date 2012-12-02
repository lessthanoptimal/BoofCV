/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Very basic tests which check for conformance to {@link DetectDescribePoint}.
 *
 * @author Peter Abeles
 */
public abstract class GenericTestsDetectDescribePoint<T extends ImageSingleBand,D extends TupleDesc> {
	int width = 100;
	int height = 120;

	Random rand = new Random(234);

	// expected capabilities
	boolean hasScale;
	boolean hasOrientation;

	T image;
	Class<T> imageType;
	Class<D> descType;

	protected GenericTestsDetectDescribePoint(boolean hasScale, boolean hasOrientation,
											  Class<T> imageType, Class<D> descType) {
		this.hasScale = hasScale;
		this.hasOrientation = hasOrientation;
		this.imageType = imageType;
		this.descType = descType;

		image = GeneralizedImageOps.createSingleBand(imageType,width,height);
		GImageMiscOps.fillUniform(image, rand, 0, 100);
	}

	public void allTests() {
		detectFeatures();
		hasScale();
		hasOrientation();
	}

	public abstract DetectDescribePoint<T,D> createDetDesc();


	/**
	 * Detects features inside the image and checks to see if it is in compliance of its reported capabilities
	 */
	@Test
	public void detectFeatures() {
		DetectDescribePoint<T,D> alg = createDetDesc();

		int numScaleNotOne = 0;
		int numOrientationNotZero = 0;

		alg.detect(image);

		int N = alg.getNumberOfFeatures();
		assertTrue(N>5);

		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p = alg.getLocation(i);
			double scale = alg.getScale(i);
			double angle = alg.getOrientation(i);
			D desc = alg.getDescriptor(i);

			assertTrue(desc!=null);
			assertTrue(p.x != 0 && p.y != 0);

			if( scale != 1 )
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

		assertTrue(descType==alg.getDescriptorType());
	}
}
