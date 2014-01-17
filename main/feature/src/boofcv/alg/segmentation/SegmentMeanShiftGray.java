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

package boofcv.alg.segmentation;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.weights.WeightDistance_F32;
import boofcv.alg.weights.WeightPixel_F32;
import boofcv.struct.image.ImageSingleBand;
import org.ddogleg.struct.FastQueue;

/**
 * <p>
 * Implementation of {@link SegmentMeanShift} for gray-scale images
 * </p>
 *
 * @author Peter Abeles
 */
public class SegmentMeanShiftGray<T extends ImageSingleBand> extends SegmentMeanShift<T> {

	// Interpolation routine used to get sub-pixel samples
	protected InterpolatePixelS<T> interpolate;

	protected float meanGray;

	/**
	 * Configures mean-shift segmentation
	 *
	 * @param maxIterations Maximum number of mean-shift iterations.  Try 30
	 * @param convergenceTol When the change is less than this amount stop.  Try 0.005
	 * @param interpolate Function used to interpolate the input image
	 * @param weightSpacial Weighting function/kernel for spacial component
	 * @param weightGray Weighting function/kernel for distance of color component
	 */
	public SegmentMeanShiftGray(int maxIterations, float convergenceTol,
								InterpolatePixelS<T> interpolate,
								WeightPixel_F32 weightSpacial,
								WeightDistance_F32 weightGray) {
		super(maxIterations,convergenceTol,weightSpacial,weightGray);
		this.interpolate = interpolate;

		modeColor = new FastQueue<float[]>(float[].class,true) {
			@Override
			protected float[] createInstance() {
				return new float[ 1 ];
			}
		};
	}

	/**
	 * Performs mean-shift clustering on the input image
	 *
	 * @param image Input image
	 */
	@Override
	public void process( T image ) {
		// initialize data structures
		this.image = image;

		modeLocation.reset();
		modeColor.reset();
		modeMemberCount.reset();

		interpolate.setImage(image);

		peakToIndex.reshape(image.width,image.height);
		ImageMiscOps.fill(peakToIndex,-1);

		// use mean shift to find the peak of each pixel in the image
		for( int y = 0; y < image.height; y++ ) {
			for( int x = 0; x < image.width; x++ ) {
				float pixelColor = interpolate.get(x, y);
				findPeak(x,y,pixelColor);

				// convert mean-shift location into pixel index
				int peakLocation = (int)(meanY+0.5f)*image.width + (int)(meanX+0.5f);

				// get index in the list of peaks
				int peakIndex = peakToIndex.data[peakLocation];
				if( peakIndex < 0 ) {
					peakIndex = this.modeLocation.getSize();
					this.modeLocation.add(peakLocation);
					// Save the peak's color
					modeColor.grow()[0] = meanGray;
					// Remember it's location in the peak arrays
					peakToIndex.data[peakLocation] = peakIndex;
					// Set the initial count to zero.  When the peak itself is processed later it will increment it
					modeMemberCount.add(0);
				}
				// Remember where this was a peak to
				modeMemberCount.data[peakIndex]++;
				peakToIndex.unsafe_set(x,y,peakIndex);
			}
		}
	}

	/**
	 * Uses mean-shift to find the peak.  Returns the peak as an index in the image data array.
	 *
	 * @param gray The color value which mean-shift is trying to find a region which minimises it
	 */
	protected void findPeak( float cx , float cy , float gray ) {

		for( int i = 0; i < maxIterations; i++ ) {
			float total = 0;
			float sumX = 0, sumY = 0, sumGray = 0;

			int kernelIndex = 0;

			float x0 = cx - radiusX;
			float y0 = cy - radiusY;

			// If it is not near the image border it can use faster techniques
			if( interpolate.isInFastBounds(x0, y0) &&
					interpolate.isInFastBounds(x0 + widthX - 1, y0 + widthY - 1)) {
				for( int yy = 0; yy < widthY; yy++ ) {
					for( int xx = 0; xx < widthX; xx++ ) {
						float ws = weightSpacial.weightIndex(kernelIndex++);
						// compute distance between gray scale value as euclidean squared
						float pixelGray = interpolate.get_fast(x0 + xx, y0 + yy);
						float d = pixelGray - gray;
						float wg = weightColor.weight(d*d);
						// Total weight is the combination of spacial and color values
						float weight = ws*wg;
						total += weight;
						sumX += weight*(xx+x0);
						sumY += weight*(yy+y0);
						sumGray += weight*pixelGray;
					}
				}
			} else {
				// Perform more sanity checks here for the image edge.  Edge pixels are handled by skipping them
				for( int yy = 0; yy < widthY; yy++ ) {
					float sampleY = y0+yy;
					// make sure it is inside the image
					if( sampleY < 0 ) {
						kernelIndex += widthX;
						continue;
					} else if( sampleY > image.height-1) {
						break;
					}
					for( int xx = 0; xx < widthX; xx++ , kernelIndex++) {
						float sampleX = x0+xx;

						// make sure it is inside the image
						if( sampleX < 0 ||  sampleX > image.width-1 ) {
							continue;
						}

						float ws = weightSpacial.weightIndex(kernelIndex);
						float pixelGray = interpolate.get(sampleX, sampleY);
						float d = pixelGray - gray;
						float wg = weightColor.weight(d*d);
						float weight = ws*wg;
						total += weight;
						sumX += weight*(xx+x0);
						sumY += weight*(yy+y0);
						sumGray += weight*pixelGray;
					}
				}
			}

			if( total == 0 )
				break;

			float peakX = sumX/total;
			float peakY = sumY/total;

			float dx = peakX-cx;
			float dy = peakY-cy;

			cx = peakX; cy = peakY;
			gray = sumGray/total;

			if( Math.abs(dx) < convergenceTol && Math.abs(dy) < convergenceTol ) {
				break;
			}
		}

		this.meanX = cx;
		this.meanY = cy;
		this.meanGray = gray;
	}
}
