/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
		adjustResolution(webcam,desiredWidth,desiredHeight);
		webcam.open();
		return webcam;
	}

	public static void adjustResolution( Webcam webcam , int desiredWidth , int desiredHeight ) {
		Dimension[] sizes = webcam.getViewSizes();
		int bestError = Integer.MAX_VALUE;
		Dimension best = null;
		for( Dimension d : sizes ) {
			int error = (d.width-desiredWidth)*(d.height-desiredHeight);
			if( error < bestError ) {
				bestError = error;
				best = d;
			}
		}
		webcam.setViewSize(best);
	}
}
