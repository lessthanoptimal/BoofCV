/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import com.github.sarxos.webcam.Webcam;

/**
 * @author Peter Abeles
 */
public class BaseStandardInputApp {

	protected InputType inputType = InputType.WEBCAM;

	protected int cameraId=0;
	protected String cameraName = null;
	protected int desiredWidth=-1,desiredHeight=-1;

	protected String filePath;

	protected String flagName;
	protected String parameters;

	protected void printInputHelp() {
		System.out.println("Camera Input:  (default)");
		System.out.println();
		System.out.println("  --Camera=<int|String>              Opens the specified camera using WebcamCapture ID");
		System.out.println("                                     or a device string.");
		System.out.println("                                     DEFAULT: Whatever WebcamCapture opens");
		System.out.println("  --Resolution=<width>:<height>      Specifies the image resolution.");
		System.out.println("                                     DEFAULT: Who knows or intrinsic, if specified");
		System.out.println();
		System.out.println("Image Input:");
		System.out.println();
		System.out.println("  --ImageFile=<path>                 Path to image file");
		System.out.println();
		System.out.println("Video Input:");
		System.out.println();
		System.out.println("  --VideoFile=<path>                 Path to video file");
	}

	protected boolean checkCameraFlag( String argument ) {
		splitFlag(argument);
		if( flagName.compareToIgnoreCase("Camera") == 0 ) {
			inputType = InputType.WEBCAM;
			try {
				cameraId = Integer.parseInt(parameters);
			} catch( NumberFormatException e ) {
				cameraId = -1;
				cameraName = parameters;
			}
			return true;
		} else if( flagName.compareToIgnoreCase("ImageFile") == 0 ) {
			inputType = InputType.IMAGE;
			filePath = parameters;
			return true;
		} else if( flagName.compareToIgnoreCase("videoFile") == 0 ) {
			inputType = InputType.VIDEO;
			filePath = parameters;
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

	protected Webcam openSelectedCamera() {
		Webcam webcam = cameraId >= 0 ? Webcam.getWebcams().get(cameraId) : Webcam.getWebcamByName(cameraName);

		if( webcam == null ) {
			if( cameraId >= 0 ) {
				System.err.println("Can't find camera with ID "+cameraId);
			} else {
				System.err.println("Can't find camera with name "+cameraName);
			}
			System.exit(-1);
		}
		return webcam;
	}

	protected String getCameraDeviceString() {
		if( cameraId >= 0 )
			return cameraId+"";
		else
			return cameraName;
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

	enum InputType {
		WEBCAM,
		IMAGE,
		VIDEO
	}
}
