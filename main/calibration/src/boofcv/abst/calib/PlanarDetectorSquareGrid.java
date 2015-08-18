/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.calib;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.feature.detect.grid.DetectSquareGridFiducial;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link PlanarCalibrationDetector} for square grid target types.
 *
 * @author Peter Abeles
 */
public class PlanarDetectorSquareGrid implements PlanarCalibrationDetector {


	DetectSquareGridFiducial<ImageFloat32> detect;

	List<Point2D_F64> layoutPoints;
	List<Point2D_F64> detected;

	public PlanarDetectorSquareGrid(ConfigSquareGrid config) {
		double spaceToSquareRatio = config.spaceWidth/config.squareWidth;

		InputToBinary<ImageFloat32> inputToBinary;

		if( config.binaryGlobalThreshold <= 0 ) {
			inputToBinary = FactoryThresholdBinary.adaptiveSquare(config.binaryAdaptiveRadius,0,true,ImageFloat32.class);
		} else {
			inputToBinary = FactoryThresholdBinary.globalFixed(config.binaryGlobalThreshold,true,ImageFloat32.class);
		}

		BinaryPolygonConvexDetector<ImageFloat32> detectorSquare =
				FactoryShapeDetector.polygon(inputToBinary,config.square,ImageFloat32.class);

		detect = new DetectSquareGridFiducial<ImageFloat32>(config.numRows/2+1,config.numCols/2+1,
				spaceToSquareRatio,detectorSquare);

		layoutPoints = createLayout(config.numCols,config.numRows,config.squareWidth,config.spaceWidth);
	}

	@Override
	public boolean process(ImageFloat32 input) {

		if( detect.process(input) )  {
			detected = new ArrayList<Point2D_F64>();
			List<Point2D_F64> found = detect.getCalibrationPoints();
			for (int i = 0; i < found.size(); i++) {
				detected.add( found.get(i).copy() );
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creates a target that is composed of squares.  The squares are spaced out and each corner provides
	 * a calibration point.
	 *
	 * @param numCols Number of column in each calibration target.  Must be odd.
	 * @param numRows Number of rows in calibration target. Must be odd.
	 * @param squareWidth How wide each square is. Units are target dependent.
	 * @param spaceWidth Distance between the sides on each square.  Units are target dependent.
	 * @return Target description
	 */
	public static List<Point2D_F64> createLayout( int numCols , int numRows , double squareWidth , double spaceWidth )
	{
		List<Point2D_F64> all = new ArrayList<Point2D_F64>();

		// modify the size so that it's just the number of black squares in the grid
		numCols = numCols/2 + 1;
		numRows = numRows/2 + 1;

		double width = (numCols*squareWidth + (numCols-1)*spaceWidth);
		double height = (numRows*squareWidth + (numRows-1)*spaceWidth);

		double startX = -width/2;
		double startY = -height/2;

		for( int i = numRows-1; i >= 0; i-- ) {
			// this will be on the top of the black in the row
			double y = startY + i*(squareWidth+spaceWidth)+squareWidth;

			List<Point2D_F64> top = new ArrayList<Point2D_F64>();
			List<Point2D_F64> bottom = new ArrayList<Point2D_F64>();

			for( int j = 0; j < numCols; j++ ) {
				double x = startX + j*(squareWidth+spaceWidth);

				top.add( new Point2D_F64(x,y));
				top.add( new Point2D_F64(x+squareWidth,y));
				bottom.add( new Point2D_F64(x,y-squareWidth));
				bottom.add( new Point2D_F64(x + squareWidth, y - squareWidth));
			}

			all.addAll(top);
			all.addAll(bottom);
		}

		return all;
	}

	@Override
	public List<Point2D_F64> getDetectedPoints() {
		return detected;
	}

	@Override
	public List<Point2D_F64> getLayout() {
		return layoutPoints;
	}

}
