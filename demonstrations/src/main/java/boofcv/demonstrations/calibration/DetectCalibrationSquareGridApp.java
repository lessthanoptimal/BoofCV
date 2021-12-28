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

package boofcv.demonstrations.calibration;

import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.fiducial.calib.ConfigSquareGrid;
import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.alg.fiducial.calib.grid.DetectSquareGridFiducial;
import boofcv.alg.fiducial.calib.squares.SquareGrid;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.UtilIO;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.shapes.Polygon2D_F64;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays detected square grid fiducials. Visualizes several of its processing steps making it easier to debug.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectCalibrationSquareGridApp extends CommonDetectCalibrationApp {
	DetectSquareGridFiducial<GrayF32> alg;
	ConfigSquareGrid configDet = new ConfigSquareGrid();
	ConfigGridDimen configGrid;

	public DetectCalibrationSquareGridApp( int numRows, int numColumns, double squareWidth, double spaceWidth,
										   boolean forCalibration,
										   List<String> exampleInputs ) {
		super(exampleInputs);
		configGrid = new ConfigGridDimen(numRows, numColumns, squareWidth, spaceWidth);
		setUpGui(new DetectCalibrationPolygonPanel(numRows, numColumns, configDet.square, configDet.thresholding));
		declareDetector();
	}

	@Override
	public void declareDetector() {
		configDet.thresholding = ((DetectCalibrationPolygonPanel)controlPanel).polygonPanel.getThresholdPanel().createConfig();
		configDet.square = ((DetectCalibrationPolygonPanel)controlPanel).polygonPanel.getConfigPolygon();

		configGrid.numRows = controlPanel.getGridRows();
		configGrid.numCols = controlPanel.getGridColumns();

		alg = FactoryFiducialCalibration.squareGrid(configDet, configGrid).getAlgorithm();
		reprocessImageOnly();
	}

	@Override
	protected boolean process( GrayF32 image ) {
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
	protected List<PointIndex2D_F64> getCalibrationPoints() {
		return alg.getCalibrationPoints();
	}

	@Override
	protected List<Contour> getContours() {

		BinaryContourFinder contour = alg.getDetectorSquare().getDetector().getContourFinder();

		List<Contour> contours = BinaryImageOps.convertContours(contour);

		return contours;
	}

	@Override
	protected List<Polygon2D_F64> getFoundPolygons() {
		return alg.getDetectorSquare().getPolygons(null, null);
	}

	@Override
	protected List<EllipseRotated_F64> getFoundEllipses() {
		return new ArrayList<>();
	}

	@Override
	protected List<SquareGrid> getGrids() {
		return alg.getGrids().getGrids();
	}

	public static void main( String[] args ) {

		List<String> examples = new ArrayList<>();

		for (int i = 1; i <= 11; i++) {
			examples.add(UtilIO.pathExample(String.format("calibration/stereo/Bumblebee2_Square/left%02d.jpg", i)));
		}
		examples.add(UtilIO.pathExample("fiducial/square_grid/movie.mp4"));

		SwingUtilities.invokeLater(() -> {
			DetectCalibrationSquareGridApp app = new DetectCalibrationSquareGridApp(4, 3, 1, 1, false, examples);

			app.openFile(new File(examples.get(0)));
			app.display("Square Grid Detector");
		});
	}
}
