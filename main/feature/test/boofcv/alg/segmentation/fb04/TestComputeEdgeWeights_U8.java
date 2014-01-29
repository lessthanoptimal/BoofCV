/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.fb04;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static boofcv.alg.segmentation.fb04.SegmentFelzenszwalb04.Edge;
import static boofcv.alg.segmentation.fb04.SegmentFelzenszwalb04.computeNumberOfEdges;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestComputeEdgeWeights_U8 {

	Random rand = new Random(234);

	@Test
	public void basic() {
		ImageUInt8 input = new ImageUInt8(5,8);
		ImageSInt32 output = new ImageSInt32(5,8);
		performTest(input,output);
	}

	/**
	 * Test case when input and output are subimage, but not identical indicies
	 */
	@Test
	public void subimage() {
		ImageUInt8 input = new ImageUInt8(5,8);
		ImageSInt32 output = new ImageSInt32(10,12).subimage(2,3,7,11,null);
		performTest(input, output);
	}

	private void performTest(ImageUInt8 input, ImageSInt32 output ) {
		ImageMiscOps.fillUniform(input, rand, 0, 200);

		ComputeEdgeWeights_U8 alg = new ComputeEdgeWeights_U8();
		List<Edge> edges = createList(computeNumberOfEdges(input.width, input.height));
		alg.process(input,output.startIndex, output.stride,edges);

		int hist[] = new int[5*8];

		// see if the edges computed the expected weight
		for( int i = 0; i < edges.size(); i++ ) {
			Edge e = edges.get(i);

			int indexA = convertIndex(e.indexA,output);
			int indexB = convertIndex(e.indexB,output);

			hist[indexA]++;
			hist[indexB]++;

			float expected = Math.abs((input.data[indexA]&0xFF) - (input.data[indexB]&0xFF));
			assertEquals(expected,e.weight(),1e-4f);
		}

		// make sure each pixel was inspected
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
	}

	public int convertIndex( int index , ImageBase image ) {
		int x = (index-image.startIndex)%image.stride;
		int y = (index-image.startIndex)/image.stride;

		return y*image.width + x;
	}

	public List<Edge> createList( int N ) {

		List<Edge> ret = new ArrayList<Edge>();

		for( int i = 0; i < N; i++ ) {
			ret.add( new Edge());
		}

		return ret;
	}
}
