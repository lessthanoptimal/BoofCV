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

import georegression.struct.shapes.Polygon2D_F64;

/**
 * Abstract class for creating qr codes. Contains the logic for rendering the QR Code but is missing
 * the actual renderer.
 *
 * @author Peter Abeles
 */
public abstract class QrCodeGenerator {

	int version;
	double markerWidth;

	// derived constants
	double moduleWidth;
	int numModules;

	// Symbolic description of the qrcode just created. used for testing purposes
	public QrCode qr = new QrCode();

	public QrCodeGenerator( int version , double markerWidth ) {
		this.version = version;
		this.markerWidth= markerWidth;
		this.numModules = version*4+17;
		this.moduleWidth = markerWidth/numModules;
	}

	/**
	 * Generates a QR Code with the specified message. An exception is thrown if the message is
	 * too long to be encoded.
	 *
	 * @param message Message encoded in qr-code
	 */
	public void generate( String message ) {
		init();
		positionPattern(0,0, qr.ppCorner);
		positionPattern((numModules-7)*moduleWidth,0, qr.ppRight);
		positionPattern(0,(numModules-7)*moduleWidth, qr.ppDown);

		timingPattern(7*moduleWidth,6*moduleWidth,moduleWidth,0);
		timingPattern(6*moduleWidth,7*moduleWidth,0,moduleWidth);

		formatInformation();

		if( version == 1 ) {
		} else if( version <= 6 ) {
			// TODO create table that can be specified for all version and create generic code
			int x = numModules-6;
			int y = 10+4*version;
			alignmentPattern(x,y);
		} else {
			throw new RuntimeException("Add support");
		}
	}

	private void positionPattern(double x , double y , Polygon2D_F64 where ) {
		// draw the outside square
		square(x,y,moduleWidth*7, moduleWidth);

		// draw the inside square
		square(x+ moduleWidth *2,y+ moduleWidth *2, moduleWidth*3);

		where.get(0).set(x,y);
		where.get(1).set(x+moduleWidth*7,y);
		where.get(2).set(x+moduleWidth*7,y+moduleWidth*7);
		where.get(3).set(x,y+moduleWidth*7);
	}

	private void timingPattern( double x , double y, double slopeX , double slopeY ) {
		int length = numModules-7*2;

		for (int i = 1; i < length; i += 2) {
			square(x+i*slopeX,y+i*slopeY,moduleWidth,moduleWidth);
		}
	}

	private void formatInformation() {
		PackedBits bits = new PackedBits(15);
		bits.data[0] = QrCodePolynomialMath.encodeFormatBits(qr.errorCorrection,qr.maskPattern);
		bits.data[0] ^= QrCodePolynomialMath.FORMAT_MASK;

		for (int i = 0; i < 15; i++) {
			if( bits.get(i)==1) {
				continue;
			}
			if( i < 6 ) {
				square(i,8);
			} else if( i < 8) {
				square(i+1,8);
			} else if( i == 8 ) {
				square( 8,7);
			} else {
				square( 8,14-i);
			}

			if( i < 8 ) {
				square(8,numModules-i-1);
			} else {
				square(numModules-(15-i),8);
			}
			square(numModules-8,8);
		}
	}

	private void alignmentPattern(int gridX , int gridY ) {

		double x = (gridX-2)*moduleWidth;
		double y = (gridY-2)*moduleWidth;

		square(x,y,moduleWidth*5, moduleWidth);
		square(x + moduleWidth*2,y + moduleWidth*2, moduleWidth);
	}

	private void square( int row , int col ) {
		square(col*moduleWidth,row*moduleWidth,moduleWidth);
	}

	public abstract void init();

	public abstract void square(double x0 , double y0 , double width );

	public abstract void square(double x0, double y0, double width0, double thickness);


}
