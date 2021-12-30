/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

/**
 * Information about a detected Micro QR Code.
 *
 * @author Peter Abeles
 */
public class MicroQrCode {
	/**
	 * Version of the marker. This can be from 1 to 4.
	 */
	public int version = -1;

	public ErrorLevel error = ErrorLevel.L;


	/** Error correction level */
	public enum ErrorLevel {
		/** Error detection only. */
		L(0b01),
		/** Error correction of about 15% */
		M(0b00),
		/** Error correction of about 25% */
		Q(0b11);

		ErrorLevel( int value ) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}

		public static ErrorLevel lookup( int value ) {
			return switch (value) {
				case 0b01 -> L;
				case 0b00 -> M;
				case 0b11 -> Q;
				default -> throw new IllegalArgumentException("Unknown");
			};
		}

		public static ErrorLevel lookup( String letter ) {
			return switch (letter) {
				case "L" -> L;
				case "M" -> M;
				case "Q" -> Q;
				default -> throw new IllegalArgumentException("Unknown");
			};
		}

		final int value;
	}
}
