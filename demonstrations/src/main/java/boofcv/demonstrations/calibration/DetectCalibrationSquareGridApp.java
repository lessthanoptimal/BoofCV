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

package boofcv.demonstrations.calibration;

import boofcv.abst.fiducial.calib.ConfigSquareGrid;
import boofcv.alg.fiducial.calib.grid.DetectSquareGridFiducial;
import boofcv.alg.fiducial.calib.squares.SquareGrid;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.filter.binary.Contour;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import georegression.struct.shapes.Polygon2D_F64;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays detected square grid fiducials.  Visualizes several of its processing steps making it easier to debug.
 *
 * @author Peter Abeles
 */
public class DetectCalibrationSquareGridApp extends CommonDetectCalibrationApp
{
	DetectSquareGridFiducial<GrayF32> alg;
	ConfigSquareGrid config;

	public DetectCalibrationSquareGridApp(int numRows , int numColumns ,  double squareWidth, double spaceWidth,
										  boolean forCalibration ,
										  List<String> exampleInputs) {
		super(new DetectCalibrationPanel(numRows,numColumns,true),exampleInputs);

		config = new ConfigSquareGrid(numRows, numColumns, squareWidth,spaceWidth);

		declareDetector();
	}

	@Override
	public void declareDetector() {
		if( controlPanel.isManual()) {
			config.thresholding.type = ThresholdType.FIXED;
			config.thresholding.fixedThreshold = controlPanel.getThresholdLevel();
		} else {
			config.thresholding.type = ThresholdType.LOCAL_SQUARE;
		}

		config.numRows = controlPanel.getGridRows();
		config.numCols = controlPanel.getGridColumns();

		alg = FactoryFiducialCalibration.squareGrid(config).getAlgorithm();
	}

	@Override
	protected boolean process(GrayF32 image) {
		return alg.process(image);
	}

	@Override
	protected GrayU8 getBinaryImage() {
		return alg.getBinary();
	}

	@Override
	protected List<List<SquareNode>> getClusters() {
		return alg.getClusters();
	}

	@Override
	protected List<Point2D_F64> getCalibrationPoints() {
		return alg.getCalibrationPoints();
	}

	@Override
	protected List<Contour> getContours() {
		return alg.getDetectorSquare().getAllContours();
	}

	@Override
	protected List<Polygon2D_F64> getFoundPolygons() {
		return alg.getDetectorSquare().getPolygons(null);
	}

	@Override
	protected List<EllipseRotated_F64> getFoundEllipses() {
		return new ArrayList<>();
	}

	@Override
	protected List<SquareGrid> getGrids() {
		return alg.getGrids().getGrids();
	}

	public static void main(String[] args) {

		List<String>  examples = new ArrayList<>();

		for (int i = 1; i <= 11; i++) {
			examples.add(UtilIO.pathExample(String.format("calibration/stereo/Bumblebee2_Square/left%02d.jpg", i)));
		}

		DetectCalibrationSquareGridApp app = new DetectCalibrationSquareGridApp(4, 3, 1,1,false,examples);

		app.openFile(new File(examples.get(0)));
		app.waitUntilInputSizeIsKnown();

		ShowImages.showWindow(app,"Square Grid Detector",true);
	}
}
