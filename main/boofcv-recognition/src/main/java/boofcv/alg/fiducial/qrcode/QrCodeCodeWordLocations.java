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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.fiducial.microqr.MicroQrCode;
import georegression.struct.point.Point2D_I32;
import org.ejml.data.BMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-computes which pixels in a QR code or Micro QR code are data bits or not. (0,0) is top left corner
 * of QR code. +x are columns and +y are rows. This is needed because there is no simple equation which
 * says what modules encode data and which ones do it.
 *
 * @author Peter Abeles
 */
public class QrCodeCodeWordLocations extends BMatrixRMaj {

	public List<Point2D_I32> bits = new ArrayList<>();

	public static QrCodeCodeWordLocations qrcode( int version ) {
		int numModules = QrCode.totalModules(version);
		int[] alignment = QrCode.VERSION_INFO[version].alignment;
		boolean hasVersion = version >= QrCode.VERSION_ENCODED_AT;

		var locations = new QrCodeCodeWordLocations(numModules);
		locations.featureMaskQrCode(numModules, alignment, hasVersion);
		locations.computeBitLocations(false);
		return locations;
	}

	public static QrCodeCodeWordLocations microqr( int version ) {
		int numModules = MicroQrCode.totalModules(version);

		var locations = new QrCodeCodeWordLocations(numModules);
		locations.featureMaskMicroQr(numModules);
		locations.computeBitLocations(true);
		return locations;
	}

	private QrCodeCodeWordLocations( int numModules ) {
		super(numModules, numModules);
	}

	/**
	 * Blocks out the location of features in the image. Needed for code world location extraction
	 */
	private void featureMaskQrCode( int numModules, int[] alignment, boolean hasVersion ) {
		// mark alignment patterns + format info
		markSquare(0, 0, 9);
		markRectangle(numModules - 8, 0, 9, 8);
		markRectangle(0, numModules - 8, 8, 9);

		// timing pattern
		markRectangle(8, 6, 1, numModules - 8 - 8);
		markRectangle(6, 8, numModules - 8 - 8, 1);

		// version info
		if (hasVersion) {
			markRectangle(numModules - 11, 0, 6, 3);
			markRectangle(0, numModules - 11, 3, 6);
		}

		// alignment patterns
		for (int i = 0; i < alignment.length; i++) {
			int row = alignment[i];

			for (int j = 0; j < alignment.length; j++) {
				if (i == 0 && j == 0)
					continue;
				if (i == alignment.length - 1 && j == 0)
					continue;
				if (i == 0 && j == alignment.length - 1)
					continue;

				int col = alignment[j];
				markSquare(row - 2, col - 2, 5);
			}
		}
	}

	/**
	 * Blocks out the location of features in the image that can't store data.
	 */
	private void featureMaskMicroQr( int numModules ) {
		// mark alignment patterns + format info
		markSquare(0, 0, 9);

		// timing pattern
		markRectangle(8, 0, 1, numModules - 8);
		markRectangle(0, 8, numModules - 8, 1);
	}

	private void markSquare( int row, int col, int width ) {
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < width; j++) {
				set(row + i, col + j, true);
			}
		}
	}

	private void markRectangle( int row, int col, int width, int height ) {
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				set(row + i, col + j, true);
			}
		}
	}

	/**
	 * Snakes through and specifies the location of each bit for all the code words in the grid.
	 * @param isMicro true if micro Qr code
	 */
	private void computeBitLocations( boolean isMicro ) {
		int N = numRows;
		int row = N - 1;
		int col = N - 1;
		int direction = -1;

		while (col > 0) {
			if (col == 6 && !isMicro)
				col -= 1;

			if (!get(row, col)) {
				bits.add(new Point2D_I32(col, row));
			}
			if (!get(row, col - 1)) {
				bits.add(new Point2D_I32(col - 1, row));
			}

			row += direction;

			if (row < 0 || row >= N) {
				direction = -direction;
				col -= 2;
				row += direction;
			}
		}
	}

	/**
	 * Returns number of data bits available.
	 */
	public int getTotalDataBits() {
		return numRows*numRows - sum();
	}
}
