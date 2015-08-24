/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary.impl;

import boofcv.core.image.border.ImageBorderValue;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.image.ImageUInt8;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * @author Peter Abeles
 */
public class BinaryThinning {

	public static byte mask0[]=new byte[]{ 0, 0, 0,
										  -1, 1,-1,
										   1, 1, 1};
	public static byte mask1[]=new byte[]{-1, 0, 0,
										   1, 1, 0,
										   1, 1,-1};
	public static byte mask2[]=new byte[]{ 1,-1, 0,
										   1, 1, 0,
										   1,-1, 0};
	public static byte mask3[]=new byte[]{ 1, 1,-1,
										   1, 1, 0,
										  -1, 0, 0};
	public static byte mask4[]=new byte[]{ 1, 1, 1,
										  -1, 1,-1,
										   0, 0, 0};
	public static byte mask5[]=new byte[]{-1, 1, 1,
										   0, 1, 1,
										   0, 0,-1};
	public static byte mask6[]=new byte[]{ 0,-1, 1,
										   0, 1, 1,
										   0,-1, 1};
	public static byte mask7[]=new byte[]{ 0, 0,-1,
										   0, 1, 1,
										  -1, 1, 1};

	private Mask masks[] = new Mask[]{ new Mask0(), new Mask1() };

	ImageUInt8 input;
	ImageBorder_S32<ImageUInt8> inputBorder = ImageBorderValue.wrap(input, 1);
	GrowQueue_I32 black0 = new GrowQueue_I32();
	GrowQueue_I32 black1 = new GrowQueue_I32();
	GrowQueue_I32 whiteOut = new GrowQueue_I32();


	public static void naive(ImageUInt8 input, byte []mask, GrowQueue_I32 list , ImageUInt8 output)
	{

	}

	public void apply( ImageUInt8 image ) {
		this.input = image;
		inputBorder.setImage(image);

		black0.reset();
		black1.reset();
		whiteOut.reset();

		// TODO find all black pixels
	}

	protected abstract class Mask {

		byte mask[];

		public Mask(byte[] mask) {
			this.mask = mask;
		}

		public void apply( GrowQueue_I32 blackIn , GrowQueue_I32 whiteOut, GrowQueue_I32 blackOut ) {
			int w = input.width-1;
			int h = input.height-1;

			for (int i = 0; i < blackIn.size; i++) {
				int indexIn = blackIn.get(i);
				int x = (indexIn-input.startIndex)%input.stride;
				int y = (indexIn-input.startIndex)/input.stride;

				boolean remainsBlack;
				if( x == 0 || x == w || y == 0 || y == h ) {
					remainsBlack = borderMask( x, y);
				} else {
					remainsBlack = innerMask(indexIn);
				}
				if( remainsBlack ) {
					blackOut.add(indexIn);
				} else {
					whiteOut.add(indexIn);
				}
			}
		}

		protected boolean borderMask( int x , int y ) {

			return false;
		}

		protected abstract boolean innerMask( int indexIn );

	}

	public class Mask0 extends Mask {

		public Mask0() {
			super(mask0);
		}

		@Override
		protected boolean innerMask(int indexIn) {
			int rowTop = indexIn-input.stride;
			if( input.data[rowTop-1] != 0 || input.data[rowTop] != 0  || input.data[rowTop+1] != 0 ) {
				return true;
			}
			int rowBottom = indexIn+input.stride;
			if( input.data[rowBottom-1] != 1 || input.data[rowBottom] != 1  || input.data[rowBottom+1] != 1 ) {
				return true;
			}
			return false;
		}
	}

	public class Mask1 extends Mask {

		public Mask1() {
			super(mask1);
		}

		@Override
		protected boolean innerMask(int indexIn) {
			int rowTop = indexIn-input.stride;
			int rowBottom = indexIn+input.stride;

			if( input.data[indexIn-1] != 1 || input.data[rowBottom-1] != 1  || input.data[rowBottom] != 1 ) {
				return true;
			}

			if( input.data[rowTop] != 0 || input.data[rowTop+1] != 0  || input.data[indexIn+1] != 0 ) {
				return true;
			}

			return false;
		}
	}
}
