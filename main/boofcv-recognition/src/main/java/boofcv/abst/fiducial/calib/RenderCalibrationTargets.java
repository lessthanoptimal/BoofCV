/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.fiducial.calib;

import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic class for rendering calibration targets
 *
 * @author Peter Abeles
 */
public abstract class RenderCalibrationTargets {

	/**
	 * Where calibration landmarks are located in the rendered document
	 */
	@Getter	List<Point2D_F64> landmarks = new ArrayList<>();

	public void chessboard( int rows , int cols , double squareWidth ) {
		specifySize(squareWidth*cols,squareWidth*rows);

		for (int i = 0; i < rows; i++) {
			double y = i*squareWidth;

			int startJ = i%2 == 0 ? 0 : 1;
			for (int j = startJ; j < cols; j += 2) {
				double x = j * squareWidth;
				drawSquare(x,y,squareWidth);
			}
		}

		landmarks.clear();
		for (int i = 1; i < rows; i++) {
			double y = i*squareWidth;
			for (int j = 1; j < cols; j ++) {
				double x = j * squareWidth;
				Point2D_F64 a = new Point2D_F64();
				markerToTarget(x,y,a);
				landmarks.add( a );
			}
		}
	}

	public void squareGrid( int rows , int cols , double squareWidth, double spaceWidth ) {
		specifySize(squareWidth*cols,squareWidth*rows);

		double targetWidth = cols*squareWidth + (cols-1)*spaceWidth;
		double targetHeight = rows*squareWidth + (rows-1)*spaceWidth;

		specifySize(targetWidth,targetHeight);

		for (int i = 0; i < rows; i++ ){
			double y =  i * (squareWidth + spaceWidth);

			for (int j = 0; j < cols; j++) {
				double x = j * (squareWidth + spaceWidth);

				drawSquare(x,y,squareWidth);
			}
		}
	}

	public void circleHex( int rows, int cols , double circleDiameter, double centerDistance ) {
		double spaceX = centerDistance/2.0;
		double spaceY = centerDistance*Math.sin(UtilAngle.radian(60));
		double radius = circleDiameter/2.0;

		double imageWidth = (cols-1)*spaceX + 2*radius;
		double imageHeight = (rows-1)*spaceY + 2*radius;

		specifySize(imageWidth,imageHeight);

		for (int row = 0; row < rows; row++) {
			double y = radius + (rows-row-1)*spaceY;
			for (int col = row%2; col < cols; col += 2) {
				double x = radius + col*spaceX;

				drawCircle(x,y,circleDiameter);
			}
		}
	}

	public void circleRegular( int rows , int cols , double circleDiameter , double centerDistance ) {
		double imageWidth =  (cols-1)*centerDistance + circleDiameter;
		double imageHeight = (rows-1)*centerDistance + circleDiameter;
		specifySize(imageWidth,imageHeight);

		double radius = circleDiameter/2;

		for (int row = 0; row < rows; row++) {
			double y = radius + row*centerDistance;
			for (int col = 0; col < cols; col++) {
				double x = radius + col*centerDistance;

				drawCircle(x,y,circleDiameter);
			}
		}
	}

	public abstract void specifySize( double width , double height );

	public abstract void markerToTarget(double x , double y , Point2D_F64 p );

	public abstract void drawSquare( double x , double y , double width );

	public abstract void drawCircle( double cx , double cy , double diameter );

}
