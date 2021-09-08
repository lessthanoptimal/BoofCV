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

package boofcv.gui.calibration;

import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckGenerator;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckUtils;
import boofcv.alg.fiducial.calib.hammingchess.HammingChessboardGenerator;
import boofcv.alg.fiducial.calib.hamminggrids.HammingGridGenerator;
import boofcv.factory.fiducial.ConfigHammingChessboard;
import boofcv.factory.fiducial.ConfigHammingGrid;
import boofcv.gui.FiducialRenderEngineGraphics2D;
import boofcv.gui.RenderCalibrationTargetsGraphics2D;

import java.awt.image.BufferedImage;

/**
 * Common utility functions for calibration UI.
 *
 * @author Peter Abeles
 */
public class UtilCalibrationGui {
	public static BufferedImage renderTargetBuffered( CalibrationPatterns type, Object config, int squareWidth ) {
		int circle = squareWidth/2;

		if (type == CalibrationPatterns.ECOCHECK) {
			ConfigECoCheckMarkers c = (ConfigECoCheckMarkers)config;

			ECoCheckUtils utils = new ECoCheckUtils();
			utils.codec.setErrorCorrectionLevel(c.errorCorrectionLevel);
			c.convertToGridList(utils.markers);
			utils.fixate();
			ConfigECoCheckMarkers.MarkerShape shape = c.markerShapes.get(0);

			int markerWidth = squareWidth*(shape.numCols - 1);
			int markerHeight = squareWidth*(shape.numRows - 1);

			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, squareWidth/2);

			ECoCheckGenerator generator = new ECoCheckGenerator(utils);
			generator.squareWidth = squareWidth;
			generator.setRender(render);
			generator.render(0);
			return render.getImage();
		} else if (type == CalibrationPatterns.HAMMING_CHESSBOARD) {
			ConfigHammingChessboard c = (ConfigHammingChessboard)config;

			int markerWidth = squareWidth*c.numCols;
			int markerHeight = squareWidth*c.numRows;

			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, squareWidth/2);

			HammingChessboardGenerator generator = new HammingChessboardGenerator(c);
			generator.squareWidth = squareWidth;
			generator.setRender(render);
			generator.render();
			return render.getImage();
		} else if (type == CalibrationPatterns.HAMMING_GRID) {
			ConfigHammingGrid c = (ConfigHammingGrid)config;

			int markerWidth = (int)Math.round(squareWidth*c.getMarkerWidth()/c.squareSize);
			int markerHeight = (int)Math.round(squareWidth*c.getMarkerHeight()/c.squareSize);

			FiducialRenderEngineGraphics2D render = configureRenderGraphics2D(markerWidth, markerHeight, squareWidth/2);

			var generator = new HammingGridGenerator(c);
			generator.squareWidth = squareWidth;
			generator.setRender(render);
			generator.render();
			return render.getImage();
		}

		final RenderCalibrationTargetsGraphics2D renderer = new RenderCalibrationTargetsGraphics2D(20, 1);

		if (type == CalibrationPatterns.CHESSBOARD) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			renderer.chessboard(c.numRows, c.numCols, squareWidth);
		} else if (type == CalibrationPatterns.SQUARE_GRID) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			double space = squareWidth*c.shapeDistance/c.shapeSize;
			renderer.squareGrid(c.numRows, c.numCols, squareWidth, space);
		} else if (type == CalibrationPatterns.CIRCLE_GRID) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			double space = circle*c.shapeDistance/c.shapeSize;
			renderer.circleRegular(c.numRows, c.numCols, circle, space);
		} else if (type == CalibrationPatterns.CIRCLE_HEXAGONAL) {
			ConfigGridDimen c = (ConfigGridDimen)config;
			double space = circle*c.shapeDistance/c.shapeSize;
			renderer.circleHex(c.numRows, c.numCols, circle, space);
		}
		return renderer.getBufferred();
	}

	private static FiducialRenderEngineGraphics2D configureRenderGraphics2D( int markerWidth, int markerHeight, int border ) {
		// Render the marker. Adjust marker size so that when the border is added it will match the paper size
		var render = new FiducialRenderEngineGraphics2D();
		render.configure(border, border, markerWidth, markerHeight);
		return render;
	}
}
