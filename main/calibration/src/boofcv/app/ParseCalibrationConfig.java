/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.app;

import boofcv.alg.geo.calibration.FactoryPlanarCalibrationTarget;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.io.MediaManager;
import boofcv.io.SimpleStringNumberReader;
import boofcv.io.wrapper.DefaultMediaManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a configuration file for {@link CalibrateMonoPlanarApp}.
 * 
 * @author Peter Abeles
 */
public class ParseCalibrationConfig {

	MediaManager media = DefaultMediaManager.INSTANCE;

	public boolean adjustLeftToRight;
	public PlanarCalibrationDetector detector;
	public PlanarCalibrationTarget target;
	public List<String> images = new ArrayList<String>();

	public ParseCalibrationConfig(MediaManager media) {
		this.media = media;
	}

	public ParseCalibrationConfig() {
	}

	public boolean parse( String fileName ) {
		Reader input = media.openFile(fileName);
		if( input == null )
			return false;

		BufferedReader reader = new BufferedReader(input);

		String infoFile = null;
		images.clear();

		try {
			String line = reader.readLine();
			// skip over comments
			while( line != null && line.charAt(0) == '#' )
				line = reader.readLine();

			if( line == null )
				return false;

			String[]v = line.split("\\s");
			if( v.length != 3 )
				return false;

			infoFile = v[2];

			while( true ) {
				line = reader.readLine();
				if( line == null )
					break;
				
				v = line.split("\\s");
				if( v.length != 3 )
					continue;
				images.add(v[2]);
			}

		} catch (IOException e) {
		}
		
		if( infoFile == null || images.size() == 0 )
			return false;

		try {
			parseTarget(infoFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		return true;
	}

	private void parseTarget( String where ) throws FileNotFoundException {
		Reader input = media.openFile(where);

		SimpleStringNumberReader reader = new SimpleStringNumberReader('#');
		if( !reader.read(input) )
			throw new RuntimeException("Parsing configuration failed");

		if( reader.remainingTokens() < 5 )
			throw new RuntimeException("Not enough tokens in config file");

		String type = reader.nextString();
		adjustLeftToRight = Boolean.parseBoolean(reader.nextString());
		int numCols = (int)reader.nextDouble();
		int numRows = (int)reader.nextDouble();

		double width = reader.nextDouble();

		if( type.compareToIgnoreCase("square") == 0 ) {
			double space = reader.nextDouble();
			detector = new WrapPlanarGridTarget(numCols,numRows);
			target = FactoryPlanarCalibrationTarget.gridSquare(numCols, numRows, width, space);
		} else if( type.compareToIgnoreCase("chess") == 0 ) {
			detector = new WrapPlanarChessTarget(numCols,numRows,4);
			target = FactoryPlanarCalibrationTarget.gridChess(numCols,numRows,width);
		} else {
			throw new RuntimeException("Unknown type: "+type);
		}

		try {
			input.close();
		} catch (IOException e) {}
	}

	public List<String> getImages() {
		return images;
	}

	public PlanarCalibrationDetector getDetector() {
		return detector;
	}

	public PlanarCalibrationTarget getTarget() {
		return target;
	}
}
