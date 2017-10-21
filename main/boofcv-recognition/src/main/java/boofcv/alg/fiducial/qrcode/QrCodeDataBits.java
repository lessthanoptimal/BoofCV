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

import org.ejml.data.BMatrixRMaj;

/**
 * Pre-computes which pixels in a QR code are data bits or not. (0,0) is top left corner of QR code. +x are columns
 * and +y are rows. This is needed because there is no simple equation which says what modules encode data and which
 * ones do it.
 *
 * @author Peter Abeles
 */
public class QrCodeDataBits extends BMatrixRMaj{

	// how many bits are available to store data.
	public int dataBits;

	public QrCodeDataBits(int numModules , int alignment[] , boolean hasVersion ) {
		super(numModules,numModules);

		// mark alignment patterns + format info
		markSquare(0,0,9);
		markRectangle(numModules-8,0,9,8);
		markRectangle(0,numModules-8,8,9);

		// timing pattern
		markRectangle(8,6,1,numModules-8-8);
		markRectangle(6,8,numModules-8-8,1);

		// version info
		if( hasVersion ) {
			markRectangle(numModules-11,0,6,3);
			markRectangle(0,numModules-11,3,6);
		}

		// alignment patterns
		for (int i = 0; i < alignment.length; i++) {
			int row = alignment[i];

			for (int j = 0; j < alignment.length; j++) {
				if( i == 0 & j == 0 )
					continue;
				if( i == alignment.length-1 & j == 0)
					continue;
				if( i == alignment.length-1 & j == alignment.length-1)
					continue;

				int col = alignment[j];
				markSquare(col-2,numModules-row-3,5);
			}
		}

		dataBits = 0;
		for (int i = 0; i < data.length; i++) {
			if( !data[i] )
				dataBits++;
		}
	}

	private void markSquare( int row , int col , int width ) {
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < width; j++) {
				set(row+i,col+j,true);
			}
		}
	}

	private void markRectangle( int row , int col , int width , int height ) {
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				set(row+i,col+j,true);
			}
		}
	}
}
