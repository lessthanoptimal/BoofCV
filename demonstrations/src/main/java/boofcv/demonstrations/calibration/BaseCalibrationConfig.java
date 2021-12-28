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

import boofcv.abst.fiducial.calib.ConfigChessboardX;
import boofcv.abst.fiducial.calib.ConfigGridDimen;
import boofcv.abst.geo.calibration.DetectSingleFiducialCalibration;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.io.MediaManager;
import boofcv.io.SimpleStringNumberReader;
import boofcv.io.wrapper.DefaultMediaManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

/**
 * Base class for planar calibration
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class BaseCalibrationConfig {

	MediaManager media = DefaultMediaManager.INSTANCE;

	public boolean assumeZeroSkew;
	public int numRadial;
	public boolean includeTangential;
	public DetectSingleFiducialCalibration detector;

	public BaseCalibrationConfig(MediaManager media) {
		this.media = media;
	}

	public BaseCalibrationConfig() {}

	protected void parseTarget( String where ) throws FileNotFoundException {
		Reader input = Objects.requireNonNull(media.openFile(where));

		SimpleStringNumberReader reader = new SimpleStringNumberReader('#');
		if( !reader.read(input) )
			throw new RuntimeException("Parsing configuration failed");

		if( reader.remainingTokens() < 7 )
			throw new RuntimeException("Not enough tokens in config file");

		String type = reader.nextString();
		numRadial = (int)reader.nextDouble();
		includeTangential = Boolean.parseBoolean(reader.nextString());
		assumeZeroSkew = Boolean.parseBoolean(reader.nextString());
		int numCols = (int)reader.nextDouble();
		int numRows = (int)reader.nextDouble();

		double width = reader.nextDouble();

		if( type.compareToIgnoreCase("square") == 0 ) {
			double space = reader.nextDouble();
			detector = FactoryFiducialCalibration.squareGrid(null,
					new ConfigGridDimen(numRows, numCols, width, space));
		} else if( type.compareToIgnoreCase("chess") == 0 ) {
			detector = FactoryFiducialCalibration.chessboardX((ConfigChessboardX)null,
					new ConfigGridDimen(numRows, numCols, width));
		} else {
			throw new RuntimeException("Unknown type: "+type);
		}

		try {
			input.close();
		} catch (IOException ignore) {}
	}

	public DetectSingleFiducialCalibration getDetector() {
		return detector;
	}
}
