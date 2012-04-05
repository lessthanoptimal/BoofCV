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

import boofcv.alg.geo.calibration.CalibrationPlanarGridZhang99;
import boofcv.alg.geo.calibration.ParametersZhang99;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.calibration.CalibrateUsingZhangData;
import georegression.struct.point.Point2D_F64;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Compute results using CommonsMath, which is a port of MinPack Levenbergh Marquardt
 *
 * @author Peter Abeles
 */
public class CalibrateUsingCommonsMath {

	
	public static void main( String args[] ) throws IOException {
		String base = "../data/evaluation/calibration/mono/PULNiX_CCD_6mm_Zhang/";

		CalibrateUsingZhangData app = new CalibrateUsingZhangData();

		app.setOptimizer(new CommonsMathLM());

		app.loadObservations(base+"data1.txt");
		app.loadObservations(base+"data2.txt");
		app.loadObservations(base+"data3.txt");
		app.loadObservations(base+"data4.txt");
		app.loadObservations(base+"data5.txt");

		app.loadModel(base+"Model.txt");

		
		System.out.println("Computing Calibration");
		app.process(false,2);
	}
}
