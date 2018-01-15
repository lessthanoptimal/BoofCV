/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;

import java.util.List;

/**
 * Abstract class for creating qr codes. Contains the logic for rendering the QR Code but is missing
 * the actual renderer.
 *
 * @author Peter Abeles
 */
public abstract class QrCodeGenerator {

	QrCode qr;

	protected double markerWidth;

	// used to toggle rendering of data
	protected boolean renderData=true;

	// derived constants
	protected double moduleWidth;
	protected int numModules;

	// data mask
	protected List<Point2D_I32> bitLocations;

	public QrCodeGenerator( double markerWidth ) {
		this.markerWidth = markerWidth;
	}

	/**
	 * Generates a QR Code with the specified message. An exception is thrown if the message is
	 * too long to be encoded.
	 */
	public void render( QrCode qr ) {
		this.qr = qr;
		this.numModules = QrCode.totalModules(qr.version);
		this.moduleWidth = markerWidth/numModules;
		this.bitLocations = null;

		init();
		positionPattern(0,0, qr.ppCorner);
		positionPattern((numModules-7)*moduleWidth,0, qr.ppRight);
		positionPattern(0,(numModules-7)*moduleWidth, qr.ppDown);

		timingPattern(7*moduleWidth,6*moduleWidth,moduleWidth,0);
		timingPattern(6*moduleWidth,7*moduleWidth,0,moduleWidth);

		formatInformation();

		if( qr.version >= QrCode.VERSION_ENCODED_AT)
			versionInformation();

		// render alignment patterns

		int alignment[] = QrCode.VERSION_INFO[qr.version].alignment;
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
				alignmentPattern(col,numModules-row-1);
			}
		}

		if( renderData ) {

			if( qr.rawbits.length != QrCode.VERSION_INFO[qr.version].codewords )
				throw new RuntimeException("Unexpected length of raw data.");

			// mark which modules can store data
			bitLocations = new QrCodeCodeWordLocations(qr.version).bits;

			int numBytes = bitLocations.size() / 8;
			if (numBytes != qr.rawbits.length)
				throw new RuntimeException("Egads. unexpected length of qrcode raw data");

			// Render the output data
			renderData();
		}

		qr.bounds.set(0,0,0);
		qr.bounds.set(1,markerWidth,0);
		qr.bounds.set(2,markerWidth,markerWidth);
		qr.bounds.set(3,0,markerWidth);
	}

	/**
	 * Renders the raw data bit output while applying the selected mask
	 */
	private void renderData() {
		QrCodeMaskPattern mask = qr.mask;
		int count = 0;

		int length = bitLocations.size() - bitLocations.size()%8;
		while( count < length ) {
			int bits = qr.rawbits[count/8]&0xFF;

			int N = Math.min(8,bitLocations.size()-count);

			for (int i = 0; i < N; i++) {
				Point2D_I32 coor = bitLocations.get(count+i);
				int value = mask.apply(coor.y,coor.x, ((bits >> i ) & 0x01));
//				int value = ((bits >> i ) & 0x01);
				if( value > 0 ) {
					square(coor.y,coor.x);
				}
			}
			count += 8;
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
			square(x+i*slopeX,y+i*slopeY,moduleWidth);
		}
	}

	static PackedBits32 formatInformationBits( QrCode qr ) {
		PackedBits32 bits = new PackedBits32(15);
		bits.data[0] = QrCodePolynomialMath.encodeFormatBits(qr.error,qr.mask.bits);
		bits.data[0] ^= QrCodePolynomialMath.FORMAT_MASK;
		return bits;
	}

	private void formatInformation() {
		PackedBits32 bits = formatInformationBits(qr);
//		System.out.println("encoder format bits "+Integer.toBinaryString(bits.data[0]));

		for (int i = 0; i < 15; i++) {
			if( bits.get(i)==0) {
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
		}
		square(numModules-8,8);
	}

	private void versionInformation() {
		PackedBits32 bits = new PackedBits32(18);
		bits.data[0] = QrCodePolynomialMath.encodeVersionBits(qr.version);
//		System.out.println("encoder version bits "+Integer.toBinaryString(bits.data[0]));

		for (int i = 0; i < 18; i++) {
			if( bits.get(i)==0) {
				continue;
			}

			int row = i/3;
			int col = i%3;

			// top right
			square(row,numModules-11+col);
			// bottom left
			square(numModules-11+col,+row);
		}
	}

	private void alignmentPattern(int gridX , int gridY ) {

		double x = (gridX-2)*moduleWidth;
		double y = (gridY-2)*moduleWidth;

		square(x,y,moduleWidth*5, moduleWidth);
		square(x + moduleWidth*2,y + moduleWidth*2, moduleWidth);

		QrCode.Alignment a = qr.alignment.grow();
		a.moduleX = gridX;
		a.moduleY = gridY;
		a.pixel.set( (gridX + 0.5)*moduleWidth , (gridY + 0.5)*moduleWidth );
	}

	private void square( int row , int col ) {
		square(col*moduleWidth,row*moduleWidth,moduleWidth);
	}

	public abstract void init();

	public abstract void square(double x0 , double y0 , double width );

	public abstract void square(double x0, double y0, double width0, double thickness);

}
