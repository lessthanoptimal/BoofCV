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

package boofcv.abst.feature.detdesc;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Very basic tests which check for conformance to {@link boofcv.abst.feature.detdesc.DetectDescribePoint}.
 *
 * @author Peter Abeles
 */
public abstract class GenericTestsDetectDescribeMulti<T extends ImageGray, TD extends TupleDesc> {
	int width = 100;
	int height = 120;

	Random rand = new Random(234);

	T image;
	Class<T> imageType;
	Class<TD> descType;

	protected GenericTestsDetectDescribeMulti(Class<T> imageType, Class<TD> descType) {
		this.imageType = imageType;
		this.descType = descType;

		image = GeneralizedImageOps.createSingleBand(imageType,width,height);
		GImageMiscOps.fillUniform(image, rand, 0, 100);
	}

	public void allTests() {
		detectFeatures();
		checkSubImage();
		checkMultipleCalls();
	}

	public abstract DetectDescribeMulti<T, TD> createDetDesc();


	/**
	 * Detects features inside the image and checks to see if it is in compliance of its reported capabilities
	 */
	@Test
	public void detectFeatures() {
		DetectDescribeMulti<T, TD> alg = createDetDesc();

		alg.process(image);

		for( int n = 0; n < alg.getNumberOfSets(); n++ ) {
			PointDescSet<TD> set = alg.getFeatureSet(n);
			int N = set.getNumberOfFeatures();
			assertTrue(N>5);

			for( int i = 0; i < N; i++ ) {
				Point2D_F64 p = set.getLocation(i);
				TD desc = set.getDescription(i);

				assertTrue(desc!=null);
				assertTrue(p.x != 0 && p.y != 0);
				assertTrue(p.x >= 0 && p.y >= 0 && p.x < image.width && p.y < image.height );
			}
		}
	}

	/**
	 * Make sure sub-images are correctly handled by having it process one and see if it produces the
	 * same results
	 */
	@Test
	public void checkSubImage() {
		DetectDescribeMulti<T, TD> alg1 = createDetDesc();
		DetectDescribeMulti<T, TD> alg2 = createDetDesc();

		T imageSub = BoofTesting.createSubImageOf(image);

		alg1.process(image);
		alg2.process(imageSub);

		checkIdenticalResponse(alg1, alg2);
	}

	/**
	 * Make sure everything has been reset correctly and that multiple calls to the same input
	 * produce the same output
	 */
	@Test
	public void checkMultipleCalls() {
		DetectDescribeMulti<T, TD> alg1 = createDetDesc();
		DetectDescribeMulti<T, TD> alg2 = createDetDesc();

		// call twice
		alg1.process(image);
		alg1.process(image);
		// call once
		alg2.process(image);

		checkIdenticalResponse(alg1, alg2);
	}

	@Test
	public void getDescriptorType() {
		DetectDescribeMulti<T, TD> alg = createDetDesc();

		assertTrue(descType==alg.getDescriptionType());
	}

	private void checkIdenticalResponse(DetectDescribeMulti<T, TD> alg1, DetectDescribeMulti<T, TD> alg2) {

		for( int n = 0; n < alg1.getNumberOfSets(); n++ ) {
			PointDescSet<TD> set1 = alg1.getFeatureSet(n);
			PointDescSet<TD> set2 = alg2.getFeatureSet(n);

			int N = set1.getNumberOfFeatures();
			assertTrue(N > 1);
			assertEquals(N,set2.getNumberOfFeatures());

			for( int i = 0; i < N; i++ ) {
				Point2D_F64 p1 = set1.getLocation(i);
				Point2D_F64 p2 = set2.getLocation(i);

				assertTrue(p1.isIdentical(p2,1e-16));

				TD desc1 = set1.getDescription(i);
				TD desc2 = set2.getDescription(i);

				for( int j = 0; j < desc1.size(); j++ ) {
					assertTrue(desc1.getDouble(j) == desc2.getDouble(j));
				}
			}
		}
	}
}
