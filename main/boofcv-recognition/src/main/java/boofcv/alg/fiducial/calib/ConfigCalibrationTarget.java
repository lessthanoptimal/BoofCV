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

package boofcv.alg.fiducial.calib;

import boofcv.abst.fiducial.calib.CalibrationPatterns;
import boofcv.abst.fiducial.calib.ConfigECoCheckMarkers;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.factory.fiducial.ConfigHammingChessboard;
import boofcv.factory.fiducial.ConfigHammingGrid;
import boofcv.struct.Configuration;
import org.jetbrains.annotations.Nullable;

/**
 * Describes the calibration target.
 *
 * @author Peter Abeles
 */
public class ConfigCalibrationTarget implements Configuration {
	/** Which type of calibration target */
	@Nullable
	public CalibrationPatterns type = CalibrationPatterns.CHESSBOARD;

	/** Physical descrition of ECoCheck */
	public ConfigECoCheckMarkers ecocheck = new ConfigECoCheckMarkers();
	/** Physical description of hamming chessboard */
	public ConfigHammingChessboard hammingChess = new ConfigHammingChessboard();
	/** Physical description of hamming grid */
	public ConfigHammingGrid hammingGrid = new ConfigHammingGrid();
	/** Physical description of all regular grid patterns */
	public ConfigGridDimen grid = new ConfigGridDimen();

	public Configuration getActiveDescription() {
		if (type == null)
			return grid;
		return switch (type) {
			case ECOCHECK -> ecocheck;
			case HAMMING_CHESSBOARD -> hammingChess;
			case HAMMING_GRID -> hammingGrid;
			default -> grid;
		};
	}

	@Override public void checkValidity() {
		getActiveDescription().checkValidity();
	}

	public ConfigCalibrationTarget setTo( ConfigCalibrationTarget src ) {
		this.type = src.type;
		this.ecocheck.setTo(src.ecocheck);
		this.hammingChess.setTo(src.hammingChess);
		this.hammingGrid.setTo(src.hammingGrid);
		this.grid.setTo(src.grid);
		return this;
	}
}
