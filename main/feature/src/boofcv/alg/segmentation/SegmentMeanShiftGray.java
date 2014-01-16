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
import boofcv.alg.weights.WeightDistance_F32;
import boofcv.alg.weights.WeightPixel_F32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Performs mean-shift segmentation on a gray scale image [1].  Segmentation is done by finding the
 * mean-shift local peak for each pixel in the image.  The gray/color value which mean-shift is minimizing is the
 * pixel's intensity value.
 * </p>
 * <p>
 * Output is provided in the form of an image where each pixel contains the index of a region the pixel belongs too.
 * Three other lists provide color value of the region, number of pixels in the region and the location
 * of the mean-shift peak for that region.
 * </p>
 *
 * <p>
 * <ul>
 * <li>The kernel's spacial radius is specified by the radius of 'weightSpacial' and the gray/color radius is specified
 * by 'weightGray'.  Those two functions also specified the amount of weight assigned to each sample in the
 * mean-shift kernel based on its distance from the spacial and color means.<li>
 * <li>The distance passed into the 'weightGray' function is the difference squared.E.g. (a-b)<sup>2</sup></li>
 * <li>Image edges are handled by truncating the spacial kernel.  This truncation
 * will create an asymmetric kernel, but there is really no good way to handle image edges.</li>
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
	protected int radiusX,radiusY;
	protected int widthX,widthY;
	// Sample weight given location relative to center
	protected WeightPixel_F32 weightSpacial;
	// Sample weight given difference in gray scale value
	protected WeightDistance_F32 weightGray;

	// converts an index of a peak into an index in the pixel image
	protected ImageSInt32 peakToIndex = new ImageSInt32(1,1);

	// location of each peak in image pixel indexes
	protected GrowQueue_I32 peakLocation = new GrowQueue_I32();

	// intensity value of each peak
	protected GrowQueue_F32 peakValue = new GrowQueue_F32();

	// number of members in this peak
	protected GrowQueue_I32 peakMemberCount = new GrowQueue_I32();

	// The input image
	protected T image;

	protected float meanX,meanY,meanGray;

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

		this.radiusX = weightSpacial.getRadiusX();
		this.radiusY = weightSpacial.getRadiusY();
		this.widthX = radiusX*2+1;
		this.widthY = radiusY*2+1;
	}

	/**
	 * Performs mean-shift clustering on the input image
	 *
	 * @param image Input image
	 */
	public void process( T image ) {
		// initialize data structures
		this.image = image;

		peakLocation.reset();
		peakValue.reset();
		peakMemberCount.reset();

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
					peakIndex = this.peakLocation.getSize();
					this.peakLocation.add(peakLocation);
					// Save the peak's color
					peakValue.add( meanGray );
					// Remember it's location in the peak arrays
					peakToIndex.data[peakLocation] = peakIndex;
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
	 *
	 * @param gray The color value which mean-shift is trying to find a region which minimises it
	 */
	protected void findPeak( float cx , float cy , float gray ) {

		int i;
		for( i = 0; i < maxIterations; i++ ) {
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
						float wg = weightGray.weight(d*d);
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
