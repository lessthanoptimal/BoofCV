/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.PackedSetsPoint2D_I32;
import boofcv.struct.image.GrayU8;

/**
 * Finds the external contours of binary blobs in linear time. No label image is required. Designed to quickly
 * move through regions with repeat values of 0 and 1. Input image is modified. The outside border is set to zero
 * and the value of contour pixels is set to -1 or -2 to indicate that it has been processed. This algorithm is
 * highly specialized but follows a similar pattern to other finding/tracing algorithms from [1] and [2].
 *
 * Algorithm:
 * <ol>
 *     <li>Fill in image border with zeros</li>
 *     <li>Start at pixel x=1,y=1</li>
 *     <li>Begin Row: Starting at (x,y) scan for a value of not zero. Let this point be (x,y)</li>
 *     <li>Trace contour. This contour must be an external contour and is saved.</li>
 *     <li>Resume scanning the row starting at (x+1,y) for a pixel with a value of zero. This pixel will now be (x,y)</li>
 *     <li>If the pixel (x-1,y) has a value of 1 trace the inner contour. Otherwise move to next step.</li>
 *     <li>Iterate from "Begin Row" until the end of the row has been reached</li>
 *     <li>Move to the next row starting at point (1,y+1)</li>
 * </ol>
 * Look at the code for all the details. There are a couple of important points which are glosed over
 * in the description above.
 *
 * To get the external contours after processing call {@link #getExternalContours()}.
 *
 * <p>
 * [1] Fu Chang and Chun-jen Chen and Chi-jen Lu, "A linear-time component-labeling algorithm using contour
 * tracing technique" Computer Vision and Image Understanding, 2004<br>
 * [2] Rosenfeld, Azriel. "Digital topology." American Mathematical Monthly (1979): 621-630.
 * </p>
 *
 * @author Peter Abeles
 */
public class LinearExternalContours {
	// Maximum number of pixels in an external contour. If the contour is longer than this it will be discarded
	private int maxContourLength = Integer.MAX_VALUE;
	// External contours less than this will be discarded
	private int minContourLength = 0;

	// adjusts coordinate from binary to output
	private int adjustX , adjustY;

	private Tracer tracer;
	private PackedSetsPoint2D_I32 storagePoints = new PackedSetsPoint2D_I32();

	public LinearExternalContours( ConnectRule rule ) {
		tracer = new Tracer(rule);
	}

	/**
	 * Detects contours inside the binary image.
	 * @param binary Binary image. Will be modified. See class description
	 * @param adjustX adjustment applied to coordinate in binary image for contour. 0 or 1 is typical
	 * @param adjustY adjustment applied to coordinate in binary image for contour. 0 or 1 is typical
	 */
	public void process( GrayU8 binary , int adjustX , int adjustY ) {
		// Initialize data structures
		this.adjustX = adjustX;
		this.adjustY = adjustY;
		storagePoints.reset();
		ImageMiscOps.fillBorder(binary, 0, 1);

		tracer.setInputs(binary);
		final byte binaryData[] = binary.data;

		// Scan through the image one row at a time looking for pixels with 1
		for (int y = 1; y < binary.height-1; y++) {
			int x = 1;
			int indexBinary = binary.startIndex + y*binary.stride + 1;

			int end = indexBinary + binary.width - 2;

			while( true ) {
				int delta = findNotZero(binaryData, indexBinary, end) - indexBinary;
				indexBinary += delta;
				if (indexBinary == end)
					break;
				x += delta;

				// If this pixel has NOT already been labeled then trace until it runs into a labeled pixel or it
				// completes the trace. If a labeled pixel is not encountered then it must be an external contour
				if( binaryData[indexBinary] == 1 ) {
					if( tracer.trace(x,y,true) ) {
						int N = storagePoints.sizeOfTail();
						if( N < minContourLength || N >= maxContourLength)
							storagePoints.removeTail();
					} else {
						// it was really an internal contour
						storagePoints.removeTail();
					}
				}

				// It's now inside a ones blob. Move forward until it hits a 0 pixel
				delta = findZero(binaryData, indexBinary, end) - indexBinary;
				indexBinary += delta;
				if (indexBinary == end)
					break;
				x += delta;

				// If this pixel has NOT already been labeled trace until it completes a loop or it encounters a
				// labeled pixel. This is always an internal contour
				if( binaryData[indexBinary-1] == 1 ) {
					tracer.trace(x-1,y,false);
					storagePoints.removeTail();
				} else {
					// Can't be sure if it's entering a hole or leaving the blob. This marker will let the
					// tracer know it just traced an internal contour and not an external contour
					binaryData[indexBinary-1] = -2;
				}
			}
		}
	}


	/**
	 * Searches for a value in the array which is not zero.
	 */
	static int findNotZero( byte[] data , int index , int end ) {
		while( index < end && data[index] == 0 ) {
			index++;
		}
		return index;
	}

	/**
	 * Searches for a value in the array which is zero.
	 */
	static int findZero( byte[] data , int index , int end ) {
		while( index < end && data[index] != 0 ) {
			index++;
		}
		return index;
	}

	@SuppressWarnings("Duplicates")
	class Tracer extends ContourTracerBase {

		public int maxContourLength = Integer.MAX_VALUE;

		public Tracer(ConnectRule rule) {
			super(rule);
		}

		public boolean trace( int initialX , int initialY , boolean external )
		{
			// TODO determine if it's ambigous or not. The number of times this test is
			// done could be reduced I think.
			// verify that it's external. If there are ones above it then it can't possibly be external
			if( external ) {
				indexBinary = binary.getIndex(initialX,initialY);
				if( rule == ConnectRule.EIGHT ) {
					if( binary.data[indexBinary+offsetsBinary[5]] != 0 ||
							binary.data[indexBinary+offsetsBinary[6]] != 0 ||
							binary.data[indexBinary+offsetsBinary[7]] != 0 )
					{
						external = false;
					}
				} else {
					if( binary.data[indexBinary+offsetsBinary[3]] != 0  ) {
						external = false;
					}
				}
			}

			if( external ) {
				this.maxContourLength = LinearExternalContours.this.maxContourLength;
			} else {
				this.maxContourLength = -1;
			}

			// start a contour here
			storagePoints.grow();
			if( rule == ConnectRule.EIGHT )
				dir = external ? 7 : 6;
			else
				dir = external ? 0 : 3;

			x = initialX;
			y = initialY;

			// index of pixels in the image array
			// binary has a 1 pixel border which labeled lacks, hence the -1,-1 for labeled
			indexBinary = binary.getIndex(x,y);
			// give the first pixel a special marking
			storagePoints.addPointToTail(x - adjustX, y - adjustY);
			binary.data[indexBinary] = -2;

			// find the next one pixel.  handle case where its an isolated point
			if( !searchNotZero() ) {
				return true;
			}
			int initialDir = dir;
			moveToNext();
			dir = nextDirection[dir];

			while( true ) {
				searchNotZero();

				if( binary.data[indexBinary] != -2 ) {
					binary.data[indexBinary] = -1;
				} else {
					if( x != initialX || y != initialY ) {
						// found an marker that was left when leaving a blob and it was ambiguous if it was external
						// or internal region of zeros
						return false;
					} else if( dir == initialDir ) {
						return external;
					}
				}
				if( storagePoints.sizeOfTail() <= maxContourLength )
					storagePoints.addPointToTail(x - adjustX, y - adjustY);

				moveToNext();
				dir = nextDirection[dir];
			}
		}

		/**
		 * Searches in a circle around the current point in a clock-wise direction for the first black pixel.
		 */
		private boolean searchNotZero() {
			// Unrolling here results in about a 10% speed up
			if( ruleN == 4 )
				return searchNotOne4();
			else
				return searchNotOne8();
		}

		private boolean searchNotOne4() {
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%4;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%4;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%4;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%4;
			return false;
		}

		private boolean searchNotOne8() {
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			if( binary.data[indexBinary + offsetsBinary[dir]] != 0)
				return true;
			dir = (dir+1)%8;
			return false;
		}

		private void moveToNext() {
			// move to the next pixel using the precomputed pixel index offsets
			indexBinary += offsetsBinary[dir];
			// compute the new pixel coordinate from the binary pixel index
			int a = indexBinary - binary.startIndex;
			x = a%binary.stride;
			y = a/binary.stride;
		}
	}

	public ConnectRule getConnectRule() {
		return tracer.rule;
	}

	public void setConnectRule( ConnectRule rule ) {
		tracer = new Tracer(rule);
	}

	public PackedSetsPoint2D_I32 getExternalContours() {
		return storagePoints;
	}

	public int getMaxContourLength() {
		return maxContourLength;
	}

	public void setMaxContourLength(int maxContourLength) {
		this.maxContourLength = maxContourLength;
	}

	public int getMinContourLength() {
		return minContourLength;
	}

	public void setMinContourLength(int minContourLength) {
		this.minContourLength = minContourLength;
	}
}
