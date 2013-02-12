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

import boofcv.io.MediaManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a configuration file for {@link boofcv.abst.calib.CalibrateMonoPlanar}.
 * 
 * @author Peter Abeles
 */
public class ParseMonoCalibrationConfig extends BaseCalibrationConfig {


	public List<String> images = new ArrayList<String>();

	public ParseMonoCalibrationConfig(MediaManager media) {
		this.media = media;
	}

	public ParseMonoCalibrationConfig() {
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

	public List<String> getImages() {
		return images;
	}
}
