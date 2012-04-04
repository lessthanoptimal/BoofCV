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
import boofcv.alg.geo.calibration.Zhang99OptimizationFunction;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Performs the full processing loop for calibrating a mono camera from a planar grid.  A
 * directory is specified that the images are read in from.  Calibration points are detected
 * inside the image and feed into the Zhang99 algorithm for parameter estimation.
 * </p>
 * 
 * <p>
 * Internally it supports status updates for a GUI and skips over bad images. Invoke functions
 * in the following order:
 * <ol>
 * <li>{@link #configure}</li> 
 * <li>{@link #reset}</li>
 * <li>{@link #addImage}</li>
 * <li>{@link #process}</li>
 * <li>{@link #getFound}</li>
 * </ol>
 * </p>
 *
 * @author Peter Abeles
 */
public class CalibrateMonoPlanar {

	// detects calibration points inside of images
	protected PlanarCalibrationDetector detector;

	// computes calibration parameters
	protected CalibrationPlanarGridZhang99 Zhang99;
	// calibration configuration
	protected PlanarCalibrationTarget target;
	protected boolean assumeZeroSkew;

	// computed parameters
	protected ParametersZhang99 found;


	// Information on calibration targets and results
	protected List<List<Point2D_F64>> observations = new ArrayList<List<Point2D_F64>>();
	protected List<ImageResults> errors;

	// how far along in the process is it
	public int state;
	// status message describing what it is up to
	public String message;

	/**
	 * High level configuration
	 *
	 * @param detector Target detection.
	 */
	public CalibrateMonoPlanar(PlanarCalibrationDetector detector) {
		this.detector = detector;
	}

	/**
	 * Specify calibration assumptions.
	 * 
	 * @param target Describes the calibration target.
	 * @param assumeZeroSkew If true zero skew is assumed.
	 * @param numRadialParam Number of radial parameters
	 */
	public void configure( PlanarCalibrationTarget target ,
						   boolean assumeZeroSkew ,
						   int numRadialParam )
	{
		this.assumeZeroSkew = assumeZeroSkew;
		this.target = target;
		Zhang99 = new CalibrationPlanarGridZhang99(target,assumeZeroSkew,numRadialParam);
	}

	/**
	 * Resets internal data structures.  Must call before adding images
	 */
	public void reset() {
		state = 0;
		message = "";
		observations = new ArrayList<List<Point2D_F64>>();
	}

	/**
	 *  Adds a new view of the calibration target and processes it.  If processing fails
	 *  then false is returned.
	 *
	 * @param image Image of a calibration target
	 * @return true if a target was detected in the image or not
	 */
	public boolean addImage( ImageFloat32 image ) {

		if( !detector.process(image) )
			return false;
		else {
			observations.add(detector.getPoints());
			return true;
		}
	}

	/**
	 * After calibration points have been found this invokes the Zhang99 algorithm to
	 * estimate calibration parameters.  Error statistics are also computed.
	 */
	public IntrinsicParameters process() {
		updateStatus(1,"Estimating Parameters");
		System.out.println("Estimating and optimizing numerical parameters");
		if( !Zhang99.process(observations) ) {
			throw new RuntimeException("Zhang99 algorithm failed!");
		}

		found = Zhang99.getOptimized();

		updateStatus(2,"Computing Errors");
		errors = computeErrors(observations,found,target.points,assumeZeroSkew);
		printErrors(errors);

		return found.convertToIntrinsic();
	}

	/**
	 * After the parameters have been estimated this computes the error for each calibration point in
	 * each image and summary error statistics.
	 *
	 * @param observation Observed control point location
	 * @param param Found calibration parameters
	 * @return List of error statistics
	 */
	public static List<ImageResults> computeErrors( List<List<Point2D_F64>> observation ,
													ParametersZhang99 param ,
													List<Point2D_F64> grid ,
													boolean assumeZeroSkew)
	{
		Zhang99OptimizationFunction function =
				new Zhang99OptimizationFunction(param,assumeZeroSkew,grid,observation);

		double residuals[] = new double[grid.size()*observation.size()*2];
		function.process(param,residuals);

		List<ImageResults> ret = new ArrayList<ImageResults>();

		int N = grid.size();
		int index = 0;
		for( int indexObs = 0; indexObs < observation.size(); indexObs++ ) {
			ImageResults r = new ImageResults(N);

			double meanX = 0;
			double meanY = 0;
			double meanErrorMag = 0;
			double maxError = 0;

			for( int i = 0; i < N; i++ ) {
				double errorX = residuals[index++];
				double errorY = residuals[index++];
				double errorMag = Math.sqrt(errorX*errorX + errorY*errorY);

				r.pointError[i] = errorMag;

				meanX += errorX;
				meanY += errorY;
				meanErrorMag += errorMag;

				if( maxError < errorMag ) {
					maxError = errorMag;
				}
			}

			r.biasX = meanX /= N;
			r.biasY = meanY /= N;
			r.meanError = meanErrorMag /= N;
			r.maxError = maxError;
			ret.add(r);
		}

		return ret;
	}

	/**
	 * Prints out error information to standard out
	 */
	public static void printErrors( List<ImageResults> results ) {
		double totalError = 0;
		for( int i = 0; i < results.size(); i++ ) {
			ImageResults r = results.get(i);
			totalError += r.meanError;

			System.out.printf("image %3d Euclidean ( mean = %7.1e max = %7.1e ) bias ( X = %8.1e Y %8.1e )\n",i,r.meanError,r.maxError,r.biasX,r.biasY);
		}
		System.out.println("Average Mean Error = "+(totalError/results.size()));
	}

	/**
	 * Lets the world know what it is doing
	 */
	private void updateStatus( int state , String message ) {
		this.state = state;
		this.message = message;
	}

	public List<List<Point2D_F64>> getObservations() {
		return observations;
	}

	public List<ImageResults> getErrors() {
		return errors;
	}

	public ParametersZhang99 getFound() {
		return found;
	}

	public PlanarCalibrationTarget getTarget() {
		return target;
	}

}
