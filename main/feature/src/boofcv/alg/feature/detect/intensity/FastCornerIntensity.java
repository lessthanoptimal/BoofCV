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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.impl.FastHelper;
import boofcv.misc.DiscretizedCircle;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Generic interface for fast corner detection algorithms. The general idea is that at the points in a circle around
 * the center point (see below) should either be above or below the center pixel's intensity value value. With
 * this information candidates corners can be quickly eliminated, see [1].
 * <p/>
 *
 * <p>
 * This implementation works by trying to minimize the number of reads per pixel.  Code is auto generated and samples
 * each point in a series of if statements such that the number of possible candidate corners around a pixel are
 * eliminated.  A different auto generated implementation is provided for Fast 9 to Fast 12. The number indicates how
 * many continuous pixels are needed for it to be considered a corner.
 * </p>
 *
 * <p>
 * After a pixel is flagged as a corner then the the intensity the difference between the average
 * exterior pixel value which is part of the corner and the center pixel value.  See code for details.
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
 * @see FastHelper
 *
 * <p>
 * [1] Edward Rosten, Reid Porter and Tom Drummond. "Faster and better: a machine learning approach to corner detection"
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class FastCornerIntensity<T extends ImageGray> implements FeatureIntensity<T> {

	// radius of the circle being sampled
	protected static final int radius = 3;

	// pixel index offsets for the circle
	protected int []offsets;
	// the image's stride.  Used to determine if the offsets need to be recomputed
	private int stride = 0;

	// list of pixels that might be corners.
	private QueueCorner candidates = new QueueCorner(10);

	// reference to the input image
	protected T image;

	// Used to sample the image and compute the score
	protected FastHelper<T> helper;

	/**
	 * Constructor
	 *
	 * @param helper Provide the image type specific helper.
	 */
	protected FastCornerIntensity(FastHelper<T> helper) {
		this.helper = helper;
	}

	public QueueCorner getCandidates() {
		return candidates;
	}

	@Override
	public int getRadius() {
		return radius;
	}

	@Override
	public int getIgnoreBorder() {
		return radius;
	}

	public void process( T image , GrayF32 intensity ) {
		candidates.reset();
		this.image = image;

		if( stride != image.stride ) {
			stride = image.stride;
			offsets = DiscretizedCircle.imageOffsets(radius, image.stride);
		}
		helper.setImage(image,offsets);

		for (int y = radius; y < image.height-radius; y++) {
			int indexIntensity = intensity.startIndex + y*intensity.stride + radius;
			int index = image.startIndex + y*image.stride + radius;
			for (int x = radius; x < image.width-radius; x++, index++,indexIntensity++) {

				helper.setThresholds(index);

				if( checkLower(index) ) {
					intensity.data[indexIntensity] = helper.scoreLower(index);
					candidates.add(x,y);
				} else if( checkUpper(index)) {
					intensity.data[indexIntensity] = helper.scoreUpper(index);
					candidates.add(x,y);
				} else {
					intensity.data[indexIntensity] = 0;
				}
			}
		}
	}

	/**
	 * Checks to see if the specified pixel qualifies as a corner with lower values
	 */
	protected abstract boolean checkLower( int index );

	/**
	 * Checks to see if the specified pixel qualifies as a corner with upper values
	 */
	protected abstract boolean checkUpper( int index );
}
