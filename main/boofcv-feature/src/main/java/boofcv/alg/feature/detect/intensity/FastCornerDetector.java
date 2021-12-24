/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.impl.FastCornerInterface;
import boofcv.misc.DiscretizedCircle;
import boofcv.struct.ListIntPoint2D;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import lombok.Getter;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * <p>
 * Generic interface for fast corner detection algorithms. The general idea is that at the points in a circle around
 * the center point (see below) should either be above or below the center pixel's intensity value value. With
 * this information candidates corners can be quickly eliminated, see [1].
 * <p/>
 *
 * <p>
 * This implementation works by trying to minimize the number of reads per pixel. Code is auto generated and samples
 * each point in a series of if statements such that the number of possible candidate corners around a pixel are
 * eliminated. A different auto generated implementation is provided for Fast 9 to Fast 12. The number indicates how
 * many continuous pixels are needed for it to be considered a corner.
 * </p>
 *
 * <p>
 * After a pixel is flagged as a corner then the the intensity the difference between the average
 * exterior pixel value which is part of the corner and the center pixel value. See code for details.
 * </p>
 *
 * <p>
 * Circle of radius 2 pixels is searched around the center point 'x':
 * <table border="1">
 * <tr> <td></td> <td></td> <td>12</td><td>13</td><td>14</td> <td></td><td></td> </tr>
 * <tr> <td></td> <td>11</td> <td></td><td></td><td></td> <td>15</td><td></td> </tr>
 * <tr> <td>10</td> <td></td></td> <td></td><td></td><td></td> <td></td><td>16</td></tr>
 * <tr> <td>09</td> <td></td></td> <td></td><td><center>x</center></td><td></td> <td></td><td>01</td></tr>
 * <tr> <td>08</td> <td></td></td> <td></td><td></td><td></td> <td></td><td>02</td></tr>
 * <tr> <td></td> <td>07</td> <td></td><td></td><td></td> <td>03</td><td></td> </tr>
 * <tr> <td></td> <td></td> <td>06</td><td>05</td><td>04</td> <td></td><td></td> </tr>
 * </table>
 * </p>
 *
 * @author Peter Abeles
 * @see FastCornerInterface
 *
 * <p>
 * [1] Edward Rosten, Reid Porter and Tom Drummond. "Faster and better: a machine learning approach to corner detection"
 * </p>
 */
@SuppressWarnings({"NullAway.Init"})
public class FastCornerDetector<T extends ImageGray<T>> implements FeatureIntensity<T> {

	// radius of the circle being sampled
	protected static final int radius = 3;

	// pixel index offsets for the circle
	protected int[] offsets;
	// the image's stride. Used to determine if the offsets need to be recomputed
	int stride = 0;

	// list of pixels that might be corners.
	final @Getter ListIntPoint2D candidatesLow = new ListIntPoint2D();
	final @Getter ListIntPoint2D candidatesHigh = new ListIntPoint2D();

	// reference to the input image
	protected T image;

	// Used to sample the image and compute the score
	protected FastCornerInterface<T> helper;

	/**
	 * Constructor
	 *
	 * @param helper Provide the image type specific helper.
	 */
	public FastCornerDetector( FastCornerInterface<T> helper ) {
		this.helper = helper;
	}

	FastCornerDetector() {}

	@Override public int getRadius() {return radius;}

	@Override public int getIgnoreBorder() {return radius;}

	/**
	 * Computes fast corner features and their intensity. The intensity is needed if non-max suppression is
	 * used
	 */
	public void process( T image, GrayF32 intensity ) {
		candidatesLow.configure(image.width, image.height);
		candidatesHigh.configure(image.width, image.height);

		this.image = image;

		if (stride != image.stride) {
			stride = image.stride;
			offsets = DiscretizedCircle.imageOffsets(radius, image.stride);
		}
		helper.setImage(image, offsets);

		for (int y = radius; y < image.height - radius; y++) {
			int indexIntensity = intensity.startIndex + y*intensity.stride + radius;
			int index = image.startIndex + y*image.stride + radius;
			for (int x = radius; x < image.width - radius; x++, index++, indexIntensity++) {

				int result = helper.checkPixel(index);

				if (result < 0) {
					intensity.data[indexIntensity] = helper.scoreLower(index);
					candidatesLow.add(x, y);
				} else if (result > 0) {
					intensity.data[indexIntensity] = helper.scoreUpper(index);
					candidatesHigh.add(x, y);
				} else {
					intensity.data[indexIntensity] = 0;
				}
			}
		}
	}

	/**
	 * Computes fast corner features
	 */
	public void process( T image ) {
		candidatesLow.configure(image.width, image.height);
		candidatesHigh.configure(image.width, image.height);

		this.image = image;

		if (stride != image.stride) {
			stride = image.stride;
			offsets = DiscretizedCircle.imageOffsets(radius, image.stride);
		}
		helper.setImage(image, offsets);

		for (int y = radius; y < image.height - radius; y++) {
			int index = image.startIndex + y*image.stride + radius;
			for (int x = radius; x < image.width - radius; x++, index++) {

				int result = helper.checkPixel(index);

				if (result < 0) {
					candidatesLow.add(x, y);
				} else if (result > 0) {
					candidatesHigh.add(x, y);
				}
			}
		}
	}

	public Class<T> getImageType() {
		return helper.getImageType();
	}
}
