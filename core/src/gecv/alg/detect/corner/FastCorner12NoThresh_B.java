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
 * A fast corner 12 algorithm where features are not pruned based on their difference in value
 * from the center.  This change forces it to consider more poor features, but also removes an
 * arbitrary magic number.  This algorithm can be significantly slower than {@link FastCorner12_B}.  In
 * some tests it is even slower than {@link gecv.alg.detect.corner.impl.KltCorner_I16}.
 *
 * @author Peter Abeles
 */
public class FastCorner12NoThresh_B {
	private final static int radius = 3;

	// the minium number of continuous pixels that must be above or below
	// the center pixel's intensity
	int minCont;

	private ImageInt8 img;
	// the image width and height
	private int widthImg;
	private int heightImg;

	private int[] circle;
	private int[] offsets;

	// intensity of the features
	private ImageFloat32 featInten;

	/**
	 * @param img
	 * @param minCont The minimum number of continuous pixels that must be
	 *                significantly different from the center pixel's intensity
	 */
	public FastCorner12NoThresh_B(ImageInt8 img,
								  int minCont) {
		this(img.getWidth(), img.getHeight(), minCont);
		this.img = img;
	}

	/**
	 * @param imgWidth  the width of the image that will be processed
	 * @param imgHeight the height of the image that will be processed
	 * @param minCont   The minimum number of continuous pixels that must be
	 *                  significantly different from the center pixel's intensity
	 */
	public FastCorner12NoThresh_B(int imgWidth,
								  int imgHeight,
								  int minCont) {
		this.widthImg = imgWidth;
		this.heightImg = imgHeight;
		this.minCont = minCont;
		circle = new int[16];
		offsets = new int[16];

		offsets = DiscretizedCircle.imageOffsets(radius, imgWidth);
		if (offsets.length != 16) throw new RuntimeException("Unexpected number of offsets");

		featInten = new ImageFloat32(imgWidth, imgHeight);
	}

	public void setImage(ImageInt8 img) {
		if (widthImg != img.getWidth() || heightImg != img.getHeight()) {
			throw new IllegalArgumentException("unexpected image size");
		}

		this.img = img;
	}

	public void process() {
		final byte[] data = img.data;
		final float[] inten = featInten.data;

		final int yEnd = heightImg - radius;

		for (int y = radius; y < yEnd; y++) {
			int rowStart = widthImg * y;
			int endX = rowStart + widthImg - radius;

			for (int index = rowStart + radius; index < endX; index++) {

				int center = data[index] & 0xFF;

				int a = data[index - radius] & 0xFF;
				int b = data[index - radius * widthImg] & 0xFF;
				int c = data[index + radius] & 0xFF;
				int d = data[index + radius * widthImg] & 0xFF;

				// see if the sample points are all above or all below the center pixel's
				// intensity

				int action = 0;

				// check to see if it is significantly below the center pixel
				if (a < center && c < center) {
					if (b < center) {
						action = -1;
					} else if (d < center) {
						action = -1;
					}
				} else if (b < center && d < center) {
					if (a < center) {
						action = -1;
					} else if (c < center) {
						action = -1;
					}
				} else if (a > center && c > center) {
					if (d > center) {
						action = 1;
					} else if (b > center) {
						action = 1;
					}
				} else if (b > center && d > center) {
					if (a > center) {
						action = 1;
					} else if (c > center) {
						action = 1;
					}
				}


				// can't be a corner here so just continue to the next pixel
				if (action == 0) {
					inten[index] = 0;
					continue;
				}

				boolean isCorner = false;

				// move until it find a valid pixel
				int totalDiff = 0;

				// see if the first pixel is valid or not
				int val = a - center;
				if ((action == -1 && a < 0) || val > 0) {
					// if it is valid then it needs to deal with wrapping
					int i;
					// find the point a bad pixel is found
					totalDiff += val;
					for (i = 1; i < circle.length; i++) {
						val = (data[index + offsets[i]] & 0xFF) - center;

						if (action == -1) {
							if (val >= 0) break;
						} else if (val <= 0) break;

						totalDiff += val;
					}

					int frontLength = i;

					if (frontLength < minCont) {
						// go the other direction
						for (i = circle.length - 1; i >= 0; i--) {
							val = (data[index + offsets[i]] & 0xFF) - center;

							if (action == -1) {
								if (val >= 0) break;
							} else if (val <= 0) break;
							totalDiff += val;
						}
						if (circle.length - 1 - i + frontLength >= minCont) {
							isCorner = true;
						}
					} else {
						isCorner = true;
					}

//                    if( isCorner )
//                        System.out.println();
				} else {
					// find the first good pixel
					int start;
					for (start = 0; start < circle.length; start++) {
						val = (data[index + offsets[start]] & 0xFF) - center;

						if (action == -1) {
							if (val < 0) break;
						} else if (val > 0) break;
					}

					// find the point where the good pixels stop
					int stop;
					for (stop = start + 1; stop < circle.length; stop++) {
						val = (data[index + offsets[stop]] & 0xFF) - center;

						if (action == -1) {
							if (val >= 0) break;
						} else if (val <= 0) break;
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

	public ImageFloat32 getIntensity() {
		return featInten;
	}
}