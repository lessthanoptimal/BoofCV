/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.InterleavedF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TestDenseOpticalFlowBlockPyramid extends BoofStandardJUnit {

	@Test void findFlow() {
		Dummy alg = new Dummy(3,2,200,GrayU8.class);

		alg.minScore = 0.1f;
		alg.targetX = 4;
		alg.targetY = 8;

		GrayU8 image = new GrayU8(30,40);

		// see if it selects the obvious minimum
		assertEquals(0.1f, alg.findFlow(6, 7, image), 1e-4);
		assertTrue(alg.tmpValid);
		assertEquals(-2,alg.tmpX,1e-4);
		assertEquals(1,alg.tmpY,1e-4);

		// now try the case where the error is too high
		alg.minScore = 100000000f;
		alg.findFlow(6, 7, image);
		assertFalse(alg.tmpValid);

		// now give it a case where everything has the same score. See if it picks the one with the least motion
		alg.sameScore = true;
		alg.minScore = 0.1f;
		alg.findFlow(6, 7, image);
		assertTrue(alg.tmpValid);
		assertEquals(0,alg.tmpX,1e-4);
		assertEquals(0,alg.tmpY,1e-4);
	}

	@Test void checkNeighbors() {
		int sr = 3;
		int rr = 2;
		Dummy alg = new Dummy(sr,rr,200,GrayU8.class);

		alg.scores = new float[20*30];
		Arrays.fill(alg.scores,20);
		InterleavedF32 flows = new InterleavedF32(20,30,2);
		ImageMiscOps.fill(flows, Float.NaN);

		float tmpX = -1;
		float tmpY = 2;

		// checks to see if a pixel is invalid that it's flow is always set
		// if a pixel is valid then the score is only set if the score is better
		flows.setBand(6,5,0, 1);
		flows.setBand(6,5,1, 2);
		alg.scores[ 5*20+6 ] = 10;
		flows.setBand(5,5,0, 1);
		flows.setBand(5,5,1, 2);
		alg.scores[ 5*20+5 ] = 4;
		// same score, but more motion
		alg.scores[ 6*20+5 ] = 5;
		flows.setBand(5,6,0, 2);
		flows.setBand(5,6,1, 2);
		// same score, but less motion
		alg.scores[ 6*20+6 ] = 5;
		flows.setBand(6,6,0, 0);
		flows.setBand(6,6,1, 1);

		alg.checkNeighbors(6,7,tmpX,tmpY,flows,5);

		for( int i = -rr; i <= rr; i++ ) {
			for( int j = -rr; j <= rr; j++ ) {
				int x = j+6;
				int y = i+7;

				float fx = flows.getBand(x,y,0);
				float fy = flows.getBand(x,y,1);

				assertTrue(!Float.isNaN(fx));
				if( x == 5 && y == 5 ) {
					assertEquals(4,alg.scores[y*20+x],1e-4);
					assertEquals(1,fx,1e-4);
					assertEquals(2,fy,1e-4);
				} else if( x == 6 && y == 6 ) {
					assertEquals(5,alg.scores[y*20+x],1e-4);
					assertEquals(0,fx,1e-4);
					assertEquals(1,fy,1e-4);
				} else {
					assertEquals(5,alg.scores[y*20+x],1e-4);
					assertEquals(-1,fx,1e-4);
					assertEquals(2,fy,1e-4);
				}
			}
		}
	}


	public static class Dummy extends DenseOpticalFlowBlockPyramid {

		public boolean sameScore = false;
		public int targetX;
		public int targetY;
		public float minScore;

		public Dummy(int searchRadius, int regionRadius, int maxPerPixelError, Class imageType) {
			super(searchRadius, regionRadius, maxPerPixelError, imageType);
		}

		@Override
		protected void extractTemplate(int cx, int cy, ImageGray prev) {}

		@Override
		protected float computeError(int cx, int cy, ImageGray curr) {
			if( sameScore )
				return minScore;
			else {
				int dx = cx-targetX;
				int dy = cy-targetY;

				return dx*dx + dy*dy + minScore;
			}
		}
	}

}
