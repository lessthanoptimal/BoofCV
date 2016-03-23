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

import boofcv.alg.InputSanityCheck;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ImagePyramid;

import java.util.Arrays;

/**
 * <p>
 * Computes dense optical flow optical using pyramidal approach with square regions and a locally exhaustive search.
 * Flow estimates from higher layers in the pyramid are used to provide an initial estimate flow lower layers.
 * For each pixel in the 'prev' image, a square region centered around it is compared against
 * all other regions within the specified search radius of it
 * in image 'curr'.  For each candidate flow the error is computed.  After the best score has been found each local
 * pixel which contributed to that square region is checked.  When a pixel is checked its current score compared
 * to see if it's better than the score it was previously assigned (if any) then its flow and score will be set to
 * the current.  This improves the handled along object edges.  If only the flow is considered when a pixel is the
 * center then it almost always fails at edges.
 * </p>
 *
 * <p>
 * When scoring hypotheses for optical flow and there is a tie, select the hypothesis with the least amount of motion.
 * This only really comes into play when there is absolutely no texture in real-world data.
 * </p>
 *
 * <p>
 * By checking all pixels associated with the score and not just the center one to see if it has a better
 * score the edges of objects is handled better.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class DenseOpticalFlowBlockPyramid<T extends ImageGray> {

	// the maximum displacement it will search
	protected int searchRadius;
	// radius of the square region it is searching with
	protected int regionRadius;

	// storage for the region in 'prev'
	protected T template;

	// maximum allowed error between two regions for it to be a valid flow
	protected int maxError;

	// flow in the previous layer
	protected ImageFlow flowPrevLayer = new ImageFlow(1,1);
	// flow in the current layer
	protected ImageFlow flowCurrLayer = new ImageFlow(1,1);

	protected ImageFlow.D tmp = new ImageFlow.D();

	// fit score for each pixel
	protected float scores[] = new float[0];

	/**
	 * Configures the search.
	 *
	 * @param searchRadius Determines the size of the area search for matches. area = (2*r + 1)^2
	 * @param regionRadius Radius of the square region
	 * @param maxPerPixelError Maximum error allowed per pixel.
	 * @param imageType Type of image which is being processed.
	 */
	public DenseOpticalFlowBlockPyramid(int searchRadius, int regionRadius,
										int maxPerPixelError, Class<T> imageType) {
		this.searchRadius = searchRadius;
		this.regionRadius = regionRadius;

		int w = regionRadius*2+1;
		maxError = maxPerPixelError*w*w;

		template = GeneralizedImageOps.createSingleBand(imageType,w, w);
	}

	/**
	 * Computes the optical flow form 'prev' to 'curr' and stores the output into output
	 * @param pyramidPrev Previous image
	 * @param pyramidCurr Current image
	 */
	public void process( ImagePyramid<T> pyramidPrev , ImagePyramid<T> pyramidCurr ) {

		InputSanityCheck.checkSameShape(pyramidPrev, pyramidCurr);

		int numLayers = pyramidPrev.getNumLayers();

		for( int i = numLayers-1; i >= 0; i-- ) {

			T prev = pyramidPrev.getLayer(i);
			T curr = pyramidCurr.getLayer(i);

			flowCurrLayer.reshape(prev.width, prev.height);

			int N = prev.width*prev.height;
			if( scores.length < N )
				scores = new float[N];
			// mark all the scores as being very large so that if it has not been processed its score
			// will be set inside of checkNeighbors.
			Arrays.fill(scores,0,N,Float.MAX_VALUE);

			int x1 = prev.width-regionRadius;
			int y1 = prev.height-regionRadius;

			if( i == numLayers-1 ) {
				// the top most layer in the pyramid has no hint
				for( int y = regionRadius; y < y1; y++ ) {
					for( int x = regionRadius; x < x1; x++ ) {
						extractTemplate(x,y,prev);
						float score = findFlow(x,y,curr,tmp);

						if( tmp.isValid() )
							checkNeighbors(x,y,tmp, flowCurrLayer,score);
						else
							flowCurrLayer.unsafe_get(x, y).markInvalid();
					}
				}
			} else {
				// for all the other layers use the hint of the previous layer to start its search
				double scale = pyramidPrev.getScale(i+1)/pyramidPrev.getScale(i);
				for( int y = regionRadius; y < y1; y++ ) {
					for( int x = regionRadius; x < x1; x++ ) {
						// grab the flow in higher level pyramid
						ImageFlow.D p = flowPrevLayer.get((int)(x/scale),(int)(y/scale));
						if( !p.isValid() )
							continue;

						// get the template around the current point in this layer
						extractTemplate(x,y,prev);

						// add the flow from the higher layer (adjusting for scale and rounding) as the start of
						// this search
						int deltaX = (int)(p.x*scale+0.5);
						int deltaY = (int)(p.y*scale+0.5);

						int startX = x + deltaX;
						int startY = y + deltaY;

						float score = findFlow(startX,startY,curr,tmp);

						// find flow only does it relative to the starting point
						tmp.x += deltaX;
						tmp.y += deltaY;

						if( tmp.isValid() )
							checkNeighbors(x,y,tmp, flowCurrLayer,score);
						else
							flowCurrLayer.unsafe_get(x,y).markInvalid();
					}
				}
			}

			// swap the flow images
			ImageFlow tmp = flowPrevLayer;
			flowPrevLayer = flowCurrLayer;
			flowCurrLayer = tmp;
		}
	}

	/**
	 * Performs an exhaustive search centered around (cx,cy) for the region in 'curr' which is the best
	 * match for the template.  Results are written into 'flow'
	 */
	protected float findFlow( int cx , int cy , T curr , ImageFlow.D flow ) {
		float bestScore = Float.MAX_VALUE;
		int bestFlowX=0,bestFlowY=0;

		// ensure the search region is contained entirely inside the image
		int startY = cy-searchRadius-regionRadius < 0 ? Math.max(regionRadius - cy, 0) : -searchRadius;
		int startX = cx-searchRadius-regionRadius < 0 ? Math.max(regionRadius-cx,0) : -searchRadius;
		int endY = cy+searchRadius+regionRadius >= curr.height ? curr.height-cy-regionRadius-1 : searchRadius;
		int endX = cx+searchRadius+regionRadius >= curr.width ? curr.width-cx-regionRadius-1 : searchRadius;

		// search around the template's center
		for( int i = startY; i <= endY; i++ ) {
			int y = cy+i;
			for( int j = startX; j <= endX; j++ ) {
				int x = cx+j;
				float error = computeError(x,y,curr);
				if( error < bestScore ) {
					bestScore = error;
					bestFlowX = j;
					bestFlowY = i;
				} else if ( error == bestScore ) {
					// Pick solution with the least motion when ambiguous
					float m0 = j*j + i*i;
					float m1 = bestFlowX*bestFlowX + bestFlowY*bestFlowY;
					if( m0 < m1 ) {
						bestFlowX = j;
						bestFlowY = i;
					}
				}
			}
		}

		if( bestScore <= maxError ) {
			flow.x = bestFlowX;
			flow.y = bestFlowY;
			return bestScore;
		} else {
			flow.markInvalid();
			return Float.NaN;
		}
	}

	/**
	 * Examines every pixel inside the region centered at (cx,cy) to see if their optical flow has a worse
	 * score the one specified in 'flow'
	 */
	protected void checkNeighbors( int cx , int cy , ImageFlow.D flow , ImageFlow image , float score ) {
		for( int i = -regionRadius; i <= regionRadius; i++ ) {
			int index = image.width*(cy+i) + (cx-regionRadius);
			for( int j = -regionRadius; j <= regionRadius; j++ , index++ ) {
				float s = scores[ index ];
				ImageFlow.D f = image.data[index];
				if( s > score ) {
					f.set(flow);
					scores[index] = score;
				} else if( s == score ) {
					// Pick solution with the least motion when ambiguous
					float m0 = f.x*f.x + f.y*f.y;
					float m1 = flow.x*flow.x + flow.y*flow.y;
					if( m1 < m0 ) {
						f.set(flow);
						scores[index] = score;
					}
				}
			}
		}
	}

	/**
	 * Extracts a square template from the image 'prev' center at cx and cy
	 */
	protected abstract void extractTemplate( int cx , int cy , T prev );

	/**
	 * Computes the error between the template and a region in 'curr' centered at cx,cy
	 */
	protected abstract float computeError( int cx , int cy , T curr );

	/**
	 * Returns the found optical flow
	 */
	public ImageFlow getOpticalFlow() {
		return flowPrevLayer;
	}

	/**
	 * Implementation for {@link GrayU8}
	 */
	public static class U8 extends DenseOpticalFlowBlockPyramid<GrayU8>
	{
		public U8(int searchRadius, int regionRadius, int maxPerPixelError) {
			super(searchRadius, regionRadius, maxPerPixelError,GrayU8.class);
		}

		@Override
		protected void extractTemplate( int cx , int cy , GrayU8 prev ) {
			int index = 0;
			for( int i = -regionRadius; i <= regionRadius; i++ ) {
				int indexPrev = prev.startIndex + prev.stride*(i+cy) + cx-regionRadius;
				for( int j = -regionRadius; j <= regionRadius; j++ ) {
					template.data[index++] = prev.data[indexPrev++];
				}
			}
		}

		@Override
		protected float computeError( int cx , int cy , GrayU8 curr ) {
			int index = 0;
			int error = 0;
			for( int i = -regionRadius; i <= regionRadius; i++ ) {
				int indexPrev = curr.startIndex + curr.stride*(i+cy) + cx-regionRadius;
				for( int j = -regionRadius; j <= regionRadius; j++ ) {
					int e = (template.data[index++]&0xFF) - (curr.data[indexPrev++]&0xFF);
					error += e < 0 ? -e : e;
				}
			}

			return error;
		}
	}

	/**
	 * Implementation for {@link GrayF32}
	 */
	public static class F32 extends DenseOpticalFlowBlockPyramid<GrayF32>
	{
		public F32(int searchRadius, int regionRadius, int maxPerPixelError) {
			super(searchRadius, regionRadius, maxPerPixelError,GrayF32.class);
		}

		@Override
		protected void extractTemplate( int cx , int cy , GrayF32 prev ) {
			int index = 0;
			for( int i = -regionRadius; i <= regionRadius; i++ ) {
				int indexPrev = prev.startIndex + prev.stride*(i+cy) + cx-regionRadius;
				for( int j = -regionRadius; j <= regionRadius; j++ ) {
					template.data[index++] = prev.data[indexPrev++];
				}
			}
		}

		@Override
		protected float computeError( int cx , int cy , GrayF32 curr ) {
			int index = 0;
			float error = 0;
			for( int i = -regionRadius; i <= regionRadius; i++ ) {
				int indexPrev = curr.startIndex + curr.stride*(i+cy) + cx-regionRadius;
				for( int j = -regionRadius; j <= regionRadius; j++ ) {
					float e = template.data[index++] - curr.data[indexPrev++];
					error += e < 0 ? -e : e;
				}
			}

			return error;
		}
	}

	public int getSearchRadius() {
		return searchRadius;
	}

	public int getRegionRadius() {
		return regionRadius;
	}
}
