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
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * Compute the errors to Zhang's data using Zhang's result
 *
 * @author Peter Abeles
 */
public class ComputeZhangErrors {

	private static List<DenseMatrix64F> getPose() {
		List<DenseMatrix64F> ret = new ArrayList<DenseMatrix64F>();


		ret.add(UtilEjml.parseMatrix("0.992759 -0.026319 0.117201\n" +
				"0.0139247 0.994339 0.105341\n" +
				"-0.11931 -0.102947 0.987505\n" +
				"-3.84019 3.65164 12.791",3));

		ret.add(UtilEjml.parseMatrix("0.997397 -0.00482564 0.0719419\n" +
				"0.0175608 0.983971 -0.17746\n" +
				"-0.0699324 0.178262 0.981495\n" +
				"-3.71693 3.76928 13.1974",3));

		ret.add(UtilEjml.parseMatrix("0.915213 -0.0356648 0.401389\n" +
				"-0.00807547 0.994252 0.106756\n" +
				"-0.402889 -0.100946 0.909665\n" +
				"-2.94409 3.77653 14.2456",3));

		ret.add(UtilEjml.parseMatrix("0.986617 -0.0175461 -0.16211\n" +
				"0.0337573 0.994634 0.0977953\n" +
				"0.159524 -0.101959 0.981915\n" +
				"-3.40697 3.6362 12.4551",3));

		ret.add(UtilEjml.parseMatrix("0.967585 -0.196899 -0.158144\n" +
				"0.191542 0.980281 -0.0485827\n" +
				"0.164592 0.0167167 0.98622\n" +
				"-4.07238 3.21033 14.3441",3));

		return ret;
	}

	/**
	 * Compute error metrics using parameters on Zhang's website
	 */
	public static void zhangResults( List<List<Point2D_F64>> observations , List<Point2D_F64> target ) {
		Zhang99Parameters param = getZhangParam();

		param.convertToIntrinsic().print();
		List<ImageResults> errors =
				CalibrateMonoPlanar.computeErrors(observations, param, target);
		CalibrateMonoPlanar.printErrors(errors);
	}

	/**
	 * Returns a set of parameters using the found results on Zhang's website
	 */
	private static Zhang99Parameters getZhangParam() {
		Zhang99Parameters param = new Zhang99Parameters(false,2,5);

		param.a = 832.5;
		param.c = 0.204494;
		param.b = 832.53;
		param.x0 = 303.959;
		param.y0 = 206.585;
		param.distortion[0] = -0.228601;
		param.distortion[1] = 0.190353;

		List<DenseMatrix64F> mat = getPose();

		for( int i = 0; i < 5; i++ ) {

			DenseMatrix64F m = mat.get(i);

			param.views[i].T.set(m.get(3,0),m.get(3,1),m.get(3,2));

			m.reshape(3,3,true);
//			CommonOps.transpose(m);

			RotationMatrixGenerator.matrixToRodrigues(m, param.views[i].rotation);
		}
		return param;
	}

	/**
	 * Zhang's results have a fairly different focal length.  Use Zhang's parameters
	 * as the initial value when optimizing to see if the focal length stays the same.
	 */
	public static void nonlinearUsingZhang(List<List<Point2D_F64>> observations ,
										   PlanarCalibrationTarget target) {
		Zhang99Parameters param = getZhangParam();
		Zhang99Parameters found = new Zhang99Parameters(false,2,5);

		// perform non-linear optimization to improve results
		// NOTE: constructor doesn't matter
		CalibrationPlanarGridZhang99 alg = new CalibrationPlanarGridZhang99(target,false,1);
		alg.optimizedParam(observations,target.points,param,found,null);

		found.convertToIntrinsic().print();
		List<ImageResults> errors =
				CalibrateMonoPlanar.computeErrors(observations, found, target.points);
		CalibrateMonoPlanar.printErrors(errors);
	}
}


