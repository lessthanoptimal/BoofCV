/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.calibration;

import boofcv.alg.geo.calibration.*;
import boofcv.alg.geo.calibration.omni.CalibParamUniversalOmni;
import boofcv.alg.geo.calibration.pinhole.CalibParamPinholeRadial;
import boofcv.struct.calib.CameraModel;
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
 * <b>Most 3D operations in BoofCV assume that the image coordinate system is right handed and the +Z axis is
 * pointing out of the camera.</b>  In standard image coordinate the origin (0,0) is at the top left corner with +x going
 * to the right and +y going down, then if it is right handed +z will be out of the image.  <b>However some times
 * this pseudo standard is not followed and the y-axis needs to be inverted by setting isInverted to true.</b>
 * </p>
 *
 * @author Peter Abeles
 */
public class CalibrateMonoPlanar {

	// detects calibration points inside of images
	protected DetectorFiducialCalibration detector;

	// how the points are laid out
	protected List<Point2D_F64> layout;
	
	// computes calibration parameters
	protected CalibrationPlanarGridZhang99 zhang99;

	// computed parameters
	protected Zhang99AllParam foundZhang;
	protected CameraModel foundIntrinsic;

	// Information on calibration targets and results
	protected List<CalibrationObservation> observations = new ArrayList<>();
	protected List<ImageResults> errors;

	public boolean verbose = false;

	// shape of the image
	private int imageWidth;
	private int imageHeight;


	public CalibrateMonoPlanar( List<Point2D_F64> layout ) {
		this.layout = layout;
	}

	/**
	 * Specifies the calibration model.
	 */
	public void configure(Zhang99IntrinsicParam param )
	{
		zhang99 = new CalibrationPlanarGridZhang99(layout,param);
	}

	public void configurePinhole(boolean assumeZeroSkew ,
								 int numRadialParam ,
								 boolean includeTangential )
	{
		zhang99 = new CalibrationPlanarGridZhang99(layout,
				new CalibParamPinholeRadial(assumeZeroSkew,numRadialParam,includeTangential));
	}

	public void configureUniversalOmni(boolean assumeZeroSkew ,
									   int numRadialParam ,
									   boolean includeTangential )
	{
		zhang99 = new CalibrationPlanarGridZhang99(layout,
				new CalibParamUniversalOmni(assumeZeroSkew,numRadialParam,includeTangential,false));
	}

	public void configureUniversalOmni(boolean assumeZeroSkew ,
									   int numRadialParam ,
									   boolean includeTangential ,
									   double mirrorOffset )
	{
		zhang99 = new CalibrationPlanarGridZhang99(layout,
				new CalibParamUniversalOmni(assumeZeroSkew,numRadialParam,includeTangential,mirrorOffset));
	}


	/**
	 * Resets internal data structures.  Must call before adding images
	 */
	public void reset() {
		observations = new ArrayList<>();
		errors = null;
		imageHeight = imageWidth = 0;
	}

	/**
	 * Adds the observations from a calibration target detector
	 * @param observation Detected calibration points
	 */
	public void addImage( CalibrationObservation observation ) {
		if( imageWidth == 0 ) {
			this.imageWidth = observation.getWidth();
			this.imageHeight = observation.getHeight();
		} else if( observation.getWidth() != this.imageWidth || observation.getHeight() != this.imageHeight) {
			throw new IllegalArgumentException("Image shape miss match");
		}
		observations.add( observation );
	}

	/**
	 * Removes the most recently added image
	 */
	public void removeLatestImage() {
		observations.remove( observations.size() - 1 );
	}

	/**
	 * After calibration points have been found this invokes the Zhang99 algorithm to
	 * estimate calibration parameters.  Error statistics are also computed.
	 */
	public <T extends CameraModel>T process() {
		if( zhang99 == null )
			throw new IllegalArgumentException("Please call configure first.");
		if( !zhang99.process(observations) ) {
			throw new RuntimeException("Zhang99 algorithm failed!");
		}

		foundZhang = zhang99.getOptimized();

		errors = computeErrors(observations, foundZhang,layout);

		foundIntrinsic = foundZhang.getIntrinsic().getCameraModel();
		foundIntrinsic.width = imageWidth;
		foundIntrinsic.height = imageHeight;

		return (T)foundIntrinsic;
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
	public static List<ImageResults> computeErrors( List<CalibrationObservation> observation ,
													Zhang99AllParam param ,
													List<Point2D_F64> grid )
	{
		Zhang99OptimizationFunction function =
				new Zhang99OptimizationFunction(param,grid,observation);

		double residuals[] = new double[grid.size()*observation.size()*2];
		function.process(param,residuals);

		List<ImageResults> ret = new ArrayList<>();

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

	public List<CalibrationObservation> getObservations() {
		return observations;
	}

	public List<ImageResults> getErrors() {
		return errors;
	}

	public Zhang99AllParam getZhangParam() {
		return foundZhang;
	}

	public <T extends CameraModel>T getIntrinsic() {
		return (T)foundIntrinsic;
	}

	public CalibrationPlanarGridZhang99 getZhang99() {
		return zhang99;
	}
}
