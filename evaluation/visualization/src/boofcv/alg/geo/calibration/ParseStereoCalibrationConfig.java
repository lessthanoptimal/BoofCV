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
 * Configuration for stereo calibration
 *
 * @author Peter Abeles
 */
public class ParseStereoCalibrationConfig extends BaseCalibrationConfig {

	public List<String> leftImages = new ArrayList<String>();
	public List<String> rightImages = new ArrayList<String>();

	public ParseStereoCalibrationConfig(MediaManager media) {
		this.media = media;
	}

	public ParseStereoCalibrationConfig() {
	}

	public boolean parse( String fileName ) {
		Reader input = media.openFile(fileName);
		if( input == null )
			return false;

		BufferedReader reader = new BufferedReader(input);

		String infoFile = null;
		leftImages.clear();
		rightImages.clear();

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

				if( v[0].compareTo("addLeft") == 0 )
					leftImages.add(v[2]);
				else if( v[0].compareTo("addRight") == 0 )
					rightImages.add(v[2]);
			}

		} catch (IOException e) {
		}

		if( infoFile == null || rightImages.size() == 0 ||
				leftImages.size() != rightImages.size() )
			return false;

		try {
			parseTarget(infoFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	public List<String> getLeftImages() {
		return leftImages;
	}

	public List<String> getRightImages() {
		return rightImages;
	}
}