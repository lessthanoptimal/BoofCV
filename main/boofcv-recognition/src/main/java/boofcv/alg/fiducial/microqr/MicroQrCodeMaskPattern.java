/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.microqr;

import java.util.ArrayList;
import java.util.List;

/**
 * Masks that are applied to Micro QR codes to ensure that there are no regions with "structure" in them. Avoid
 * accidentally having patterns that look like a finder pattern or alignment pattern.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"UnnecessaryParentheses"})
public abstract class MicroQrCodeMaskPattern {

	public static final MicroQrCodeMaskPattern M00 = new M00();
	public static final MicroQrCodeMaskPattern M01 = new M01();
	public static final MicroQrCodeMaskPattern M10 = new M10();
	public static final MicroQrCodeMaskPattern M11 = new M11();

	int bits;

	public static List<MicroQrCodeMaskPattern> values() {
		List<MicroQrCodeMaskPattern> values = new ArrayList<>();
		values.add(MicroQrCodeMaskPattern.M00);
		values.add(MicroQrCodeMaskPattern.M01);
		values.add(MicroQrCodeMaskPattern.M10);
		values.add(MicroQrCodeMaskPattern.M11);
		return values;
	}

	protected MicroQrCodeMaskPattern( int bits ) {
		this.bits = bits;
	}

	/**
	 * Applies the mask to the specified bit. grid coordinates are relative to top left corner (0,0)
	 *
	 * @param row module row
	 * @param col module column
	 * @param bitValue value of the bit. 0 or 1
	 * @return value after masking has been applied
	 */
	public abstract int apply( int row, int col, int bitValue );

	public static MicroQrCodeMaskPattern lookupMask( int maskPattern ) {
		return switch (maskPattern) {
			case 0b00 -> M00;
			case 0b01 -> M01;
			case 0b10 -> M10;
			case 0b11 -> M11;
			default -> throw new RuntimeException("Unknown mask: " + maskPattern);
		};
	}

	public static MicroQrCodeMaskPattern lookupMask( String maskPattern ) {
		return switch (maskPattern) {
			case "00" -> M00;
			case "01" -> M01;
			case "10" -> M10;
			case "11" -> M11;
			default -> throw new RuntimeException("Unknown mask: " + maskPattern);
		};
	}

	static class NONE extends MicroQrCodeMaskPattern {
		public NONE( int bits ) {super(bits);}

		@Override public int apply( int row, int col, int bitValue ) {
			return bitValue;
		}
	}

	static class M00 extends MicroQrCodeMaskPattern {
		public M00() {super(0b00);}

		@Override public int apply( int row, int col, int bitValue ) {
			int mask = row%2;
			return bitValue ^ ((~mask) & 0x1);
		}
	}

	static class M01 extends MicroQrCodeMaskPattern {
		public M01() {super(0b01);}

		@Override public int apply( int row, int col, int bitValue ) {
			int mask = (row/2 + col/3)%2;
			return bitValue ^ ((~mask) & 0x1);
		}
	}

	static class M10 extends MicroQrCodeMaskPattern {
		public M10() {super(0b10);}

		@Override public int apply( int row, int col, int bitValue ) {
			return bitValue ^ (((row*col)%2 + (row*col)%3)%2 == 0 ? 1 : 0);
		}
	}

	static class M11 extends MicroQrCodeMaskPattern {
		public M11() {super(0b11);}

		@Override public int apply( int row, int col, int bitValue ) {
			int mask = ((row*col)%3 + (row + col)%2)%2;
			return bitValue ^ ((~mask) & 0x1);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
