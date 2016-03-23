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

package boofcv.alg.feature.detect.edge.impl;

import boofcv.core.image.border.FactoryImageBorderAlgs;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;


/**
 * Algorithms for performing non-max suppression.  Edge intensities are set to zero if adjacent pixels
 * have a value greater than the current value.  Adjacency is determined by the gradients
 * discretized direction.
 *
 * NOTE: This is technically not true non-maximum suppression because equal values are allowed.
 *
 * @author Peter Abeles
 */
public class ImplEdgeNonMaxSuppression {

	/**
	 * Only processes the inner image.  Ignoring the border.
	 */
	static public void inner4(GrayF32 intensity , GrayS8 direction , GrayF32 output )
	{
		final int w = intensity.width;
		final int h = intensity.height-1;

		for( int y = 1; y < h; y++ ) {
			int indexI = intensity.startIndex + y*intensity.stride+1;
			int indexD = direction.startIndex + y*direction.stride+1;
			int indexO = output.startIndex + y*output.stride+1;

			int end = indexI + w - 2;
			for( ; indexI < end; indexI++ , indexD++, indexO++ ) {
				int dir = direction.data[indexD];
				int dx,dy;
				
				if( dir == 0 ) {
					dx = 1; dy = 0;
				} else if( dir == 1 ) {
					dx = 1; dy = 1;
				} else if( dir == 2 ) {
					dx = 0; dy = 1;
				} else {
					dx = 1; dy = -1;
				}

				float middle = intensity.data[indexI];

				// suppress the value if either of its neighboring values are more than or equal to it
				if( intensity.data[indexI-dx-dy*intensity.stride] > middle || intensity.data[indexI+dx+dy*intensity.stride] > middle ) {
					output.data[indexO] = 0;
				} else {
					output.data[indexO] = middle;
				}
			}
		}
	}

	/**
	 * Slow algorithm which processes the whole image.
	 */
	static public void naive4(GrayF32 _intensity , GrayS8 direction , GrayF32 output )
	{
		final int w = _intensity.width;
		final int h = _intensity.height;

		ImageBorder_F32 intensity = (ImageBorder_F32)FactoryImageBorderAlgs.value(_intensity, 0);

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				int dir = direction.get(x,y);
				int dx,dy;

				if( dir == 0 ) {
					dx = 1; dy = 0;
				} else if( dir == 1 ) {
					dx = 1; dy = 1;
				} else if( dir == 2 ) {
					dx = 0; dy = 1;
				} else {
					dx = 1; dy = -1;
				}

				float left = intensity.get(x-dx,y-dy);
				float middle = intensity.get(x,y);
				float right = intensity.get(x+dx,y+dy);

				// suppress the value if either of its neighboring values are more than or equal to it
				if( left > middle || right > middle ) {
					output.set(x,y,0);
				} else {
					output.set(x,y,middle);
				}
			}
		}

	}

	/**
	 * Just processes the image border.
	 */
	static public void border4(GrayF32 _intensity , GrayS8 direction , GrayF32 output )
	{
		int w = _intensity.width;
		int h = _intensity.height-1;

		ImageBorder_F32 intensity = (ImageBorder_F32)FactoryImageBorderAlgs.value(_intensity, 0);

		// top border
		for( int x = 0; x < w; x++ ) {
			int dir = direction.get(x,0);
			int dx,dy;

			if( dir == 0 ) {
				dx = 1; dy = 0;
			} else if( dir == 1 ) {
				dx = 1; dy = 1;
			} else if( dir == 2 ) {
				dx = 0; dy = 1;
			} else {
				dx = 1; dy = -1;
			}

			float left = intensity.get(x-dx,-dy);
			float middle = intensity.get(x,0);
			float right = intensity.get(x+dx,dy);

			if( left > middle || right > middle ) {
				output.set(x,0,0);
			} else {
				output.set(x,0,middle);
			}
		}

		// bottom border
		for( int x = 0; x < w; x++ ) {
			int dir = direction.get(x,h);
			int dx,dy;

			if( dir == 0 ) {
				dx = 1; dy = 0;
			} else if( dir == 1 ) {
				dx = 1; dy = 1;
			} else if( dir == 2 ) {
				dx = 0; dy = 1;
			} else {
				dx = 1; dy = -1;
			}

			float left = intensity.get(x-dx,h-dy);
			float middle = intensity.get(x,h);
			float right = intensity.get(x+dx,h+dy);

			if( left > middle || right > middle ) {
				output.set(x,h,0);
			} else {
				output.set(x,h,middle);
			}
		}

		// left border
		for( int y = 1; y < h; y++ ) {
			int dir = direction.get(0,y);
			int dx,dy;

			if( dir == 0 ) {
				dx = 1; dy = 0;
			} else if( dir == 1 ) {
				dx = 1; dy = 1;
			} else if( dir == 2 ) {
				dx = 0; dy = 1;
			} else {
				dx = 1; dy = -1;
			}

			float left = intensity.get(-dx,y-dy);
			float middle = intensity.get(0,y);
			float right = intensity.get(dx,y+dy);

			if( left > middle || right > middle ) {
				output.set(0,y,0);
			} else {
				output.set(0,y,middle);
			}
		}

		// right border
		w = w - 1;
		for( int y = 1; y < h; y++ ) {
			int dir = direction.get(w,y);
			int dx,dy;

			if( dir == 0 ) {
				dx = 1; dy = 0;
			} else if( dir == 1 ) {
				dx = 1; dy = 1;
			} else if( dir == 2 ) {
				dx = 0; dy = 1;
			} else {
				dx = 1; dy = -1;
			}

			float left = intensity.get(w-dx,y-dy);
			float middle = intensity.get(w,y);
			float right = intensity.get(w+dx,y+dy);

			if( left > middle || right > middle ) {
				output.set(w,y,0);
			} else {
				output.set(w,y,middle);
			}
		}
	}

	/**
	 * Only processes the inner image.  Ignoring the border.
	 */
	static public void inner8(GrayF32 intensity , GrayS8 direction , GrayF32 output )
	{
		final int w = intensity.width;
		final int h = intensity.height-1;

		for( int y = 1; y < h; y++ ) {
			int indexI = intensity.startIndex + y*intensity.stride+1;
			int indexD = direction.startIndex + y*direction.stride+1;
			int indexO = output.startIndex + y*output.stride+1;

			int end = indexI + w - 2;
			for( ; indexI < end; indexI++ , indexD++, indexO++ ) {
				int dir = direction.data[indexD];
				int dx,dy;

				if( dir == 0 || dir == 4) {
					dx = 1; dy = 0;
				} else if( dir == 1 || dir == -3) {
					dx = 1; dy = 1;
				} else if( dir == 2 || dir == -2) {
					dx = 0; dy = 1;
				} else {
					dx = 1; dy = -1;
				}

				float middle = intensity.data[indexI];

				// suppress the value if either of its neighboring values are more than or equal to it
				if( intensity.data[indexI-dx-dy*intensity.stride] > middle || intensity.data[indexI+dx+dy*intensity.stride] > middle ) {
					output.data[indexO] = 0;
				} else {
					output.data[indexO] = middle;
				}
			}
		}
	}

	/**
	 * Slow algorithm which processes the whole image.
	 */
	static public void naive8(GrayF32 _intensity , GrayS8 direction , GrayF32 output )
	{
		final int w = _intensity.width;
		final int h = _intensity.height;

		ImageBorder_F32 intensity = (ImageBorder_F32)FactoryImageBorderAlgs.value(_intensity, 0);

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				int dir = direction.get(x,y);
				int dx,dy;

				if( dir == 0 || dir == 4) {
					dx = 1; dy = 0;
				} else if( dir == 1 || dir == -3) {
					dx = 1; dy = 1;
				} else if( dir == 2 || dir == -2) {
					dx = 0; dy = 1;
				} else {
					dx = 1; dy = -1;
				}

				float left = intensity.get(x-dx,y-dy);
				float middle = intensity.get(x,y);
				float right = intensity.get(x+dx,y+dy);

				// suppress the value if either of its neighboring values are more than or equal to it
				if( left > middle || right > middle ) {
					output.set(x,y,0);
				} else {
					output.set(x,y,middle);
				}
			}
		}

	}

	/**
	 * Just processes the image border.
	 */
	static public void border8(GrayF32 _intensity , GrayS8 direction , GrayF32 output )
	{
		int w = _intensity.width;
		int h = _intensity.height-1;

		ImageBorder_F32 intensity = (ImageBorder_F32)FactoryImageBorderAlgs.value(_intensity, 0);

		// top border
		for( int x = 0; x < w; x++ ) {
			int dir = direction.get(x,0);
			int dx,dy;

			if( dir == 0 || dir == 4) {
				dx = 1; dy = 0;
			} else if( dir == 1 || dir == -3) {
				dx = 1; dy = 1;
			} else if( dir == 2 || dir == -2) {
				dx = 0; dy = 1;
			} else {
				dx = 1; dy = -1;
			}

			float left = intensity.get(x-dx,-dy);
			float middle = intensity.get(x,0);
			float right = intensity.get(x+dx,dy);

			if( left > middle || right > middle ) {
				output.set(x,0,0);
			} else {
				output.set(x,0,middle);
			}
		}

		// bottom border
		for( int x = 0; x < w; x++ ) {
			int dir = direction.get(x,h);
			int dx,dy;

			if( dir == 0 || dir == 4) {
				dx = 1; dy = 0;
			} else if( dir == 1 || dir == -3) {
				dx = 1; dy = 1;
			} else if( dir == 2 || dir == -2) {
				dx = 0; dy = 1;
			} else {
				dx = 1; dy = -1;
			}

			float left = intensity.get(x-dx,h-dy);
			float middle = intensity.get(x,h);
			float right = intensity.get(x+dx,h+dy);

			if( left > middle || right > middle ) {
				output.set(x,h,0);
			} else {
				output.set(x,h,middle);
			}
		}

		// left border
		for( int y = 1; y < h; y++ ) {
			int dir = direction.get(0,y);
			int dx,dy;

			if( dir == 0 || dir == 4) {
				dx = 1; dy = 0;
			} else if( dir == 1 || dir == -3) {
				dx = 1; dy = 1;
			} else if( dir == 2 || dir == -2) {
				dx = 0; dy = 1;
			} else {
				dx = 1; dy = -1;
			}

			float left = intensity.get(-dx,y-dy);
			float middle = intensity.get(0,y);
			float right = intensity.get(dx,y+dy);

			if( left > middle || right > middle ) {
				output.set(0,y,0);
			} else {
				output.set(0,y,middle);
			}
		}

		// right border
		w = w - 1;
		for( int y = 1; y < h; y++ ) {
			int dir = direction.get(w,y);
			int dx,dy;

			if( dir == 0 || dir == 4) {
				dx = 1; dy = 0;
			} else if( dir == 1 || dir == -3) {
				dx = 1; dy = 1;
			} else if( dir == 2 || dir == -2) {
				dx = 0; dy = 1;
			} else {
				dx = 1; dy = -1;
			}

			float left = intensity.get(w-dx,y-dy);
			float middle = intensity.get(w,y);
			float right = intensity.get(w+dx,y+dy);

			if( left > middle || right > middle ) {
				output.set(w,y,0);
			} else {
				output.set(w,y,middle);
			}
		}
	}

}
