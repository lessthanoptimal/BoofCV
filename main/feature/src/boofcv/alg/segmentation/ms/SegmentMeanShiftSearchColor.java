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

package boofcv.alg.segmentation.ms;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.feature.ColorQueue_F32;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.util.Arrays;

/**
 * <p>
 * Implementation of {@link SegmentMeanShiftSearch} for color images
 * </p>
 *
 * @author Peter Abeles
 */
public class SegmentMeanShiftSearchColor<T extends ImageMultiBand> extends SegmentMeanShiftSearch<T> {

	// Interpolation routine used to get sub-pixel samples
	protected InterpolatePixelMB<T> interpolate;

	// storage for interpolated pixel value
	protected float[] pixelColor;
	protected float[] meanColor;
	protected float[] sumColor;

	// Mean-shift trajectory history
	protected FastQueue<Point2D_F32> history = new FastQueue<>(Point2D_F32.class, true);

	ImageType<T> imageType;

	public SegmentMeanShiftSearchColor(int maxIterations, float convergenceTol,
									   InterpolatePixelMB<T> interpolate,
									   int radiusX , int radiusY , float maxColorDistance ,
									   boolean fast,
									   ImageType<T> imageType) {
		super(maxIterations,convergenceTol,radiusX,radiusY,maxColorDistance,fast);
		this.interpolate = interpolate;
		this.pixelColor = new float[ imageType.getNumBands() ];
		this.meanColor = new float[ imageType.getNumBands() ];
		this.sumColor = new float[ imageType.getNumBands() ];
		this.imageType = imageType;

		final int numBands = imageType.getNumBands();

		modeColor = new ColorQueue_F32(numBands);
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

		pixelToMode.reshape(image.width, image.height);
		quickMode.reshape(image.width, image.height);
		// mark as -1 so it knows which pixels have been assigned a mode already and can skip them
		ImageMiscOps.fill(pixelToMode, -1);
		// mark all pixels are not being a mode
		ImageMiscOps.fill(quickMode,-1);

		// use mean shift to find the peak of each pixel in the image
		int indexImg = 0;
		for( int y = 0; y < image.height; y++ ) {
			for( int x = 0; x < image.width; x++ , indexImg++ ) {
				if( pixelToMode.data[indexImg] != -1 ) {
					int peakIndex = pixelToMode.data[indexImg];
					modeMemberCount.data[peakIndex]++;
					continue;
				}

				interpolate.get(x, y, meanColor);
				findPeak(x,y, meanColor);

				// convert mean-shift location into pixel index
				int modeX = (int)(this.modeX +0.5f);
				int modeY = (int)(this.modeY +0.5f);

				int modePixelIndex = modeY*image.width + modeX;

				// get index in the list of peaks
				int modeIndex = quickMode.data[modePixelIndex];
				// If the mode is new add it to the list
				if( modeIndex < 0 ) {
					modeIndex = this.modeLocation.size();
					this.modeLocation.grow().set(modeX, modeY);
					// Save the peak's color
					savePeakColor(meanColor);
					// Mark the mode in the segment image
					quickMode.data[modePixelIndex] = modeIndex;
					// Set the initial count to zero. This will be incremented when it is traversed later on
					modeMemberCount.add(0);
				}

				// add this pixel to the membership list
				modeMemberCount.data[modeIndex]++;

				// Add all pixels it traversed through to the membership of this mode
				// This is an approximate of mean-shift
				for( int i = 0; i < history.size; i++ ) {
					Point2D_F32 p = history.get(i);
					int px = (int)(p.x+0.5f);
					int py = (int)(p.y+0.5f);

					int index = pixelToMode.getIndex(px,py);
					if( pixelToMode.data[index] == -1 ) {
						pixelToMode.data[index] = modeIndex;
					}
				}
			}
		}
	}

	@Override
	public ImageType<T> getImageType() {
		return imageType;
	}

	/**
	 * Uses mean-shift to find the peak.  Returns the peak as an index in the image data array.
	 *
	 * @param meanColor The color value which mean-shift is trying to find a region which minimises it
	 */
	protected void findPeak( float cx , float cy , float[] meanColor ) {

		history.reset();
		history.grow().set(cx,cy);

		for( int i = 0; i < maxIterations; i++ ) {
			float total = 0;
			float sumX = 0, sumY = 0;

			Arrays.fill(sumColor,0);

			int kernelIndex = 0;

			float x0 = cx - radiusX;
			float y0 = cy - radiusY;

			// If it is not near the image border it can use faster techniques
			if( interpolate.isInFastBounds(x0, y0) &&
					interpolate.isInFastBounds(x0 + widthX - 1, y0 + widthY - 1)) {
				for( int yy = 0; yy < widthY; yy++ ) {
					for( int xx = 0; xx < widthX; xx++ ) {
						float ds = spacialTable[kernelIndex++];
						interpolate.get(x0 + xx, y0 + yy, pixelColor);
						float dc = distanceSq(pixelColor,meanColor)/ maxColorDistanceSq;
						float weight = dc > 1 ? 0 : weight((ds+dc)/2f);
						total += weight;
						sumX += weight*(xx+x0);
						sumY += weight*(yy+y0);
						sumColor(sumColor, pixelColor,weight);
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

						float ds = spacialTable[kernelIndex];
						interpolate.get(x0 + xx, y0 + yy, pixelColor);
						float dc = distanceSq(pixelColor,meanColor)/ maxColorDistanceSq;
						float weight = dc > 1 ? 0 : weight((ds+dc)/2f);
						total += weight;
						sumX += weight*(xx+x0);
						sumY += weight*(yy+y0);
						sumColor(sumColor, pixelColor,weight);
					}
				}
			}

			if( total == 0 )
				break;

			float peakX = sumX/total;
			float peakY = sumY/total;

			if( fast ) {
				history.grow().set(peakX,peakY);

				// see if it has already been here before
				int px = (int)(peakX+0.5f);
				int py = (int)(peakY+0.5f);

				int index = pixelToMode.getIndex(px,py);
				int modeIndex = pixelToMode.data[index];
				if( modeIndex != -1 ) {
					// it already knows the solution so stop searching
					Point2D_I32 modeP = modeLocation.get(modeIndex);
					this.modeX = modeP.x;
					this.modeY = modeP.y;
					return;
				}
			}

			// move on to the next iteration
			float dx = peakX-cx;
			float dy = peakY-cy;

			cx = peakX; cy = peakY;
			meanColor(sumColor,meanColor,total);

			if( Math.abs(dx) < convergenceTol && Math.abs(dy) < convergenceTol ) {
				break;
			}
		}

		this.modeX = cx;
		this.modeY = cy;
	}

	protected static void meanColor( float[] sum, float[] mean , float total ) {
		for( int i = 0; i < sum.length; i++ ) {
			mean[i] = sum[i]/total;
		}
	}

	protected static void sumColor( float[] sum, float[] pixel , float weight ) {
		for( int i = 0; i < sum.length; i++ ) {
			sum[i] += pixel[i]*weight;
		}
	}

	protected void savePeakColor( float[] a ) {
		float[] b = modeColor.grow();
		for( int i = 0; i < a.length; i++ ) {
			b[i] = a[i];
		}
	}
}
