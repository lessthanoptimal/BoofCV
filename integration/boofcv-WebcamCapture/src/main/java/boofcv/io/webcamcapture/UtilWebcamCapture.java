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

package boofcv.io.webcamcapture;

import com.github.sarxos.webcam.Webcam;

import java.awt.*;
import java.util.List;


/**
 * Utility functions related to Webcam capture
 *
 * @author Peter Abeles
 */
public class UtilWebcamCapture {

	/**
	 * Opens the default camera while adjusting its resolution
	 */
	public static Webcam openDefault( int desiredWidth , int desiredHeight) {
		Webcam webcam = Webcam.getDefault();

		// Webcam doesn't list all available resolutions. Just pass in a custom
		// resolution and hope it works
		adjustResolution(webcam,desiredWidth,desiredHeight);

		webcam.open();
		return webcam;
	}

	/**
	 * Searches for the first device which matches the pattern.  Webcam capture doesn't name devices
	 * using the standard "/dev/video0" scheme, but it includes that in its name.
	 *
	 * @param deviceName Partial or complete name of the device you wish to pen
	 * @return The webcam it found
	 */
	public static Webcam openDevice( String deviceName , int desiredWidth , int desiredHeight ) {
		Webcam webcam = findDevice(deviceName);
		if( webcam == null )
			throw new IllegalArgumentException("Can't find camera "+deviceName);
		adjustResolution(webcam,desiredWidth,desiredHeight);
		webcam.open();
		return webcam;
	}

	public static Webcam findDevice( String deviceName ) {
		List<Webcam> found = Webcam.getWebcams();

		for( Webcam cam : found ) {
			if( cam.getName().contains(deviceName)) {
				return cam;
			}
		}
		return null;
	}

	public static void adjustResolution( Webcam webcam , int desiredWidth , int desiredHeight ) {
		// Bug in the library where it doesn't list all the camera's possible resolutions
//		Dimension[] sizes = webcam.getViewSizes();
//		int bestError = Integer.MAX_VALUE;
//		Dimension best = sizes[0]; // to get rid of null warning
//		for( Dimension d : sizes ) {
//			int error = (d.width-desiredWidth)*(d.height-desiredHeight);
//			if( error < bestError ) {
//				bestError = error;
//				best = d;
//			}
//		}

		Dimension[] sizes = webcam.getCustomViewSizes();
		Dimension match = null;
		for( Dimension d : sizes ) {
			if( d.width == desiredWidth && d.height == desiredHeight ) {
				match = d;
				break;
			}
		}

		if( match == null ) {
			match = new Dimension(desiredWidth,desiredHeight);
			webcam.setCustomViewSizes(new Dimension[]{match});
		}

		webcam.setViewSize(match);
	}
}
