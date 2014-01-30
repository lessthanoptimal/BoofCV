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

import boofcv.struct.image.ImageUInt8;
import org.ddogleg.struct.FastQueue;

import static boofcv.alg.segmentation.fb04.SegmentFelzenszwalb04.Edge;

/**
 * Computes edge weight as the absolute value of the different in pixel value.
 * A 4-connect neighborhood is considered.
 *
 * @author Peter Abeles
 */
// TODO create code generator
public class ComputeEdgeWeights4_U8 implements ComputeEdgeWeights<ImageUInt8> {
	@Override
	public void process(ImageUInt8 input,
						int outputStartIndex , int outputStride ,
						FastQueue<Edge> edges) {

		int w = input.width-1;
		int h = input.height-1;

		// First consider the inner pixels
		for( int y = 0; y < h; y++ ) {
			int indexSrc = input.startIndex + y*input.stride;
			int indexDst = outputStartIndex + y*outputStride;

			for( int x = 0; x < w; x++ , indexSrc++ , indexDst++ ) {
			 	int color0 = input.data[indexSrc] & 0xFF;              // (x,y)
				int color1 = input.data[indexSrc+1] & 0xFF;            // (x+1,y)
				int color2 = input.data[indexSrc+input.stride] & 0xFF; // (x,y+1)

				Edge e1 = edges.grow();
				Edge e2 = edges.grow();

				e1.sortValue = Math.abs(color1-color0);
				e1.indexA = indexDst;
				e1.indexB = indexDst+1;

				e2.sortValue = Math.abs(color2-color0);
				e2.indexA = indexDst;
				e2.indexB = indexDst+outputStride;
			}
		}

		// Just pixels along the right border
		for( int y = 0; y < h; y++ ) {
			int indexSrc = input.startIndex + y*input.stride + w;
			int indexDst = outputStartIndex + y*outputStride + w;

			int color0 = input.data[indexSrc] & 0xFF;              // (x,y)
			int color2 = input.data[indexSrc+input.stride] & 0xFF; // (x,y+1)

			Edge e2 = edges.grow();

			e2.sortValue = Math.abs(color2-color0);
			e2.indexA = indexDst;
			e2.indexB = indexDst+outputStride;
		}

		// Finally, pixels along the bottom border
		int indexSrc = input.startIndex + h*input.stride;
		int indexDst = outputStartIndex + h*outputStride;

		for( int x = 0; x < w; x++ , indexSrc++ , indexDst++) {
			int color0 = input.data[indexSrc] & 0xFF;              // (x,y)
			int color1 = input.data[indexSrc+1] & 0xFF;            // (x+1,y)

			Edge e1 = edges.grow();

			e1.sortValue = Math.abs(color1-color0);
			e1.indexA = indexDst;
			e1.indexB = indexDst+1;
		}
	}
}
