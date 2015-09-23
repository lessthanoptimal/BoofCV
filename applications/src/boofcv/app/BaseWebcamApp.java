/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

/**
 * @author Peter Abeles
 */
public class BaseWebcamApp {
	int cameraId=0;
	int desiredWidth=-1,desiredHeight=-1;

	String flagName;
	String parameters;

	protected boolean checkCameraFlag( String argument ) {
		splitFlag(argument);
		if( flagName.compareToIgnoreCase("Camera") == 0 ) {
			cameraId = Integer.parseInt(parameters);
			return true;
		} else if( flagName.compareToIgnoreCase("Resolution") == 0 ) {
			String words[] = parameters.split(":");
			if( words.length != 2 )throw new RuntimeException("Expected two for width and height");
			desiredWidth = Integer.parseInt(words[0]);
			desiredHeight = Integer.parseInt(words[1]);
			return true;
		} else {
			return false;
		}
	}

	protected void splitFlag( String word ) {
		int indexEquals = 2;
		for(; indexEquals < word.length(); indexEquals++ ) {
			if( word.charAt(indexEquals)=='=') {
				break;
			}
		}
		if(indexEquals == word.length() )
			throw new RuntimeException("Expected = inside of flag");

		flagName = word.substring(2,indexEquals);
		parameters = word.substring(indexEquals+1,word.length());
	}
}
