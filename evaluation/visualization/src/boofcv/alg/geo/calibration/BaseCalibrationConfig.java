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

package boofcv.alg.geo.calibration;

import boofcv.abst.calib.ConfigChessboard;
import boofcv.abst.calib.ConfigSquareGrid;
import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.factory.calib.FactoryPlanarCalibrationTarget;
import boofcv.io.MediaManager;
import boofcv.io.SimpleStringNumberReader;
import boofcv.io.wrapper.DefaultMediaManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

/**
 * Base class for planar calibration
 *
 * @author Peter Abeles
 */
public class BaseCalibrationConfig {

	MediaManager media = DefaultMediaManager.INSTANCE;

	public boolean assumeZeroSkew;
	public boolean flipY;
	public PlanarCalibrationDetector detector;
	public PlanarCalibrationTarget target;

	public BaseCalibrationConfig(MediaManager media) {
		this.media = media;
	}

	public BaseCalibrationConfig() {
	}

	protected void parseTarget( String where ) throws FileNotFoundException {
		Reader input = media.openFile(where);

		SimpleStringNumberReader reader = new SimpleStringNumberReader('#');
		if( !reader.read(input) )
			throw new RuntimeException("Parsing configuration failed");

		if( reader.remainingTokens() < 6 )
			throw new RuntimeException("Not enough tokens in config file");

		String type = reader.nextString();
		assumeZeroSkew = Boolean.parseBoolean(reader.nextString());
		flipY = Boolean.parseBoolean(reader.nextString());
		int numCols = (int)reader.nextDouble();
		int numRows = (int)reader.nextDouble();

		double width = reader.nextDouble();

		if( type.compareToIgnoreCase("square") == 0 ) {
			double space = reader.nextDouble();
			detector = FactoryPlanarCalibrationTarget.detectorSquareGrid(new ConfigSquareGrid(numCols, numRows));
			target = FactoryPlanarCalibrationTarget.gridSquare(numCols, numRows, width, space);
		} else if( type.compareToIgnoreCase("chess") == 0 ) {
			detector = FactoryPlanarCalibrationTarget.detectorChessboard(new ConfigChessboard(numCols, numRows));
			target = FactoryPlanarCalibrationTarget.gridChess(numCols,numRows,width);
		} else {
			throw new RuntimeException("Unknown type: "+type);
		}

		try {
			input.close();
		} catch (IOException e) {}
	}

	public PlanarCalibrationDetector getDetector() {
		return detector;
	}

	public PlanarCalibrationTarget getTarget() {
		return target;
	}
}
