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

package boofcv.alg.segmentation;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.weights.WeightDistance_F32;
import boofcv.struct.weights.WeightPixel_F32;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Performs mean-shift segmentation on a gray scale image.  Segmentation is performed by finding the mean-shift
 * local peak for each pixel in the image.  The output is an image with the index of each peak in each pixel and
 * lists which contain the location of a peak, the pixel value at the peak, and the number of pixels which
 * have that pixel as a peak. Inspired by [1].
 * </p>
 *
 * <p>
 * Weight/distance measure is done independently for spacial and color components, allowing for better tuning
 * considering the difference in units.  Image edges are handled by truncating the spacial kernel.  This truncation
 * will create an asymmetric kernel, but there is really no good way to handle image edges.
 * </p>
 *
 * <p>
 * [1] Cheng, Yizong. "Mean shift, mode seeking, and clustering." Pattern Analysis and Machine Intelligence,
 * IEEE Transactions on 17.8 (1995): 790-799.
 * </p>
 *
 * @author Peter Abeles
 */
public class SegmentMeanShiftGray<T extends ImageSingleBand> {

	// Interpolation routine used to get sub-pixel samples
	protected InterpolatePixelS<T> interpolate;

	// used to detect convergence of mean-shift
	protected int maxIterations;
	protected float convergenceTol;

	// specifies the size of the mean-shift kernel
	protected int radius;
	protected int width;
	// Sample weight given location relative to center
	protected WeightPixel_F32 weightSpacial;
	// Sample weight given difference in gray scale value
	protected WeightDistance_F32 weightGray;

	// converts an index of a peak into an index in the pixel image
	protected ImageSInt32 peakToIndex = new ImageSInt32(1,1);

	// location of each peak in image pixel indexes
	protected GrowQueue_I32 peakLocation;

	// intensity value of each peak
	protected GrowQueue_F32 peakValue;

	// number of members in this peak
	protected GrowQueue_I32 peakMemberCount;

	// The input image
	private T image;

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
		this.maxIterations = maxIterations;
		this.convergenceTol = convergenceTol;
		this.interpolate = interpolate;
		this.weightSpacial = weightSpacial;
		this.weightGray = weightGray;
	}

	/**
	 * Specifies the size of the spacial kernel
	 *
	 * @param radius Size of spacial kernel
	 */
	public void setRadius( int radius ) {
		weightSpacial.setRadius(radius);
		this.radius = radius;
		this.width = radius*2+1;
	}

	/**
	 * Performs mean-shift clustering on the input image
	 *
	 * @param image Input image
	 */
	public void process( T image ) {
		// initialize data structures
		this.image = image;

		interpolate.setImage(image);

		peakToIndex.reshape(image.width,image.height);
		ImageMiscOps.fill(peakToIndex,-1);

		// use mean shift to find the peak of each pixel in the image
		for( int y = radius; y < image.height-radius; y++ ) {
			for( int x = radius; x < image.width-radius; x++ ) {
				float localGray = computeGray(x,y);
				int foundPeak = findPeak(x,y,localGray);

				int peakIndex = peakToIndex.data[foundPeak];
				if( peakIndex < 0 ) {
					peakIndex = peakLocation.getSize();
					peakLocation.add( foundPeak );
					// Save the peak's color
					peakValue.add( interpolate.get(x,y) );
					// Remember it's location in the peak arrays
					peakToIndex.data[foundPeak] = peakIndex;
					// Set the initial count to zero.  When the peak itself is processed later it will increment it
					peakMemberCount.add(0);
				}
				// Remember where this was a peak to
				peakMemberCount.data[peakIndex]++;
				peakToIndex.unsafe_set(x,y,peakIndex);
			}
		}
	}

	/**
	 * Uses mean-shift to find the peak.  Returns the peak as an index in the image data array.
	 */
	protected int findPeak( float cx , float cy , float gray ) {

		float prevX = cx;
		float prevY = cy;

		float meanGray = gray;

		for( int i = 0; i < maxIterations; i++ ) {
			float total = 0;
			float sumX = 0, sumY = 0, sumGray = 0;

			int kernelIndex = 0;

			float x0 = cx - radius;
			float y0 = cy - radius;

			// If it is not near the image border it can use faster techniques
			if( interpolate.isInFastBounds(x0, y0) &&
					interpolate.isInFastBounds(x0 + width - 1, y0 + width - 1)) {
				for( int yy = 0; yy < width; yy++ ) {
					for( int xx = 0; xx < width; xx++ ) {
						float ws = weightSpacial.weightIndex(kernelIndex++);
						// compute distance between gray scale value as euclidean squared
						float pixelGray = interpolate.get_fast(x0 + xx, y0 + yy);
						float d = pixelGray - meanGray;
						float wg = weightGray.weight(d*d);
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
				for( int yy = 0; yy < width; yy++ ) {
					float sampleY = y0+yy;
					// make sure it is inside the image
					if( sampleY < 0 ) {
						kernelIndex += width;
						continue;
					} else if( sampleY > image.height-1) {
						break;
					}
					for( int xx = 0; xx < width; xx++ , kernelIndex++) {
						float sampleX = x0+xx;

						// make sure it is inside the image
						if( sampleX < 0 ||  sampleX > image.width-1 ) {
							continue;
						}

						float ws = weightSpacial.weightIndex(kernelIndex);
						float pixelGray = interpolate.get_fast(sampleX, sampleY);
						float d = pixelGray - meanGray;
						float wg = weightGray.weight(d*d);
						float weight = ws*wg;
						total += weight;
						sumX += weight*(xx+x0);
						sumY += weight*(yy+y0);
						sumGray += weight*pixelGray;
					}
				}
			}

			cx = sumX/total;
			cy = sumY/total;
			meanGray = sumGray/total;


			float dx = cx-prevX;
			float dy = cy-prevY;

			prevX = cx; prevY = cy;

			if( Math.abs(dx) < convergenceTol && Math.abs(dy) < convergenceTol ) {
				break;
			}
		}

		// round to the nearest pixel
		int pixelX = (int)(cx+0.5f);
		int pixelY = (int)(cy+0.5f);

		// return the pixel index
		return pixelY*image.width + pixelX;
	}

	/**
	 * Compute the gray value of the region using the 2D spacial kernel
	 */
	protected float computeGray( int cx , int cy ) {

		float sumGray = 0;

		int x0 = cx - radius;
		int y0 = cy - radius;

		float total = 0;
		int kernelIndex = 0;
		for( int yy = 0; yy < width; yy++ ) {
			float sampleY = y0+yy;
			if( sampleY < 0 ||sampleY > image.height-1 ) {
				kernelIndex += width;
				continue;
			}
			for( int xx = 0; xx < width; xx++ , kernelIndex++ ) {
				float sampleX = x0+xx;
				if( sampleX < 0 ||sampleX > image.width-1 ) {
					continue;
				}

				float ws = weightSpacial.weightIndex(kernelIndex);
				total += ws;
				sumGray += ws*interpolate.get(sampleX, sampleY);
			}
		}

		return sumGray/total;
	}

	/**
	 * From peak index to pixel index
	 */
	public ImageSInt32 getPeakToIndex() {
		return peakToIndex;
	}

	/**
	 * Location of each peak in the image
	 */
	public GrowQueue_I32 getPeakLocation() {
		return peakLocation;
	}

	/**
	 * Pixel value of each peak
	 */
	public GrowQueue_F32 getPeakValue() {
		return peakValue;
	}

	/**
	 * Number of pixels which each peak as a member
	 */
	public GrowQueue_I32 getPeakMemberCount() {
		return peakMemberCount;
	}
}
