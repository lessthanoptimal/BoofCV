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

package boofcv.calibration;

import boofcv.abst.calib.CalibrateMonoPlanar;
import boofcv.abst.calib.ImageResults;
import boofcv.alg.geo.calibration.CalibrationPlanarGridZhang99;
import boofcv.alg.geo.calibration.PlanarCalibrationTarget;
import boofcv.alg.geo.calibration.Zhang99Parameters;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.optimization.UnconstrainedLeastSquares;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validate camera calibration code using calibration points provided by the original author Zhang.
 *
 * http://research.microsoft.com/en-us/um/people/zhang/Calib/#Calibration
 *
 * @author Peter Abeles
 */
public class CalibrateUsingZhangData {
	PlanarCalibrationTarget target;
	List<List<Point2D_F64>> observations = new ArrayList<List<Point2D_F64>>();
	Zhang99Parameters found;

	UnconstrainedLeastSquares optimizer;
	
	public void loadModel( String fileName) throws IOException {
		System.out.println("loading model "+fileName);
		target = new PlanarCalibrationTarget(loadPoints(fileName));
	}
	
	public void loadObservations( String fileName ) throws IOException {
		System.out.println("loading "+fileName);
		observations.add(loadPoints(fileName));
	}
	
	public static List<Point2D_F64> loadPoints( String fileName ) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(fileName));

		List<Point2D_F64> points = new ArrayList<Point2D_F64>();
		String line;
		while( (line = in.readLine()) != null ) {
			String words[] = line.split("\\s");
			for( int i = 0; i < words.length;  ) {
				double x = Double.parseDouble(words[i]);
				double y = Double.parseDouble(words[i+1]);

				points.add( new Point2D_F64(x,y));
				i += 2;
				while( i < words.length && words[i].length() == 0 )
					i++;
			}
		}

		return points;
	}

	public void process(  boolean assumeZeroSkew ,
						  int numRadialParam) {
		CalibrationPlanarGridZhang99 Zhang99
				= new CalibrationPlanarGridZhang99(target,assumeZeroSkew,numRadialParam);
		Zhang99.setOptimizer(optimizer);

		if( !Zhang99.process(observations) ) {
			throw new RuntimeException("Zhang99 algorithm failed!");
		}

		found = Zhang99.getOptimized();

		List<ImageResults> errors =
				CalibrateMonoPlanar.computeErrors(observations, found, target.points);
		CalibrateMonoPlanar.printErrors(errors);

		System.out.println("center x = "+found.x0);
		System.out.println("center y = "+found.y0);
		System.out.println("a = "+found.a);
		System.out.println("b = "+found.b);
		System.out.println("c = "+found.c);
		for( int i = 0; i < found.distortion.length; i++ ) {
			System.out.printf("radial[%d] = %6.2e\n",i,found.distortion[i]);
		}
	}

	public void printZhangError() {
		ComputeZhangErrors.zhangResults(observations,target.points);
	}

	public void optimizeUsingZhang() {
		ComputeZhangErrors.nonlinearUsingZhang(observations,target);
	}

	public void setOptimizer(UnconstrainedLeastSquares optimizer) {
		this.optimizer = optimizer;
	}

	public static void main( String args[] ) throws IOException {
		String base = "../data/evaluation/calibration/mono/PULNiX_CCD_6mm_Zhang/";

		CalibrateUsingZhangData app = new CalibrateUsingZhangData();
		app.loadObservations(base+"data1.txt");
		app.loadObservations(base+"data2.txt");
		app.loadObservations(base+"data3.txt");
		app.loadObservations(base+"data4.txt");
		app.loadObservations(base+"data5.txt");

		app.loadModel(base+"Model.txt");

		
		System.out.println("Computing Calibration");
		app.process(false,2);

		System.out.println();
		System.out.println("Results using Zhang's calibrated data");
		app.printZhangError();

		System.out.println();
		System.out.println("Results using Zhang's initial parameters");
		app.optimizeUsingZhang();
	}
}
