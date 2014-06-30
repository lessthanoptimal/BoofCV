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

package boofcv.examples;

import boofcv.io.UtilIO;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.calib.VisualDepthParameters;

/**
 * Loads an intrinsic parameters file for the RGB camera and creates a VisualDepthParameters
 * for the Kinect.
 *
 * @author Peter Abeles
 */
public class IntrinsicToDepthParameters {

	public static void main( String args[] ) {
		String baseDir = "../data/evaluation/kinect/";

		String nameCalib = baseDir+"intrinsic.xml";

		IntrinsicParameters intrinsic = UtilIO.loadXML(nameCalib);

		VisualDepthParameters depth = new VisualDepthParameters();

		depth.setVisualParam(intrinsic);
		depth.setMaxDepth(UtilOpenKinect.FREENECT_DEPTH_MM_MAX_VALUE);
		depth.setPixelNoDepth(UtilOpenKinect.FREENECT_DEPTH_MM_NO_VALUE);

		UtilIO.saveXML(depth, baseDir + "visualdepth.xml");
	}
}
