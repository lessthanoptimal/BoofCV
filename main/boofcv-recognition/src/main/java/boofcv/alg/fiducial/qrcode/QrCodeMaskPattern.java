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

import java.util.ArrayList;
import java.util.List;

/**
 * Masks that are applied to the QR code toe ensure that there are no regions with "structure" in them. Avoid
 * accidentally having patterns that look like a finder pattern or alignment pattern.
 *
 * @author Peter Abeles
 */
public abstract class QrCodeMaskPattern {

	public static final QrCodeMaskPattern M000 = new M000();
	public static final QrCodeMaskPattern M001 = new M001();
	public static final QrCodeMaskPattern M010 = new M010();
	public static final QrCodeMaskPattern M011 = new M011();
	public static final QrCodeMaskPattern M100 = new M100();
	public static final QrCodeMaskPattern M101 = new M101();
	public static final QrCodeMaskPattern M110 = new M110();
	public static final QrCodeMaskPattern M111 = new M111();

	int bits;

	public static List<QrCodeMaskPattern> values() {
		List<QrCodeMaskPattern> values = new ArrayList<>();
		values.add(M001);
		values.add(M010);
		values.add(M011);
		values.add(M100);
		values.add(M101);
		values.add(M110);
		values.add(M111);
		return values;
	}

	public QrCodeMaskPattern( int bits ) {
		this.bits = bits;
	}

	/**
	 * Applies the mask to the specified bit. grid coordinates are relative to top left corner (0,0)
	 * @param row module row
	 * @param col module column
	 * @param bitValue value of the bit. 0 or 1
	 * @return value after masking has been applied
	 */
	public abstract int apply( int row , int col , int bitValue );

	public static QrCodeMaskPattern lookupMask( int maskPattern ) {
		switch( maskPattern ) {
			case 0b000: return M000;
			case 0b001: return M001;
			case 0b010: return M010;
			case 0b011: return M011;
			case 0b100: return M100;
			case 0b101: return M101;
			case 0b110: return M110;
			case 0b111: return M111;
			default: throw new RuntimeException("Unknown mask: "+maskPattern);
		}
	}

	public static QrCodeMaskPattern lookupMask( String maskPattern ) {
		switch( maskPattern ) {
			case "000": return M000;
			case "001": return M001;
			case "010": return M010;
			case "011": return M011;
			case "100": return M100;
			case "101": return M101;
			case "110": return M110;
			case "111": return M111;
			default: throw new RuntimeException("Unknown mask: "+maskPattern);
		}
	}

	static class NONE extends QrCodeMaskPattern {
		public NONE(int bits) { super(bits); }

		@Override
		public int apply(int row, int col, int bitValue) {
			return bitValue;
		}
	}
	static class M000 extends QrCodeMaskPattern {
		public M000() { super(0b000); }

		@Override
		public int apply(int row, int col, int bitValue) {
			int mask = (row+col)%2;
			return bitValue^((~mask)&0x1);
		}
	}
	static class M001 extends QrCodeMaskPattern {
		public M001() { super(0b001); }
		@Override
		public int apply(int row, int col, int bitValue) {
			int mask = row%2;
			return bitValue^((~mask)&0x1);
		}
	}
	static class M010 extends QrCodeMaskPattern {
		public M010() { super(0b010); }
		@Override
		public int apply(int row, int col, int bitValue) {
			return bitValue^(col%3 == 0 ? 1 : 0);
		}
	}
	static class M011 extends QrCodeMaskPattern {
		public M011() { super(0b011); }
		@Override
		public int apply(int row, int col, int bitValue) {
			return bitValue^((row+col)%3 == 0 ? 1 : 0);
		}
	}
	static class M100 extends QrCodeMaskPattern {
		public M100() { super(0b100); }
		@Override
		public int apply(int row, int col, int bitValue) {
			int mask = (row/2 + col/3)%2;
			return bitValue^((~mask)&0x1);
		}
	}
	static class M101 extends QrCodeMaskPattern {
		public M101() { super(0b101); }
		@Override
		public int apply(int row, int col, int bitValue) {
			return bitValue^((row*col)%2 + (row*col)%3 == 0 ? 1 : 0);
		}
	}
	static class M110 extends QrCodeMaskPattern {
		public M110() { super(0b110); }
		@Override
		public int apply(int row, int col, int bitValue) {
			return bitValue^(((row*col)%2 + (row*col)%3)%2 == 0 ? 1 : 0);
		}
	}
	static class M111 extends QrCodeMaskPattern {
		public M111() { super(0b111); }
		@Override
		public int apply(int row, int col, int bitValue) {
			int mask = ((row*col)%3 + (row+col)%2)%2;
			return bitValue^((~mask)&0x1);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
