/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.feature.detect.intensity.FastCornerIntensity;
import boofcv.misc.DiscretizedCircle;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;

/**
 * TODO Comment
 *
 * @author Peter Abeles
 */
public class ImplFastCorner12_Table_F32 implements FastCornerIntensity<ImageFloat32> {

	// look up table for identifying if a feature is a corner or not
	boolean table[] = new boolean[65536];

	private final static int radius = 3;

	// how similar do the pixel in the circle need to be to the center pixel
	private float pixelTol;

	// list of pixels that might be corners.
	private QueueCorner candidates = new QueueCorner(10);

	/**
	 *
	 * @param consecutive minimum number of features in a row.  Typically 9
	 */
	public ImplFastCorner12_Table_F32(float pixelTol , int consecutive) {
		this.pixelTol = pixelTol;
		createTable(consecutive);
	}

	private void createTable( int consecutive ) {
		for( int v = 0; v < 65536; v++ ) {
			table[v] = isFeature(consecutive,v);
		}
	}

	/**
	 * Searches to see if there are at least 'consecutive' high bits in a row
	 */
	private boolean isFeature( int consecutive , int value ) {
		for( int start = 0; start < 12; start++ ) {
			boolean worked = true;
			for( int i = 0; i < consecutive; i++ ) {
				if( (value & ( 1 << ((start + i) % 12) )) == 0 ) {
					worked = false;
					break;
				}
			}
			if( worked )
				return true;
		}

		return false;
	}

	@Override
	public void process(ImageFloat32 input, ImageFloat32 intensity) {
		candidates.reset();
		final float[] data = input.data;

		final int width = input.getWidth();
		final int yEnd = input.getHeight() - radius;
		final int stride = input.stride;

		// relative offsets of pixel locations in a circle
		int []offsets = DiscretizedCircle.imageOffsets(radius, stride);

		final float[] inten = intensity.data;

		int offA = offsets[0];
		int offB = offsets[4];
		int offC = offsets[8];
		int offD = offsets[12];

		for (int y = radius; y < yEnd; y++) {
			int rowStart = input.startIndex + stride * y;
			int endX = rowStart + width - radius;
			int intenIndex = intensity.startIndex + y*intensity.stride+radius;
			for (int index = rowStart + radius; index < endX; index++,intenIndex++) {

				// quickly eliminate bad choices by examining 4 points spread out
				float center = data[index];

				float v0 = data[index + offA];
				float v4 = data[index + offB];
				float v8 = data[index + offC];
				float v12 = data[index + offD];

				float thresh = center - pixelTol;

				int action = 0;

				// check to see if it is significantly below the center pixel
				if (v0 < thresh && v8 < thresh) {
					if (v4 < thresh) {
						action = -1;
					} else if (v12 < thresh) {
						action = -1;
					}
				} else if (v4 < thresh && v12 < thresh) {
					if (v0 < thresh) {
						action = -1;
					} else if (v8 < thresh) {
						action = -1;
					}
				} else {
					// see if it is significantly more than the center pixel
					thresh = center + pixelTol;

					if (v0 > thresh && v8 > thresh) {
						if (v12 > thresh) {
							action = 1;
						} else if (v4 > thresh) {
							action = 1;
						}
					}
					if (v4 > thresh && v12 > thresh) {
						if (v0 > thresh) {
							action = 1;
						} else if (v8 > thresh) {
							action = 1;
						}
					}
				}

				// can't be a corner here so just continue to the next pixel
				if (action == 0) {
					inten[intenIndex] = 0F;
					continue;
				}

				float v1 = data[index + offsets[1]];
				float v2 = data[index + offsets[2]];
				float v3 = data[index + offsets[3]];
				float v5 = data[index + offsets[5]];
				float v6 = data[index + offsets[6]];
				float v7 = data[index + offsets[7]];
				float v9 = data[index + offsets[9]];
				float v10 = data[index + offsets[10]];
				float v11 = data[index + offsets[11]];
				float v13 = data[index + offsets[13]];
				float v14 = data[index + offsets[14]];
				float v15 = data[index + offsets[15]];

				int code = 0;
				if( action == -1 ) {
					code |= (Float.floatToRawIntBits(v0-thresh) & 0x80000000) >>> 31;
					code |= (Float.floatToRawIntBits(v1-thresh) & 0x80000000) >>> 30;
					code |= (Float.floatToRawIntBits(v2-thresh) & 0x80000000) >>> 29;
					code |= (Float.floatToRawIntBits(v3-thresh) & 0x80000000) >>> 28;
					code |= (Float.floatToRawIntBits(v4-thresh) & 0x80000000) >>> 27;
					code |= (Float.floatToRawIntBits(v5-thresh) & 0x80000000) >>> 26;
					code |= (Float.floatToRawIntBits(v6-thresh) & 0x80000000) >>> 25;
					code |= (Float.floatToRawIntBits(v7-thresh) & 0x80000000) >>> 24;
					code |= (Float.floatToRawIntBits(v8-thresh) & 0x80000000) >>> 23;
					code |= (Float.floatToRawIntBits(v9-thresh) & 0x80000000) >>> 22;
					code |= (Float.floatToRawIntBits(v10-thresh) & 0x80000000) >>> 21;
					code |= (Float.floatToRawIntBits(v11-thresh) & 0x80000000) >>> 20;
					code |= (Float.floatToRawIntBits(v12-thresh) & 0x80000000) >>> 19;
					code |= (Float.floatToRawIntBits(v13-thresh) & 0x80000000) >>> 18;
					code |= (Float.floatToRawIntBits(v14-thresh) & 0x80000000) >>> 17;
					code |= (Float.floatToRawIntBits(v15-thresh) & 0x80000000) >>> 16;

//					if( v0 < thresh ) code |= 0x0001;
//					if( v1 < thresh ) code |= 0x0002;
//					if( v2 < thresh ) code |= 0x0004;
//					if( v3 < thresh ) code |= 0x0008;
//					if( v4 < thresh ) code |= 0x0010;
//					if( v5 < thresh ) code |= 0x0020;
//					if( v6 < thresh ) code |= 0x0040;
//					if( v7 < thresh ) code |= 0x0080;
//					if( v8 < thresh ) code |= 0x0100;
//					if( v9 < thresh ) code |= 0x0200;
//					if( v10 < thresh ) code |= 0x0400;
//					if( v11 < thresh ) code |= 0x0800;
//					if( v12 < thresh ) code |= 0x1000;
//					if( v13 < thresh ) code |= 0x2000;
//					if( v14 < thresh ) code |= 0x4000;
//					if( v15 < thresh ) code |= 0x8000;
				} else {
//					if( v0 > thresh ) code |= 0x0001;
//					if( v1 > thresh ) code |= 0x0002;
//					if( v2 > thresh ) code |= 0x0004;
//					if( v3 > thresh ) code |= 0x0008;
//					if( v4 > thresh ) code |= 0x0010;
//					if( v5 > thresh ) code |= 0x0020;
//					if( v6 > thresh ) code |= 0x0040;
//					if( v7 > thresh ) code |= 0x0080;
//					if( v8 > thresh ) code |= 0x0100;
//					if( v9 > thresh ) code |= 0x0200;
//					if( v10 > thresh ) code |= 0x0400;
//					if( v11 > thresh ) code |= 0x0800;
//					if( v12 > thresh ) code |= 0x1000;
//					if( v13 > thresh ) code |= 0x2000;
//					if( v14 > thresh ) code |= 0x4000;
//					if( v15 > thresh ) code |= 0x8000;
					code |= (Float.floatToRawIntBits(v0-thresh) & 0x80000000) >>> 31;
					code |= (Float.floatToRawIntBits(v1-thresh) & 0x80000000) >>> 30;
					code |= (Float.floatToRawIntBits(v2-thresh) & 0x80000000) >>> 29;
					code |= (Float.floatToRawIntBits(v3-thresh) & 0x80000000) >>> 28;
					code |= (Float.floatToRawIntBits(v4-thresh) & 0x80000000) >>> 27;
					code |= (Float.floatToRawIntBits(v5-thresh) & 0x80000000) >>> 26;
					code |= (Float.floatToRawIntBits(v6-thresh) & 0x80000000) >>> 25;
					code |= (Float.floatToRawIntBits(v7-thresh) & 0x80000000) >>> 24;
					code |= (Float.floatToRawIntBits(v8-thresh) & 0x80000000) >>> 23;
					code |= (Float.floatToRawIntBits(v9-thresh) & 0x80000000) >>> 22;
					code |= (Float.floatToRawIntBits(v10-thresh) & 0x80000000) >>> 21;
					code |= (Float.floatToRawIntBits(v11-thresh) & 0x80000000) >>> 20;
					code |= (Float.floatToRawIntBits(v12-thresh) & 0x80000000) >>> 19;
					code |= (Float.floatToRawIntBits(v13-thresh) & 0x80000000) >>> 18;
					code |= (Float.floatToRawIntBits(v14-thresh) & 0x80000000) >>> 17;
					code |= (Float.floatToRawIntBits(v15-thresh) & 0x80000000) >>> 16;
				}

				if (table[code]) {
					float totalDiff = 10;
					inten[intenIndex] = action == -1 ? -totalDiff : totalDiff;
					candidates.add( index-rowStart , y );
				} else {
					inten[intenIndex] = 0F;
				}
			}
		}
	}


	@Override
	public int getRadius() {
		return radius;
	}

	@Override
	public int getIgnoreBorder() {
		return radius;
	}

	@Override
	public QueueCorner getCandidates() {
		return candidates;
	}
}
