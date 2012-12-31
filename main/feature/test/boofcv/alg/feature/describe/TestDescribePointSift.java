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

package boofcv.alg.feature.describe;

import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.image.ImageFloat32;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDescribePointSift {

	Random rand = new Random(234);
	int width = 80;
	int height = 100;

	ImageFloat32 image = new ImageFloat32(width,height);

	SiftImageScaleSpace ss = new SiftImageScaleSpace(1.6f,5,4,false);

	private DescribePointSift create() {
		return new DescribePointSift(4,8,8,0.5,2.5);
	}

	@Before
	public void initialize() {
		GImageMiscOps.fillUniform(image,rand,0,100);
		ss.constructPyramid(image);
		ss.computeDerivatives();
	}

// No sub-image test because that is handled by SiftImageScaleSpace's tests
//	@Test
//	public void checkSubImage() {
//	}

	/**
	 * Check to see if the descriptor changes for change in scale
	 */
	@Test
	public void checkChangeScale() {
		DescribePointSift alg = create();

		alg.setScaleSpace(ss);

		SurfFeature f1 = new SurfFeature(128);
		SurfFeature f2 = new SurfFeature(128);

		alg.process(30,31,2,0,f1);
		alg.process(30,31,3,0,f2);

		assertFalse(checkSimilar(f1,f2));
	}

	/**
	 * Check to see if the descriptor changes for change in angle
	 */
	@Test
	public void checkChangeAngle() {
		DescribePointSift alg = create();

		alg.setScaleSpace(ss);

		SurfFeature f1 = new SurfFeature(128);
		SurfFeature f2 = new SurfFeature(128);

		alg.process(30, 31, 2, 0, f1);
		alg.process(30,31,2,0.1,f2);

		assertFalse(checkSimilar(f1, f2));
	}

	/**
	 * Compute a feature at each corner and see if it blows up
	 */
	@Test
	public void checkImageBorder() {
		DescribePointSift alg = create();

		alg.setScaleSpace(ss);

		SurfFeature f = new SurfFeature(128);
		alg.process(0,0,2,0,f);
		checkNotTrivial(f);

		f = new SurfFeature(128);
		alg.process(width-1,0,2,0,f);
		checkNotTrivial(f);

		f = new SurfFeature(128);
		alg.process(width-1,height-1,2,0,f);
		checkNotTrivial(f);

		f = new SurfFeature(128);
		alg.process(0,height-1,2,0,f);
		checkNotTrivial(f);
	}

	/**
	 * Make sure the descriptor has been filled in with some values that change
	 */
	private void checkNotTrivial( SurfFeature f ) {
		int numNotZero = 0;
		for( int i = 0; i < f.value.length; i++ ) {
			if( f.value[i] != 0 )
				numNotZero++;
		}

		assertTrue(numNotZero!= 0);
	}

	private boolean checkSimilar( SurfFeature f1 , SurfFeature f2 ) {
		if( f2.laplacianPositive != f2.laplacianPositive )
			return false;

		for( int i = 0; i < f1.value.length; i++ ) {
			if( Math.abs(f1.value[i]-f2.value[i]) > 1e-8 )
				return false;
		}

		return true;
	}
}
