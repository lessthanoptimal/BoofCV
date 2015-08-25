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
 * Applies binary thinning operators.  There are 8 masks and each is run in sequence.
 *
 * TODO CITE SORUCE
 * TODO SAY WHAT ITS GUARANTEES ARE
 *
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

	private Mask masks[] = new Mask[]{
			new Mask0(), new Mask1(), new Mask2(), new Mask3(),
			new Mask4(), new Mask5(), new Mask6(), new Mask7() };

	// reference to input image
	ImageUInt8 binary;
	// all pixels outside the image are set to 0
	ImageBorder_S32<ImageUInt8> inputBorder = ImageBorderValue.wrap(binary, 0);
	// list of black pixels, input
	GrowQueue_I32 black0 = new GrowQueue_I32();
	// list of black pixels, output
	GrowQueue_I32 black1 = new GrowQueue_I32();
	// list of pixels which need to be set to white
	GrowQueue_I32 whiteOut = new GrowQueue_I32();

	/**
	 * Applies the thinning algorithm.  Runs for the specified number of loops or until no change is detected.
	 *
	 * @param binary Input binary image which is to be thinned.  This is modified
	 * @param maxLoops Maximum number of thinning loops.  Set to -1 to run until the image is no longer modified.
	 */
	public void apply( ImageUInt8 binary , int maxLoops) {
		this.binary = binary;
		inputBorder.setImage(binary);

		black0.reset();
		black1.reset();
		whiteOut.reset();

		findBlackPixels(black0);

		escape:
		for (int loop = 0; loop < maxLoops || maxLoops == -1; loop++) {
			// do one cycle through all the masks
			for (int i = 0; i < masks.length; i++) {
				Mask mask = masks[i];
				mask.apply(black0,whiteOut,black1);

				if( black0.size() == black1.size() )
					break escape;

				// modify the input image and turn the pixels which changes white
				for (int j = 0; j < whiteOut.size(); j++) {
					binary.data[whiteOut.get(j)] = 0;
				}

				// swap the lists
				GrowQueue_I32 tmp = black0;
				black0 = black1;
				black1 = tmp;

				// reset data structures
				whiteOut.reset();
				black1.reset();
			}
		}

	}

	/**
	 * Scans through the image and record the array index of all black pixels
	 */
	protected void findBlackPixels( GrowQueue_I32 black ) {
		for (int y = 0; y < binary.height; y++) {
			int index = binary.startIndex + y* binary.stride;
			for (int x = 0; x < binary.width; x++) {
				if( binary.data[index++] != 0 ) {
					black.add(index);
				}
			}
		}
	}

	/**
	 * Abstract class for applying the mask.  Uses sparse list of black pixels.  Determines if the pixel is along
	 * the border or not.  If border then generic code is code, otherwise opimized code is called.
	 */
	protected abstract class Mask {

		byte mask[];

		public Mask(byte[] mask) {
			this.mask = mask;
		}

		public void apply( GrowQueue_I32 blackIn , GrowQueue_I32 whiteOut, GrowQueue_I32 blackOut ) {
			int w = binary.width-1;
			int h = binary.height-1;

			for (int i = 0; i < blackIn.size; i++) {
				int indexIn = blackIn.get(i);
				int x = (indexIn- binary.startIndex)% binary.stride;
				int y = (indexIn- binary.startIndex)/ binary.stride;

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

		protected boolean borderMask( int cx , int cy ) {

			int maskIndex = 0;
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					int m = mask[maskIndex++];
					if( m == -1 ) continue;
					int pixel = inputBorder.get(cx+j,cy+i);
					if( m == 0 ) {
						if( pixel != 0 )
							return true;
					} else if( pixel != 1 )
						return true;
				}
			}

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
			int rowTop = indexIn- binary.stride;
			if( binary.data[rowTop-1] != 0 || binary.data[rowTop] != 0  || binary.data[rowTop+1] != 0 ) {
				return true;
			}
			int rowBottom = indexIn+ binary.stride;
			if( binary.data[rowBottom-1] != 1 || binary.data[rowBottom] != 1  || binary.data[rowBottom+1] != 1 ) {
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
			int rowTop = indexIn- binary.stride;
			int rowBottom = indexIn+ binary.stride;

			if( binary.data[indexIn-1] != 1 || binary.data[rowBottom-1] != 1  || binary.data[rowBottom] != 1 ) {
				return true;
			}

			if( binary.data[rowTop] != 0 || binary.data[rowTop+1] != 0  || binary.data[indexIn+1] != 0 ) {
				return true;
			}

			return false;
		}
	}

	public class Mask2 extends Mask {

		public Mask2() {
			super(mask2);
		}

		@Override
		protected boolean innerMask(int indexIn) {
			int rowTop = indexIn- binary.stride;
			if( binary.data[indexIn-1] != 1 || binary.data[rowTop-1] != 1  || binary.data[rowTop+1] != 0 ) {
				return true;
			}

			int rowBottom = indexIn+ binary.stride;
			if( binary.data[rowBottom-1] != 1 || binary.data[rowBottom+1] != 0 ) {
				return true;
			}

			return false;
		}
	}

	public class Mask3 extends Mask {

		public Mask3() {
			super(mask3);
		}

		@Override
		protected boolean innerMask(int indexIn) {
			int rowTop = indexIn- binary.stride;
			if( binary.data[indexIn-1] != 1 || binary.data[rowTop-1] != 1 || binary.data[rowTop] != 1 ) {
				return true;
			}

			int rowBottom = indexIn+ binary.stride;
			if( binary.data[rowBottom] != 0 || binary.data[rowBottom+1] != 0 ) {
				return true;
			}

			return false;
		}
	}

	public class Mask4 extends Mask {

		public Mask4() {
			super(mask4);
		}

		@Override
		protected boolean innerMask(int indexIn) {
			int rowTop = indexIn- binary.stride;
			if( binary.data[rowTop-1] != 1 || binary.data[rowTop] != 1 || binary.data[rowTop+1] != 1 ) {
				return true;
			}

			int rowBottom = indexIn+ binary.stride;
			if( binary.data[rowBottom-1] != 0  || binary.data[rowBottom] != 0 || binary.data[rowBottom+1] != 0 ) {
				return true;
			}

			return false;
		}
	}

	public class Mask5 extends Mask {

		public Mask5() {
			super(mask5);
		}

		@Override
		protected boolean innerMask(int indexIn) {
			int rowTop = indexIn- binary.stride;
			if( binary.data[indexIn-1] != 0 || binary.data[indexIn+1] != 1 || binary.data[rowTop] != 1 || binary.data[rowTop+1] != 1 ) {
				return true;
			}

			int rowBottom = indexIn+ binary.stride;
			if( binary.data[rowBottom-1] != 0  || binary.data[rowBottom] != 0 ) {
				return true;
			}

			return false;
		}
	}

	public class Mask6 extends Mask {

		public Mask6() {
			super(mask6);
		}

		@Override
		protected boolean innerMask(int indexIn) {
			int rowTop = indexIn- binary.stride;
			if( binary.data[indexIn-1] != 0 || binary.data[indexIn+1] != 1  || binary.data[rowTop-1] != 0 ||  binary.data[rowTop+1] != 1 ) {
				return true;
			}

			int rowBottom = indexIn+ binary.stride;
			if( binary.data[rowBottom-1] != 0  || binary.data[rowBottom+1] != 1 ) {
				return true;
			}

			return false;
		}
	}

	public class Mask7 extends Mask {

		public Mask7() {
			super(mask7);
		}

		@Override
		protected boolean innerMask(int indexIn) {
			int rowTop = indexIn- binary.stride;
			if( binary.data[rowTop-1] != 0 || binary.data[rowTop] != 0 || binary.data[indexIn-1] != 0 || binary.data[indexIn+1] != 1 ) {
				return true;
			}

			int rowBottom = indexIn+ binary.stride;
			if( binary.data[rowBottom] != 1 || binary.data[rowBottom+1] != 1 ) {
				return true;
			}

			return false;
		}
	}
}
