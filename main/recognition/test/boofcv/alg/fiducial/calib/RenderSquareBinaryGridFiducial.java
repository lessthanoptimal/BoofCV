/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the fiducial for use in unit tests
 *
 * @author Peter Abeles
 */
public class RenderSquareBinaryGridFiducial {

	public double border = 0.25;
	public int binaryGrid = 3;
	public int squareWidth = 40;


	public List<Quadrilateral_F64> expectedCorners = new ArrayList<>();

	public List<Point2D_F64> getOrderedExpectedPoints( int numRows, int numCols ) {

		List<Point2D_F64> points = new ArrayList<>();

		for (int row = 0; row < numRows; row++) {
			for (int col = 0; col < numCols; col++) {
				Quadrilateral_F64 q = expectedCorners.get(row*numCols+col);
				points.add(q.a);
				points.add(q.b);
			}
			for (int col = 0; col < numCols; col++) {
				Quadrilateral_F64 q = expectedCorners.get(row*numCols+col);
				points.add(q.d);
				points.add(q.c);
			}
		}
		return points;
	}

	public GrayF32 generate(int numRows, int numCols ) {
		expectedCorners.clear();

		int imageWidth = (numCols*2+2)*squareWidth;
		int imageHeight = (numRows*2+2)*squareWidth;

		GrayF32 image = new GrayF32(imageWidth,imageHeight);
		ImageMiscOps.fill(image, 255);

		int number = 0;
		for (int i = 0; i < numRows; i++) {
			int y0 = i*squareWidth*2 + squareWidth;
			for (int j = 0; j < numCols; j++) {
				int x0 = j*squareWidth*2 + squareWidth;

				generateSquare(x0,y0,squareWidth,number++,image);
			}
		}

		return image;
	}

	private void generateSquare( int x0 , int y0 , int width , int value , GrayF32 image ) {
		ImageMiscOps.fillRectangle(image,0,x0,y0,width,width);

		int innerWidth = (int)(width*(1.0-border*2)+0.5);

		int x1 = x0+(width-innerWidth)/2;
		int y1 = y0+(width-innerWidth)/2;

		expectedCorners.add(new Quadrilateral_F64(x0,y0,  x0+width,y0,  x0+width,y0+width, x0,y0+width));

		int bit = 0;
		for( int row = 0; row < binaryGrid; row++ ) {
			int pixelY0 = y1 + innerWidth-(row+1)*innerWidth/binaryGrid;
			int pixelY1 = y1 + innerWidth-(row)*innerWidth/binaryGrid;

			for (int col = 0; col < binaryGrid; col++) {
				int pixelX0 = x1 + col*innerWidth/binaryGrid;
				int pixelX1 = x1 + (col+1)*innerWidth/binaryGrid;

				boolean white;
				if( row ==0 && col == 0)
					white = false;
				else if ( row ==0 && col == binaryGrid-1)
					white = true;
				else if ( row ==binaryGrid-1 && col == binaryGrid-1)
					white = true;
				else if ( row ==binaryGrid-1 && col == 0)
					white = true;
				else {
					white = ((value >> bit) & 0x01) == 0;
					bit++;
				}

				if( white ) {
					ImageMiscOps.fillRectangle(image,255, pixelX0,pixelY0, pixelX1-pixelX0,pixelY1-pixelY0);
				}
			}
		}
	}
}
