/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

/**
 * Masks that are applied to the QR code toe ensure that there are no regions with "structure" in them.
 *
 * @author Peter Abeles
 */
public interface QrCodeMaskPattern {

	QrCodeMaskPattern M000 = new M000();
	QrCodeMaskPattern M001 = new M001();
	QrCodeMaskPattern M010 = new M010();
	QrCodeMaskPattern M011 = new M011();
	QrCodeMaskPattern M100 = new M100();
	QrCodeMaskPattern M101 = new M101();
	QrCodeMaskPattern M110 = new M110();
	QrCodeMaskPattern M111 = new M111();

	/**
	 * Applies the mask to the specified bit. grid coordinates are relative to top left corner (0,0)
	 * @param row module row
	 * @param col module column
	 * @param bitValue value of the bit. 0 or 1
	 * @return value after masking has been applied
	 */
	int apply( int row , int col , int bitValue );

	class M000 implements QrCodeMaskPattern {
		@Override
		public int apply(int row, int col, int bitValue) {
			int mask = (row+col)%2;
			return bitValue^((~mask)&0x1);
		}
	}

	class M001 implements QrCodeMaskPattern {
		@Override
		public int apply(int row, int col, int bitValue) {
			int mask = row%2;
			return bitValue^((~mask)&0x1);
		}
	}
	class M010 implements QrCodeMaskPattern {
		@Override
		public int apply(int row, int col, int bitValue) {
			return bitValue^(col%3 == 0 ? 1 : 0);
		}
	}
	class M011 implements QrCodeMaskPattern {
		@Override
		public int apply(int row, int col, int bitValue) {
			return bitValue^((row+col)%3 == 0 ? 1 : 0);
		}
	}

	class M100 implements QrCodeMaskPattern {
		@Override
		public int apply(int row, int col, int bitValue) {
			int mask = (row/2 + col/3)%2;
			return bitValue^((~mask)&0x1);
		}
	}

	class M101 implements QrCodeMaskPattern {
		@Override
		public int apply(int row, int col, int bitValue) {
			return bitValue^((row*col)%2 + (row*col)%3 == 0 ? 1 : 0);
		}
	}

	class M110 implements QrCodeMaskPattern {
		@Override
		public int apply(int row, int col, int bitValue) {
			return bitValue^(((row*col)%2 + (row*col)%3)%2 == 0 ? 1 : 0);
		}
	}

	class M111 implements QrCodeMaskPattern {
		@Override
		public int apply(int row, int col, int bitValue) {
			int mask = ((row*col)%3 + (row+col)%2)%2;
			return bitValue^((~mask)&0x1);
		}
	}
}
