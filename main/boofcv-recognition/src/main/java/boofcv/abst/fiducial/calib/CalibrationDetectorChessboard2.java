/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.feature.detect.chess.DetectChessboardCorners;
import boofcv.alg.feature.detect.chess.DetectChessboardCornersPyramid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterFinder;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Detector for chessboard calibration targets. Returns the first chessboard which is detected and matches
 * the expected size
 * 
 * @author Peter Abeles
 */
public class CalibrationDetectorChessboard2 implements DetectorFiducialCalibration {

	DetectChessboardCornersPyramid detector = new DetectChessboardCornersPyramid();
	ChessboardCornerClusterFinder clusterFinder = new ChessboardCornerClusterFinder();
	ChessboardCornerClusterToGrid clusterToGrid = new ChessboardCornerClusterToGrid();

	int cornerRows,cornerCols;
	ChessboardCornerClusterToGrid.GridInfo info = new ChessboardCornerClusterToGrid.GridInfo();

	List<Point2D_F64> layoutPoints;
	CalibrationObservation detected;

	public CalibrationDetectorChessboard2(ConfigChessboard2 config) {

		cornerRows = config.numRows-1;
		cornerCols = config.numCols-1;

		// the user is unlikely to set this value correctly
		config.threshold.maxPixelValue = DetectChessboardCorners.GRAY_LEVELS;

		InputToBinary<GrayF32> thresholder = FactoryThresholdBinary.threshold(config.threshold,GrayF32.class);

		detector.getDetector().setThresholding(thresholder);
		detector.getDetector().setKernelRadius(config.cornerRadius);
		detector.getDetector().setCornerIntensityThreshold(config.cornerThreshold);
		detector.setPyramidTopSize(config.pyramidTopSize);

		layoutPoints = gridChess(config.numRows, config.numCols, config.squareWidth);

		clusterToGrid.setCheckShape((r,c)->r==cornerRows&&c==cornerCols);
		clusterToGrid.setVerbose(System.out);
	}

	@Override
	public boolean process(GrayF32 input) {
		System.out.println("===========================================================");
		detected = new CalibrationObservation(input.width, input.height);
		try {
			detector.process(input);
			clusterFinder.process(detector.getCorners().toList());
		} catch( RuntimeException e ) {
			e.printStackTrace();
			return false;
		}

		FastQueue<ChessboardCornerGraph> clusters = clusterFinder.getOutputClusters();

		System.out.println("corners "+detector.getCorners().size);
		System.out.println("total clusters "+clusters.size);

		for (int clusterIdx = 0; clusterIdx < clusters.size; clusterIdx++) {
			ChessboardCornerGraph c = clusters.get(clusterIdx);

			if( c.corners.size > 1 )
				System.out.println(" ["+clusterIdx+"] corners.size = "+c.corners.size+"  vs "+(cornerCols*cornerRows));
			if (c.corners.size != cornerCols * cornerRows)
				continue;

			if (clusterToGrid.convert(c, info)) {
				if (info.cols != cornerCols || info.rows != cornerRows)
					continue;

				for (int i = 0; i < info.nodes.size(); i++) {
					detected.add(info.nodes.get(i), i);
				}
				return true;
			}
		}

		return false;
	}

	@Override
	public CalibrationObservation getDetectedPoints() {
		return detected;
	}

	@Override
	public List<Point2D_F64> getLayout() {
		return layoutPoints;
	}

	@Override
	public void setLensDistortion(LensDistortionNarrowFOV distortion, int width, int height) {
		// TODO apply undistortion to found corners
		throw new RuntimeException("SUPPORT!");
	}

	public DetectChessboardCornersPyramid getDetector() {
		return detector;
	}

	public ChessboardCornerClusterFinder getClusterFinder() {
		return clusterFinder;
	}

	public ChessboardCornerClusterToGrid getClusterToGrid() {
		return clusterToGrid;
	}

	public int getCornerRows() {
		return cornerRows;
	}

	public int getCornerCols() {
		return cornerCols;
	}

	/**
	 * This target is composed of a checkered chess board like squares.  Each corner of an interior square
	 * touches an adjacent square, but the sides are separated.  Only interior square corners provide
	 * calibration points.
	 *
	 * @param numRows Number of grid rows in the calibration target
	 * @param numCols Number of grid columns in the calibration target
	 * @param squareWidth How wide each square is.  Units are target dependent.
	 * @return Target description
	 */
	public static List<Point2D_F64> gridChess(int numRows, int numCols, double squareWidth)
	{
		List<Point2D_F64> all = new ArrayList<>();

		// convert it into the number of calibration points
		numCols = numCols - 1;
		numRows = numRows - 1;

		// center the grid around the origin. length of a size divided by two
		double startX = -((numCols-1)*squareWidth)/2.0;
		double startY = -((numRows-1)*squareWidth)/2.0;

		for( int i = numRows-1; i >= 0; i-- ) {
			double y = startY+i*squareWidth;
			for( int j = 0; j < numCols; j++ ) {
				double x = startX+j*squareWidth;
				all.add( new Point2D_F64(x,y));
			}
		}

		return all;
	}
}
