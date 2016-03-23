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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_I32;

/**
 * <p>
 * Implementation of {@link NonMaxBlock} which implements a relaxed maximum rule.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class NonMaxBlockRelaxed extends NonMaxBlock {

	// storage for local maximums
	Point2D_I32 foundMax[];
	Point2D_I32 foundMin[];

	protected NonMaxBlockRelaxed(boolean detectsMinimum, boolean detectsMaximum) {
		super(detectsMinimum, detectsMaximum);
	}

	public static class Max extends NonMaxBlockRelaxed {
		public Max() { super(false, true); }

		@Override
		protected void searchBlock( int x0 , int y0 , int x1 , int y1 , GrayF32 img ) {

			int numPeaks = 0;
			float peakVal = thresholdMax;

			for( int y = y0; y < y1; y++ ) {
				int index = img.startIndex + y*img.stride+x0;
				for( int x = x0; x < x1; x++ ) {
					float v = img.data[index++];

					if( v > peakVal ) {
						peakVal = v;
						foundMax[0].set(x, y);
						numPeaks = 1;
					} else if( v == peakVal ) {
						foundMax[numPeaks++].set(x, y);
					}
				}
			}

			if( numPeaks > 0 && peakVal != Float.MAX_VALUE ) {
				for( int i = 0; i < numPeaks; i++ ) {
					Point2D_I32 p = foundMax[i];
					checkLocalMax(p.x,p.y,peakVal,img);
				}
			}
		}
	}

	public static class Min extends NonMaxBlockRelaxed {
		public Min() { super(true, false); }

		@Override
		protected void searchBlock( int x0 , int y0 , int x1 , int y1 , GrayF32 img ) {

			int numPeaks = 0;
			float peakVal = thresholdMin;

			for( int y = y0; y < y1; y++ ) {
				int index = img.startIndex + y*img.stride+x0;
				for( int x = x0; x < x1; x++ ) {
					float v = img.data[index++];

					if( v < peakVal ) {
						peakVal = v;
						foundMin[0].set(x, y);
						numPeaks = 1;
					} else if( v == peakVal ) {
						foundMin[numPeaks++].set(x, y);
					}
				}
			}

			if( numPeaks > 0 && peakVal != -Float.MAX_VALUE ) {
				for( int i = 0; i < numPeaks; i++ ) {
					Point2D_I32 p = foundMin[i];
					checkLocalMin(p.x, p.y, peakVal, img);
				}
			}
		}
	}

	public static class MinMax extends NonMaxBlockRelaxed {
		public MinMax() { super(true, true); }

		@Override
		protected void searchBlock( int x0 , int y0 , int x1 , int y1 , GrayF32 img ) {

			int numMinPeaks = 0;
			float peakMinVal = thresholdMin;
			int numMaxPeaks = 0;
			float peakMaxVal = thresholdMax;

			for( int y = y0; y < y1; y++ ) {
				int index = img.startIndex + y*img.stride+x0;
				for( int x = x0; x < x1; x++ ) {
					float v = img.data[index++];

					if( v < peakMinVal ) {
						peakMinVal = v;
						foundMin[0].set(x, y);
						numMinPeaks = 1;
					} else if( v == peakMinVal ) {
						foundMin[numMinPeaks++].set(x, y);
					}

					if( v > peakMaxVal ) {
						peakMaxVal = v;
						foundMax[0].set(x, y);
						numMaxPeaks = 1;
					} else if( v == peakMaxVal ) {
						foundMax[numMaxPeaks++].set(x, y);
					}
				}
			}

			if( numMinPeaks > 0 && peakMinVal != -Float.MAX_VALUE ) {
				for( int i = 0; i < numMinPeaks; i++ ) {
					Point2D_I32 p = foundMin[i];
					checkLocalMin(p.x,p.y,peakMinVal,img);
				}
			}

			if( numMaxPeaks > 0 && peakMaxVal != Float.MAX_VALUE ) {
				for( int i = 0; i < numMaxPeaks; i++ ) {
					Point2D_I32 p = foundMax[i];
					checkLocalMax(p.x,p.y,peakMaxVal,img);
				}
			}
		}
	}

	protected void checkLocalMax( int x_c , int y_c , float peakVal , GrayF32 img ) {
		int x0 = x_c-radius;
		int x1 = x_c+radius;
		int y0 = y_c-radius;
		int y1 = y_c+radius;

		if (x0 < 0) x0 = 0;
		if (y0 < 0) y0 = 0;
		if (x1 >= img.width) x1 = img.width - 1;
		if (y1 >= img.height) y1 = img.height - 1;

		for( int y = y0; y <= y1; y++ ) {
			int index = img.startIndex + y*img.stride+x0;
			for( int x = x0; x <= x1; x++ ) {
				float v = img.data[index++];

				if( v > peakVal ) {
					// not a local maximum
					return;
				}
			}
		}

		localMax.add(x_c,y_c);
	}

	protected void checkLocalMin( int x_c , int y_c , float peakVal , GrayF32 img ) {
		int x0 = x_c-radius;
		int x1 = x_c+radius;
		int y0 = y_c-radius;
		int y1 = y_c+radius;

		if (x0 < 0) x0 = 0;
		if (y0 < 0) y0 = 0;
		if (x1 >= img.width) x1 = img.width - 1;
		if (y1 >= img.height) y1 = img.height - 1;

		for( int y = y0; y <= y1; y++ ) {
			int index = img.startIndex + y*img.stride+x0;
			for( int x = x0; x <= x1; x++ ) {
				float v = img.data[index++];

				if( v < peakVal ) {
					// not a local minimum
					return;
				}
			}
		}

		localMin.add(x_c,y_c);
	}

	@Override
	public void setSearchRadius(int radius) {
		super.setSearchRadius(radius);

		int w = 2* radius +1;

		foundMax = new Point2D_I32[w*w];
		for( int i = 0; i < foundMax.length; i++ )
			foundMax[i] = new Point2D_I32();
		foundMin = new Point2D_I32[w*w];
		for( int i = 0; i < foundMin.length; i++ )
			foundMin[i] = new Point2D_I32();
	}

}
