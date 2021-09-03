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

package boofcv.alg.fiducial.square;

import boofcv.abst.distort.FDistort;
import boofcv.alg.drawing.FiducialImageGenerator;
import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.image.GrayU8;
import lombok.Getter;
import lombok.Setter;

/**
 * Generates images of square fiducials
 *
 * @author Peter Abeles
 */
public class FiducialSquareGenerator extends FiducialImageGenerator {
	/** length of the white border surrounding the fiducial */
	@Getter @Setter double whiteBorderDoc = 0;
	/** length of the black border */
	@Getter @Setter double blackBorder = 0.25;

	public FiducialSquareGenerator( FiducialRenderEngine renderer ) {
		this.renderer = renderer;
	}

	public void generate( GrayU8 image ) {
		renderer.init();

		// make sure the image is square and divisible by 8
		int s = image.width - (image.width%8);
		if (image.width != s || image.height != s) {
			GrayU8 tmp = new GrayU8(s, s);
			new FDistort(image, tmp).scaleExt().apply();
			image = tmp;
		}

		GrayU8 binary = ThresholdImageOps.threshold(image, null, 125, false);

		PixelMath.multiply(binary, 255, binary);

		double whiteBorder = whiteBorderDoc/markerWidth;
		double X0 = whiteBorder + blackBorder;
		double Y0 = whiteBorder + blackBorder;

		drawBorder();

		// Draw the image inside
		draw(image, X0, Y0, 1.0 - X0, 1.0 - Y0);
	}

	void drawBorder() {
		double whiteBorder = whiteBorderDoc/markerWidth;
		double X0 = whiteBorder + blackBorder;
		double Y0 = whiteBorder + blackBorder;

		// top and bottom
		rectangle(whiteBorder, whiteBorder, 1.0 - whiteBorder, Y0);
		rectangle(whiteBorder, 1.0 - Y0, 1.0 - whiteBorder, 1.0 - whiteBorder);

		// left and right sides
		rectangle(whiteBorder, Y0, X0, 1.0 - Y0);
		rectangle(1.0 - X0, Y0, 1.0 - whiteBorder, 1.0 - Y0);
	}

	/**
	 * Renders a binary square fiducial
	 *
	 * @param value Value encoded in the fiducial
	 * @param gridWidth number of cells wide the grid is
	 */
	public void generate( long value, int gridWidth ) {
		renderer.init();

		drawBorder();

		double whiteBorder = whiteBorderDoc/markerWidth;
		double X0 = whiteBorder + blackBorder;
//		double Y0 = whiteBorder+blackBorder;

		double bw = (1.0 - 2*X0)/gridWidth;

		// Draw the black corner used to ID the orientation
		square(X0, 1.0 - whiteBorder - blackBorder - bw, bw);

		final int bitCount = gridWidth*gridWidth - 4;
		for (int j = 0; j < bitCount; j++) {
			if ((value & (1L << j)) != 0) {
				box(bw, j, gridWidth);
			}
		}

		//		int s2 = (int)Math.round(ret.width*borderFraction);
//		int s5 = s2+square*(gridWidth-1);
//
//		int N = gridWidth*gridWidth-4;
//		for (int i = 0; i < N; i++) {
//			if( (value& (1<<i)) != 0 )
//				continue;
//
//			int where = index(i, gridWidth);
//			int x = where%gridWidth;
//			int y = gridWidth-1-(where/gridWidth);
//
//			x = s2 + square*x;
//			y = s2 + square*y;
//
//			ImageMiscOps.fillRectangle(ret,0xFF,x,y,square,square);
//		}
//		ImageMiscOps.fillRectangle(ret,0xFF,s2,s2,square,square);
//		ImageMiscOps.fillRectangle(ret,0xFF,s5,s5,square,square);
//		ImageMiscOps.fillRectangle(ret,0xFF,s5,s2,square,square);
	}

	private void box( double boxWidth, final int bit, int gridWidth ) {

		double whiteBorder = whiteBorderDoc/markerWidth;
		double X0 = whiteBorder + blackBorder;
		double Y0 = whiteBorder + blackBorder;

		int transitionBit0 = gridWidth - 3;
		int transitionBit1 = transitionBit0 + gridWidth*(gridWidth - 2);
		int transitionBit2 = transitionBit1 + gridWidth - 2;

		final int adjustedBit;
		if (bit <= transitionBit0)
			adjustedBit = bit + 1;
		else if (bit <= transitionBit1)
			adjustedBit = bit + 2;
		else if (bit <= transitionBit2)
			adjustedBit = bit + 3;
		else
			throw new RuntimeException("Bit must be between 0 and " + transitionBit2);

		int x = adjustedBit%gridWidth;
		int y = gridWidth - (adjustedBit/gridWidth) - 1;
		square(X0 + x*boxWidth, Y0 + y*boxWidth, boxWidth);
	}

	private void draw( GrayU8 image, double x0, double y0, double x1, double y1 ) {
		renderer.draw(image, U(x0), U(y0), U(x1), U(y1));
	}

	private void rectangle( double x0, double y0, double x1, double y1 ) {
		renderer.rectangle(U(x0), U(y0), U(x1), U(y1));
	}

	private void square( double x0, double y0, double width ) {
		renderer.square(U(x0), U(y0), U(width));
	}

	/**
	 * Convert from fractional unit to document unit
	 */
	double U( double f ) {
		return f*markerWidth;
	}
}
