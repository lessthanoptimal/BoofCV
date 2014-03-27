/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

	public WrapPlanarSquareGridTarget( ConfigSquareGrid config ) {
		this.config = config;
		refine = new WrapRefineCornerSegmentFit();
//		refine = new WrapRefineCornerCanny();

		this.squareColumns = config.numCols;

		pointColumns = (squareColumns/2+1)*2;
		pointRows = (config.numRows/2+1)*2;

		detect = new DetectSquareCalibrationPoints(config.relativeSizeThreshold,config.spaceToSquareRatio , squareColumns,config.numRows);
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

			ret = UtilCalibrationGrid.rotatePoints(subpixel,
					pointRows,pointColumns,
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
