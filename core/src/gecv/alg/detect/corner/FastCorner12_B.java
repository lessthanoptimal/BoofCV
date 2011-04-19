/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.corner;

import gecv.misc.DiscretizedCircle;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt8;

/**
 * An implementation of {@link FastCorner12_B} algorithm that is designed to be
 * more easily read and verified for correctness.  The price is some computations are done
 * that might not always be necissary.
 * <p/>
 * In this implementation it is assumed that valid pixels must form a chain of at least
 * 12 continuous pixels
 * <p/>
 * See the paper: "Faster and better: a machine learning approach to corner detection" by
 * Edward Rosten, Reid Porter, and Tom Drummond
 * <p/>
 * A global threshold is used to select which features are used
 *
 * @author Peter Abeles
 */
public class FastCorner12_B {

	private int minCont;
	private final static int radius = 3;

	private ImageInt8 img;
	// how similar do the pixel in the circle need to be to the center pixel
	private int pixelTol;

	// relative offsets of pixel locations in a circle
	private int[] offsets;

	// the intensity of the found features in the image
	// this could be slightly speed up by using an int32 here instead
	// but it would be less generic...
	private ImageFloat32 featureIntensity;

	/**
	 * Constructor
	 *
	 * @param img	  The image where features are extracted from.
	 * @param pixelTol The difference in intensity value from the center pixel the circle needs to be.
	 * @param minCont  The minimum number of continuous pixels that a circle needs to be a corner.
	 */
	public FastCorner12_B(ImageInt8 img,
						  int pixelTol, int minCont) {
		this.img = img;
		this.pixelTol = pixelTol;
		this.minCont = minCont;

		offsets = DiscretizedCircle.imageOffsets(radius, img.getWidth());

		featureIntensity = new ImageFloat32(img.getWidth(), img.getHeight());
	}

	public ImageFloat32 getIntensity() {
		return featureIntensity;
	}

	public void process() {
		final byte[] data = img.data;

		final int width = img.getWidth();
		final int yEnd = img.getHeight() - radius;

		final float[] inten = featureIntensity.data;

		int offA = offsets[0];
		int offB = offsets[4];
		int offC = offsets[8];
		int offD = offsets[12];

		for (int y = radius; y < yEnd; y++) {
			int rowStart = width * y;
			int endX = rowStart + width - radius;

			for (int index = rowStart + radius; index < endX; index++) {

				int center = data[index] & 0xFF;

				int a = data[index + offA] & 0xFF;
				int b = data[index + offB] & 0xFF;
				int c = data[index + offC] & 0xFF;
				int d = data[index + offD] & 0xFF;

				int thresh = center - pixelTol;

				int action = 0;

				// check to see if it is significantly below tthe center pixel
				if (a < thresh && c < thresh) {
					if (b < thresh) {
						action = -1;
					} else if (d < thresh) {
						action = -1;
					}
				} else if (b < thresh && d < thresh) {
					if (a < thresh) {
						action = -1;
					} else if (c < thresh) {
						action = -1;
					}
				} else {
					// see if it is significantly more than the center pixel
					thresh = center + pixelTol;

					if (a > thresh && c > thresh) {
						if (d > thresh) {
							action = 1;
						} else if (b > thresh) {
							action = 1;
						}
					}
					if (b > thresh && d > thresh) {
						if (a > thresh) {
							action = 1;
						} else if (c > thresh) {
							action = 1;
						}
					}
				}

				// can't be a corner here so just continue to the next pixel
				if (action == 0) {
					inten[index] = 0F;
					continue;
				}

				boolean isCorner = false;

				// move until it find a valid pixel
				int totalDiff = 0;

				// see if the first pixel is valid or not
				int val = a - center;
				if ((action == -1 && val < -pixelTol) || val > pixelTol) {
					// if it is valid then it needs to deal with wrapping
					int i;
					// find the point a bad pixel is found
					totalDiff += val;
					for (i = 1; i < offsets.length; i++) {
						val = (data[index + offsets[i]] & 0xFF) - center;

						if (action == -1) {
							if (val >= -pixelTol) break;
						} else if (val <= pixelTol) break;

						totalDiff += val;
					}

					int frontLength = i;

					if (frontLength < minCont) {
						// go the other direction
						for (i = offsets.length - 1; i >= 0; i--) {
							val = (data[index + offsets[i]] & 0xFF) - center;

							if (action == -1) {
								if (val >= -pixelTol) break;
							} else if (val <= pixelTol) break;
							totalDiff += val;
						}
						if (offsets.length - 1 - i + frontLength >= minCont) {
							isCorner = true;
						}
					} else {
						isCorner = true;
					}

				} else {
					// find the first good pixel
					int start;
					for (start = 0; start < offsets.length; start++) {
						val = (data[index + offsets[start]] & 0xFF) - center;

						if (action == -1) {
							if (val < -pixelTol) break;
						} else if (val > pixelTol) break;
					}

					// find the point where the good pixels stop
					int stop;
					for (stop = start + 1; stop < offsets.length; stop++) {
						val = (data[index + offsets[stop]] & 0xFF) - center;

						if (action == -1) {
							if (val >= -pixelTol) break;
						} else if (val <= pixelTol) break;
						totalDiff += val;
					}

					isCorner = stop - start >= minCont;

				}

				if (isCorner) {
					inten[index] = totalDiff < 0 ? -totalDiff : totalDiff;
				} else {
					inten[index] = 0F;
				}
			}
		}
	}

}