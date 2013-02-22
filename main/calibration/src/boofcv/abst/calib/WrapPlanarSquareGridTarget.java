/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.feature.detect.grid.AutoThresholdCalibrationGrid;
import boofcv.alg.feature.detect.grid.DetectSquareCalibrationPoints;
import boofcv.alg.feature.detect.grid.RefineCalibrationGridCorner;
import boofcv.alg.feature.detect.grid.UtilCalibrationGrid;
import boofcv.alg.feature.detect.grid.refine.WrapRefineCornerSegmentFit;
import boofcv.alg.feature.detect.quadblob.OrderPointsIntoGrid;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import boofcv.struct.image.ImageFloat32;
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
	AutoThresholdCalibrationGrid autoThreshold;
	DetectSquareCalibrationPoints detect;

	// set of found points
	List<Point2D_F64> ret;

	OrderPointsIntoGrid orderAlg = new OrderPointsIntoGrid();

	public WrapPlanarSquareGridTarget( ConfigSquareGrid config ) {
		refine = new WrapRefineCornerSegmentFit();
//		refine = new WrapRefineCornerCanny();

		this.squareColumns = config.numCols;

		pointColumns = (squareColumns/2+1)*2;
		pointRows = (config.numRows/2+1)*2;

		detect = new DetectSquareCalibrationPoints(500,config.relativeSizeThreshold,squareColumns,config.numRows);
		autoThreshold = new AutoThresholdCalibrationGrid(config.binaryThreshold);
	}

	@Override
	public boolean process(ImageFloat32 input) {

		// detect the target at pixel level accuracy
		if( !autoThreshold.process(detect,input) )
			return false;
		try {
			List<QuadBlob> squares = detect.getInterestSquares();
		
			// refine the corner accuracy estimate to sub-pixel
			refine.refine(squares,input);

			List<Point2D_F64> unordered = new ArrayList<Point2D_F64>();
			for( QuadBlob b : squares ) {
				for( Point2D_F64 p : b.subpixel )
					unordered.add(p);
			}

			orderAlg.process(unordered);

			ret = UtilCalibrationGrid.rotatePoints(orderAlg.getOrdered(),
					orderAlg.getNumRows(),orderAlg.getNumCols(),
					pointRows,pointColumns);
		} catch( InvalidCalibrationTarget e ) {
//			e.printStackTrace();
			return false;
		}

		return ret != null;

	}

	@Override
	public List<Point2D_F64> getPoints() {
		return ret;
	}

	public AutoThresholdCalibrationGrid getAutoThreshold() {
		return autoThreshold;
	}

	public RefineCalibrationGridCorner getRefine() {
		return refine;
	}

	public DetectSquareCalibrationPoints getDetect() {
		return detect;
	}
}
