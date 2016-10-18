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

package boofcv.alg.segmentation.fh04.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.segmentation.fh04.FhEdgeWeights;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import java.util.Random;

import static boofcv.alg.segmentation.fh04.SegmentFelzenszwalbHuttenlocher04.Edge;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class GenericFhEdgeWeightsChecks<T extends ImageBase> {
	ImageType<T> imageType;
	ConnectRule rule;

	Random rand = new Random(234);

	protected GenericFhEdgeWeightsChecks(ImageType<T> imageType, ConnectRule rule) {
		this.imageType = imageType;
		this.rule = rule;
	}

	public abstract FhEdgeWeights<T> createAlg();

	public abstract float weight( T input , int indexA , int indexB );

	@Test
	public void basicTest() {
		T input = imageType.createImage(10,12);

		GImageMiscOps.fillUniform(input, rand, 0, 200);

		FhEdgeWeights<T> alg = createAlg();
		FastQueue<Edge> edges = new FastQueue<>(Edge.class, true);
		alg.process(input,edges);

		int hist[] = new int[input.width*input.height];

		// see if the edges computed the expected weight
		for( int i = 0; i < edges.size(); i++ ) {
			Edge e = edges.get(i);

			int indexA = e.indexA;
			int indexB = e.indexB;

			hist[indexA]++;
			hist[indexB]++;

			float expected = weight(input,indexA,indexB);
			assertEquals(expected,e.weight(),1e-4f);
		}

		// make sure each pixel was inspected
		if( rule == ConnectRule.FOUR ) {
			for( int y = 0; y < input.height; y++ ) {
				for( int x = 0; x < input.width; x++ ) {
					if( x >= 1 && x < input.width-1 && y >= 1 && y < input.height-1)
						assertEquals(hist[input.getIndex(x,y)],4);
					else if( x == 0 && y == 0 )
						assertEquals(hist[input.getIndex(x,y)],2);
					else if( x == input.width-1 && y == 0 )
						assertEquals(hist[input.getIndex(x,y)],2);
					else if( x == input.width-1 && y == input.height-1 )
						assertEquals(hist[input.getIndex(x,y)],2);
					else if( x == 0 && y == input.height-1 )
						assertEquals(hist[input.getIndex(x,y)],2);
					else {
						assertEquals(hist[input.getIndex(x,y)],3);
					}
				}
			}
		} else if( rule == ConnectRule.EIGHT ) {
			for( int y = 0; y < input.height; y++ ) {
				for( int x = 0; x < input.width; x++ ) {
					if( x >= 1 && x < input.width-1 && y >= 1 && y < input.height-1)
						assertEquals(hist[input.getIndex(x,y)],8);
					else if( x == 0 && y == 0 )
						assertEquals(hist[input.getIndex(x,y)],3);
					else if( x == input.width-1 && y == 0 )
						assertEquals(hist[input.getIndex(x,y)],3);
					else if( x == input.width-1 && y == input.height-1 )
						assertEquals(hist[input.getIndex(x,y)],3);
					else if( x == 0 && y == input.height-1 )
						assertEquals(hist[input.getIndex(x,y)],3);
					else {
						assertEquals(hist[input.getIndex(x,y)],5);
					}
				}
			}
		} else {
			throw new RuntimeException("Unknown rule");
		}
	}

	@Test
	public void subimage() {

		T input = imageType.createImage(10,12);
		GImageMiscOps.fillUniform(input, rand, 0, 200);
		T inputSub = BoofTesting.createSubImageOf(input);

		FhEdgeWeights<T> alg = createAlg();
		FastQueue<Edge> edges0 = new FastQueue<>(Edge.class, true);
		FastQueue<Edge> edges1 = new FastQueue<>(Edge.class, true);

		alg.process(input,edges0);
		alg.process(inputSub,edges1);

		// both should be identical
		assertEquals(edges0.size, edges1.size);

		for( int i = 0; i < edges0.size; i++ ) {
			Edge e0 = edges0.get(i);
			Edge e1 = edges1.get(i);

			assertEquals("i = "+i,e0.indexA,e1.indexA);
			assertEquals("i = "+i,e0.indexB,e1.indexB);
			assertEquals("i = "+i,e0.sortValue,e1.sortValue,1e-4f);
		}
	}
}
