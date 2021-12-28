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

package boofcv.alg.fiducial.calib.hamminggrids;

import boofcv.alg.drawing.FiducialRenderEngine;
import boofcv.alg.fiducial.square.FiducialSquareHammingGenerator;
import boofcv.factory.fiducial.ConfigHammingGrid;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates hamming grids
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class HammingGridGenerator {
	/** How wide a checkerboard square is */
	public @Setter @Getter double squareWidth = 1.0;

	// used to draw the fiducial
	@Setter protected FiducialRenderEngine render;

	final ConfigHammingGrid config;

	// Used to render individual markers
	private final FiducialSquareHammingGenerator squareGenerator;

	// list of corners in ground truth
	public final List<Point2D_F64> corners = new ArrayList<>();

	public HammingGridGenerator( ConfigHammingGrid config ) {
		this.config = config;
		this.squareGenerator = new FiducialSquareHammingGenerator(config.markers);
	}

	public void render() {
		render.init();
		squareGenerator.setRenderer(render);

		double w = squareWidth*config.squareSize;
		double ws = w*(config.spaceToSquare + 1.0);
		squareGenerator.setMarkerWidth(w);

		int markerIndex = config.markerOffset;
		for (int row = 0; row < config.numRows; row++) {
			squareGenerator.offsetY = row*ws;
			for (int col = 0; col < config.numCols; col++, markerIndex++) {
				squareGenerator.offsetX = col*ws;
				squareGenerator.generateNoInit(markerIndex);
			}
		}

		saveCornerLocations();
	}

	public void saveCornerLocations() {
		corners.clear();
		final int rows = config.numRows;
		final int cols = config.numCols;
		double w = squareWidth*config.squareSize;
		double ws = w*(config.spaceToSquare + 1.0);

		for (int row = 0; row < rows; row++) {
			double y = (rows - row - 1)*ws + w;
			for (int col = 0; col < cols; col++) {
				double x = col*ws;
				corners.add(new Point2D_F64(x, y));
				corners.add(new Point2D_F64(x + w, y));
			}
			y -= w;
			for (int col = 0; col < cols; col++) {
				double x = col*ws;
				corners.add(new Point2D_F64(x, y));
				corners.add(new Point2D_F64(x + w, y));
			}
		}
	}
}
