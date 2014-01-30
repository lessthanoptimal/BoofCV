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
import boofcv.struct.image.MultiSpectral;
import org.ddogleg.struct.FastQueue;

import static boofcv.alg.segmentation.fb04.SegmentFelzenszwalb04.Edge;

/**
 * Computes edge weight as the absolute value of the different in pixel value.
 * A 4-connect neighborhood is considered.
 *
 * @author Peter Abeles
 */
// TODO create code generator
public class ComputeEdgeWeights4_MsU8 implements ComputeEdgeWeights<MultiSpectral<ImageUInt8>> {
	@Override
	public void process(MultiSpectral<ImageUInt8> input,
						int outputStartIndex , int outputStride ,
						FastQueue<Edge> edges) {

		int w = input.width-1;
		int h = input.height-1;

		final int numBands = input.getNumBands();

		// First consider the inner pixels
		for( int y = 0; y < h; y++ ) {
			int indexSrc = input.startIndex + y*input.stride;
			int indexDst = outputStartIndex + y*outputStride;

			for( int x = 0; x < w; x++ , indexSrc++ , indexDst++ ) {

				int weight1=0,weight2=0;

				for( int i = 0; i < numBands; i++ ) {
					ImageUInt8 band = input.getBand(i);

					int color0 = band.data[indexSrc] & 0xFF;              // (x,y)
					int color1 = band.data[indexSrc+1] & 0xFF;            // (x+1,y)
					int color2 = band.data[indexSrc+input.stride] & 0xFF; // (x,y+1)

					int diff1 = color0-color1;
					int diff2 = color0-color2;

					weight1 += diff1*diff1;
					weight2 += diff2*diff2;
				}

				Edge e1 = edges.grow();
				Edge e2 = edges.grow();

				e1.sortValue = (float)Math.sqrt(weight1);
				e1.indexA = indexDst;
				e1.indexB = indexDst+1;

				e2.sortValue = (float)Math.sqrt(weight2);
				e2.indexA = indexDst;
				e2.indexB = indexDst+outputStride;
			}
		}

		// Just pixels along the right border
		for( int y = 0; y < h; y++ ) {
			int indexSrc = input.startIndex + y*input.stride + w;
			int indexDst = outputStartIndex + y*outputStride + w;

			int weight2=0;

			for( int i = 0; i < numBands; i++ ) {
				ImageUInt8 band = input.getBand(i);

				int color0 = band.data[indexSrc] & 0xFF;              // (x,y)
				int color2 = band.data[indexSrc+input.stride] & 0xFF; // (x,y+1)

				int diff2 = color0-color2;

				weight2 += diff2*diff2;
			}

			Edge e2 = edges.grow();

			e2.sortValue = (float)Math.sqrt(weight2);
			e2.indexA = indexDst;
			e2.indexB = indexDst+outputStride;
		}

		// Finally, pixels along the bottom border
		int indexSrc = input.startIndex + h*input.stride;
		int indexDst = outputStartIndex + h*outputStride;

		for( int x = 0; x < w; x++ , indexSrc++ , indexDst++) {
			int weight1=0;

			for( int i = 0; i < numBands; i++ ) {
				ImageUInt8 band = input.getBand(i);

				int color0 = band.data[indexSrc] & 0xFF;              // (x,y)
				int color1 = band.data[indexSrc+1] & 0xFF;            // (x+1,y)

				int diff1 = color0-color1;

				weight1 += diff1*diff1;
			}

			Edge e1 = edges.grow();

			e1.sortValue = (float)Math.sqrt(weight1);
			e1.indexA = indexDst;
			e1.indexB = indexDst+1;
		}
	}
}
