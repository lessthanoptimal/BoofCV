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
 * <li>{@link #getIntrinsic}</li>
 * </ol>
 * </p>
 *
 * <p>
 * If the image coordinates are in a left handed coordinate system, which is standard, then they can be put into
 * a right handed coordinate system by setting the 'leftHanded' constructor flag to true.  If true, then
 * the y-axis coordinate of observations is adjusted so that the image bottom left corner is the (0,0) coordinate
 * and that the y-axis is pointed in the positive direction.  This is accomplished by setting y = height - 1 - obs.y,
 * where height is the image height.  <b>If the camera calibration is adjusted into a right handed coordinate system
 * as just specified then all other references to pixel coordinates need to be adjusted the same way.</b>
 * </p>
 *
 * @author Peter Abeles
 */
public class CalibrateMonoPlanar {

	// detects calibration points inside of images
	protected PlanarCalibrationDetector detector;

	// are image coordinates in a left-handed coordinate system?
	// most images have +y pointed down, which makes it left handed and breaks the standard right handed
	// assumption in other algorithms.  If true then the y-axis is adjusted so that it is pointed up
	protected boolean convertToRightHanded;

	// computes calibration parameters
	protected CalibrationPlanarGridZhang99 zhang99;
	// calibration configuration
	protected PlanarCalibrationTarget target;
	protected boolean assumeZeroSkew;

	// computed parameters
	protected ParametersZhang99 foundZhang;
	protected IntrinsicParameters foundIntrinsic;

	// Information on calibration targets and results
	protected List<List<Point2D_F64>> observations = new ArrayList<List<Point2D_F64>>();
	// adjusted observations so that they are in a right handed coordinate system
	protected List<List<Point2D_F64>> observationsAdj = new ArrayList<List<Point2D_F64>>();
	protected List<ImageResults> errors;

	public boolean verbose = false;

	// shape of the image
	private int widthImg;
	private int heightImg;

	/**
	 * High level configuration
	 *
	 * @param detector Target detection algorithm.
	 * @param convertToRightHanded If true it will convert a left handed image coordinate system into a right handed one.
	 *                             Normally this should be true.
	 */
	public CalibrateMonoPlanar(PlanarCalibrationDetector detector , boolean convertToRightHanded ) {
		this.detector = detector;
		this.convertToRightHanded = convertToRightHanded;
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
		zhang99 = new CalibrationPlanarGridZhang99(target,assumeZeroSkew,numRadialParam);
	}

	/**
	 * Resets internal data structures.  Must call before adding images
	 */
	public void reset() {
		observations = new ArrayList<List<Point2D_F64>>();
		errors = null;
		heightImg = widthImg = 0;
	}

	/**
	 *  Adds a new view of the calibration target and processes it.  If processing fails
	 *  then false is returned.
	 *
	 * @param image Image of a calibration target
	 * @return true if a target was detected in the image or not
	 */
	public boolean addImage( ImageFloat32 image ) {

		if( widthImg == 0 ) {
			widthImg = image.width;
			heightImg = image.height;
		} else if( widthImg != image.width || heightImg != image.height ) {
			throw new IllegalArgumentException("All images must have the same shape");
		}

		if( !detector.process(image) )
			return false;
		else {
			int h = image.getHeight()-1;
			List<Point2D_F64> points = detector.getPoints();
			List<Point2D_F64> adjusted = new ArrayList<Point2D_F64>();

			// make it so +y is pointed up not down, and becomes a right handed coordinate system
			if(convertToRightHanded) {
				for( Point2D_F64 p : points ) {
					Point2D_F64 a = new Point2D_F64(p.x,h-p.y);
					adjusted.add(a);
				}
			} else {
				adjusted.addAll(points);
			}

			observations.add(points);
			observationsAdj.add(adjusted);
			return true;
		}
	}

	/**
	 * After calibration points have been found this invokes the Zhang99 algorithm to
	 * estimate calibration parameters.  Error statistics are also computed.
	 */
	public IntrinsicParameters process() {
		if( !zhang99.process(observationsAdj) ) {
			throw new RuntimeException("Zhang99 algorithm failed!");
		}

		foundZhang = zhang99.getOptimized();

		errors = computeErrors(observationsAdj, foundZhang,target.points,assumeZeroSkew);

		foundIntrinsic = foundZhang.convertToIntrinsic();
		foundIntrinsic.width = widthImg;
		foundIntrinsic.height = heightImg;

		return foundIntrinsic;
	}

	public void printStatistics() {
		printErrors(errors);
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

	public List<List<Point2D_F64>> getObservations() {
		return observations;
	}

	public List<ImageResults> getErrors() {
		return errors;
	}

	public ParametersZhang99 getZhangParam() {
		return foundZhang;
	}

	public IntrinsicParameters getIntrinsic() {
		return foundIntrinsic;
	}

	public PlanarCalibrationTarget getTarget() {
		return target;
	}

	public boolean isConvertToRightHanded() {
		return convertToRightHanded;
	}
}
