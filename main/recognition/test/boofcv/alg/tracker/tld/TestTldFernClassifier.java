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

package boofcv.alg.tracker.tld;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTldFernClassifier {

	int width = 60;
	int height = 80;

	int numFerns = 5;
	int numLearnRandom = 7;

	Random rand = new Random(234);

	GrayU8 input = new GrayU8(width,height);

	InterpolatePixelS<GrayU8> interpolate = FactoryInterpolation.bilinearPixelS(
			GrayU8.class, BorderType.EXTENDED);

	public TestTldFernClassifier() {
		ImageMiscOps.fillUniform(input,rand,0,200);
		interpolate.setImage(input);
	}

	@Test
	public void learnFern() {
		TldFernClassifier<GrayU8> alg = createAlg();

		alg.setImage(input);
		alg.learnFern(true, new ImageRectangle(10,12,30,45));

		for( int i = 0; i < alg.managers.length; i++ ) {
			assertEquals(1, countNum(true,alg.managers[i]));
			assertEquals(0, countNum(false,alg.managers[i]));
		}
		assertTrue(alg.getMaxP() > 0 );
		assertTrue(alg.getMaxN() == 0 );

		alg.learnFern(false, new ImageRectangle(10,12,30,45));

		for( int i = 0; i < alg.managers.length; i++ ) {
			assertEquals(1, countNum(true,alg.managers[i]));
			assertEquals(1, countNum(false,alg.managers[i]));
		}
		assertTrue(alg.getMaxP() > 0 );
		assertTrue(alg.getMaxN() > 0 );
	}

	@Test
	public void learnFernNoise() {
		TldFernClassifier<GrayU8> alg = createAlg();

		alg.setImage(input);
		alg.learnFernNoise(true, new ImageRectangle(10,12,30,45));

		for( int i = 0; i < alg.managers.length; i++ ) {
			int found = countNum(true,alg.managers[i]);
			assertEquals(1+numLearnRandom, found);
			assertEquals(0, countNum(false,alg.managers[i]));
		}
		assertTrue(alg.getMaxP() > 0 );
		assertTrue(alg.getMaxN() == 0 );

		alg.learnFernNoise(false, new ImageRectangle(10,12,30,45));

		for( int i = 0; i < alg.managers.length; i++ ) {
			assertEquals(1+numLearnRandom, countNum(true,alg.managers[i]));
			assertEquals(1+numLearnRandom, countNum(false,alg.managers[i]));
		}
		assertTrue(alg.getMaxP() > 0 );
		assertTrue(alg.getMaxN() > 0 );
	}

	@Test
	public void computeFernValue() {

		TldFernDescription fern = new TldFernDescription(rand,10);

		ImageRectangle r = new ImageRectangle(2,20,12,28);

		float cx = r.x0 + (r.getWidth()-1)/2.0f;
		float cy = r.x0 + (r.getHeight()-1)/2.0f;
		float w = r.getWidth()-1;
		float h = r.getHeight()-1;

		boolean expected[] = new boolean[10];
		for( int i = 0; i < 10; i++ ) {
			Point2D_F32 a = fern.pairs[i].a;
			Point2D_F32 b = fern.pairs[i].b;

			float valA = interpolate.get(cx + a.x*w, cy + a.y*h);
			float valB = interpolate.get(cx + b.x*w, cy + b.y*h);

			expected[9-i] = valA < valB;
		}

		TldFernClassifier<GrayU8> alg = createAlg();
		alg.setImage(input);

		int found = alg.computeFernValue(cx,cy,r.getWidth(),r.getHeight(),fern);

		for( int i = 0; i < 10; i++ ) {
			assertTrue(expected[i] == (((found >> i) & 0x0001) == 1));
		}
	}

	@Test
	public void computeFernValueRand() {
		TldFernDescription fern = new TldFernDescription(rand,10);

		ImageRectangle r = new ImageRectangle(2,20,12,28);

		float cx = r.x0 + r.getWidth()/2.0f;
		float cy = r.x0 + r.getHeight()/2.0f;
		float w = r.getWidth();
		float h = r.getHeight();

		boolean expected[] = new boolean[10];
		for( int i = 0; i < 10; i++ ) {
			Point2D_F32 a = fern.pairs[i].a;
			Point2D_F32 b = fern.pairs[i].b;

			float valA = interpolate.get(cx + a.x*w, cy + a.y*h);
			float valB = interpolate.get(cx + b.x*w, cy + b.y*h);

			expected[9-i] = valA < valB;
		}

		TldFernClassifier<GrayU8> alg = createAlg();
		alg.setImage(input);

		int found = alg.computeFernValueRand(cx,cy,w,h,fern);

		int numDiff = 0;
		for( int i = 0; i < 10; i++ ) {
			if(expected[i] != (((found >> i) & 0x0001) == 1)) {
				numDiff++;
			}
		}

		assertTrue(numDiff != 0 );
		assertTrue( numDiff < 10 );
	}

		@Test
	public void renormalizeP() {
			TldFernClassifier<GrayU8> alg = createAlg();

			alg.maxP = 1000;
			alg.managers[2].table[1] = new TldFernFeature();
			alg.managers[2].table[1].numP = 600;

			alg.renormalizeP();

			int expected = 600/20;

			assertEquals(expected,alg.managers[2].table[1].numP);
	}

	@Test
	public void renormalizeN() {
		TldFernClassifier<GrayU8> alg = createAlg();

		alg.maxN = 1000;
		alg.managers[2].table[1] = new TldFernFeature();
		alg.managers[2].table[1].numN = 600;

		alg.renormalizeN();

		int expected = 600/20;

		assertEquals(expected,alg.managers[2].table[1].numN);
	}

	private TldFernClassifier<GrayU8> createAlg() {
		InterpolatePixelS<GrayU8> interpolate = FactoryInterpolation.bilinearPixelS(
				GrayU8.class, BorderType.EXTENDED);
		return new TldFernClassifier<>(rand,numFerns,8,numLearnRandom,10,interpolate);
	}

	private int countNum( boolean positive , TldFernManager manager ) {
		int total = 0;

		for( int i = 0; i < manager.table.length; i++ ) {

			TldFernFeature f = manager.table[i];

			if( f != null ) {
				if( positive )
					total += f.numP;
				else
					total += f.numN;
			}
		}

		return total;
	}
}
