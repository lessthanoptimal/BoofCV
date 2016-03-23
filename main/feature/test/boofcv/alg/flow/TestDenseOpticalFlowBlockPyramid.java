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

package boofcv.alg.flow;

import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestDenseOpticalFlowBlockPyramid {

	@Test
	public void findFlow() {
		Dummy alg = new Dummy(3,2,200,GrayU8.class);

		alg.minScore = 0.1f;
		alg.targetX = 4;
		alg.targetY = 8;

		GrayU8 image = new GrayU8(30,40);
		ImageFlow.D flow = new ImageFlow.D();

		// see if it selects the obvious minimum
		assertEquals(0.1f, alg.findFlow(6, 7, image, flow), 1e-4);
		assertTrue(flow.isValid());
		assertEquals(-2,flow.x,1e-4);
		assertEquals(1,flow.y,1e-4);

		// now try the case where the error is too high
		alg.minScore = 100000000f;
		alg.findFlow(6, 7, image, flow);
		assertFalse(flow.isValid());

		// now give it a case where everything has the same score.  See if it picks the one with the least motion
		alg.sameScore = true;
		alg.minScore = 0.1f;
		alg.findFlow(6, 7, image, flow);
		assertTrue(flow.isValid());
		assertEquals(0,flow.x,1e-4);
		assertEquals(0,flow.y,1e-4);
	}

	@Test
	public void checkNeighbors() {
		int sr = 3;
		int rr = 2;
		Dummy alg = new Dummy(sr,rr,200,GrayU8.class);

		alg.scores = new float[20*30];
		Arrays.fill(alg.scores,20);
		ImageFlow flows = new ImageFlow(20,30);
		flows.invalidateAll();
		ImageFlow.D tmp = new ImageFlow.D();

		tmp.x = -1;
		tmp.y = 2;

		// checks to see if a pixel is invalid that it's flow is always set
		// if a pixel is valid then the score is only set if the score is better
		flows.get(6,5).x = 1;
		flows.get(6,5).y = 2;
		alg.scores[ 5*20+6 ] = 10;
		flows.get(5,5).x = 1;
		flows.get(5,5).y = 2;
		alg.scores[ 5*20+5 ] = 4;
		// same score, but more motion
		alg.scores[ 6*20+5 ] = 5;
		flows.get(5,6).x = 2;
		flows.get(5,6).y = 2;
		// same score, but less motion
		alg.scores[ 6*20+6 ] = 5;
		flows.get(6,6).x = 0;
		flows.get(6,6).y = 1;

		alg.checkNeighbors(6,7,tmp,flows,5);

		for( int i = -rr; i <= rr; i++ ) {
			for( int j = -rr; j <= rr; j++ ) {
				int x = j+6;
				int y = i+7;

				ImageFlow.D f = flows.get(x,y);

				assertTrue(f.isValid());
				if( x == 5 && y == 5 ) {
					assertEquals(4,alg.scores[y*20+x],1e-4);
					assertEquals(1,f.x,1e-4);
					assertEquals(2,f.y,1e-4);
				} else if( x == 6 && y == 6 ) {
					assertEquals(5,alg.scores[y*20+x],1e-4);
					assertEquals(0,f.x,1e-4);
					assertEquals(1,f.y,1e-4);
				} else {
					assertEquals(x+" "+y,5,alg.scores[y*20+x],1e-4);
					assertEquals(-1,f.x,1e-4);
					assertEquals(2,f.y,1e-4);
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
