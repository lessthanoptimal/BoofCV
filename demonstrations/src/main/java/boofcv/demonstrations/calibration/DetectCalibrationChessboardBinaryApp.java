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

import boofcv.abst.fiducial.calib.ConfigChessboardBinary;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.filter.binary.BinaryContourFinder;
import boofcv.alg.fiducial.calib.chess.DetectChessboardBinaryPattern;
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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Visualizes {@link DetectChessboardBinaryPattern}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DetectCalibrationChessboardBinaryApp extends CommonDetectCalibrationApp {
	DetectChessboardBinaryPattern<GrayF32> alg;
	ConfigGridDimen configGrid;
	ConfigChessboardBinary configDet = new ConfigChessboardBinary();

	public DetectCalibrationChessboardBinaryApp( int numRows, int numColumns,
												 List<String> exampleInputs ) {
		super(exampleInputs);
		configGrid = new ConfigGridDimen(numRows, numColumns, 1);
		setUpGui(new DetectCalibrationPolygonPanel(numRows, numColumns, configDet.square, configDet.thresholding));

		declareDetector();
	}

	@Override
	public void declareDetector() {
		configDet.thresholding = ((DetectCalibrationPolygonPanel)controlPanel).polygonPanel.thresholdPanel.createConfig();
		configDet.square = ((DetectCalibrationPolygonPanel)controlPanel).polygonPanel.getConfigPolygon();
		configGrid.numRows = controlPanel.getGridRows();
		configGrid.numCols = controlPanel.getGridColumns();

		alg = FactoryFiducialCalibration.chessboardB(configDet, configGrid).getAlgorithm();
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
		return alg.getFindSeeds().getGraphs();
	}

	@Override
	protected List<PointIndex2D_F64> getCalibrationPoints() {
		return alg.getCalibrationPoints();
	}

	@Override
	protected List<Contour> getContours() {
		BinaryContourFinder contour = alg.getFindSeeds().getDetectorSquare().getDetector().getContourFinder();

		return BinaryImageOps.convertContours(contour);
	}

	@Override
	protected List<Polygon2D_F64> getFoundPolygons() {
		return alg.getFindSeeds().getDetectorSquare().getPolygons(null, null);
	}

	@Override
	protected List<EllipseRotated_F64> getFoundEllipses() {
		return new ArrayList<>();
	}

	@Override
	protected List<SquareGrid> getGrids() {
		return alg.getFindSeeds().getGrids().getGrids().toList();
	}

	public void configure( int numRows, int numCols ) {
		configGrid = new ConfigGridDimen(numRows, numCols, 1);
	}

	public static void main( String[] args ) throws FileNotFoundException {

		List<String> examples = new ArrayList<>();

		for (int i = 1; i <= 11; i++) {
			examples.add(UtilIO.pathExample(String.format("calibration/stereo/Bumblebee2_Chess/left%02d.jpg", i)));
		}
		examples.add(UtilIO.pathExample("fiducial/chessboard/movie.mjpeg"));

		SwingUtilities.invokeLater(() -> {
			DetectCalibrationChessboardBinaryApp app = new DetectCalibrationChessboardBinaryApp(7, 5, examples);

			app.openFile(new File(examples.get(0)));
			app.display("Chessboard (Binary) Detection");
		});
	}
}
