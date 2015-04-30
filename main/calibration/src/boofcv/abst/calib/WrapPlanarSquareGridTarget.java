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

import boofcv.alg.feature.detect.InvalidCalibrationTarget;
import boofcv.alg.feature.detect.grid.DetectSquareCalibrationPoints;
import boofcv.alg.feature.detect.grid.RefineCalibrationGridCorner;
import boofcv.alg.feature.detect.grid.UtilCalibrationGrid;
import boofcv.alg.feature.detect.grid.refine.WrapRefineCornerSegmentFit;
import boofcv.alg.feature.detect.quadblob.OrderPointsIntoGrid;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapPlanarSquareGridTarget implements PlanarCalibrationDetector {

	int squareColumns;

	int pointColumns;
	int pointRows;

	RefineCalibrationGridCorner refine;
	DetectSquareCalibrationPoints detect;

	// binary image computed from the threshold
	private ImageUInt8 binary = new ImageUInt8(1,1);

	// set of found points
	List<Point2D_F64> ret;

	ImageFloat32 work1 = new ImageFloat32(1,1);
	ImageFloat32 work2 = new ImageFloat32(1,1);

	ConfigSquareGrid config;
	List<Point2D_F64> layoutPoints;

	public WrapPlanarSquareGridTarget( ConfigSquareGrid config ) {
		this.config = config;
		refine = new WrapRefineCornerSegmentFit();
//		refine = new WrapRefineCornerCanny();

		this.squareColumns = config.numCols;

		pointColumns = (squareColumns/2+1)*2;
		pointRows = (config.numRows/2+1)*2;

		double spaceToSquareRatio = config.spaceWidth/config.squareWidth;

		detect = new DetectSquareCalibrationPoints(config.relativeSizeThreshold,spaceToSquareRatio ,
				squareColumns,config.numRows);

		layoutPoints = createLayout(config.numCols,config.numRows,config.squareWidth,config.spaceWidth);
	}

	@Override
	public boolean process(ImageFloat32 input) {

		work1.reshape(input.width,input.height);
		work2.reshape(input.width,input.height);

		binary.reshape(input.width,input.height);

		if( config.binaryGlobalThreshold <= 0 ) {
			work1.reshape(input.width,input.height);
			work2.reshape(input.width,input.height);
			GThresholdImageOps.adaptiveSquare(input, binary, config.binaryAdaptiveRadius, config.binaryAdaptiveBias, true, work1, work2);
		} else
			GThresholdImageOps.threshold(input, binary, config.binaryGlobalThreshold, true);


		// detect the target at pixel level accuracy
		if( !detect.process(binary) )
			return false;
		try {
			List<QuadBlob> squares = detect.getInterestSquares();
		
			// refine the corner accuracy estimate to sub-pixel
			refine.refine(squares,input);

			List<Point2D_F64> subpixel = new ArrayList<Point2D_F64>();
			for( QuadBlob b : squares ) {
				for( Point2D_F64 p : b.subpixel )
					subpixel.add(p);
			}

			//TODO it was already ordered before subpixel.  can that be used again?
			OrderPointsIntoGrid orderAlg = new OrderPointsIntoGrid();
			List<Point2D_F64> ordered = orderAlg.process(subpixel);

			ret = UtilCalibrationGrid.rotatePoints(ordered,
					orderAlg.getNumRows(),orderAlg.getNumCols(),
					pointRows,pointColumns);
		} catch( InvalidCalibrationTarget e ) {
//			e.printStackTrace();
			return false;
		}

		return ret != null;

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
			double y = startY + i*(squareWidth+spaceWidth);

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
		return ret;
	}

	@Override
	public List<Point2D_F64> getLayout() {
		return layoutPoints;
	}

	public ImageUInt8 getBinary() {
		return binary;
	}

	public RefineCalibrationGridCorner getRefine() {
		return refine;
	}

	public DetectSquareCalibrationPoints getDetect() {
		return detect;
	}

}
