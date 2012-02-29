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

import boofcv.alg.geo.calibration.*;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Performs the full processing loop for calibrating a mono camera from a planar grid.  A
 * directory is specified that the images are read in from.  Calibration points are detected
 * inside the image and feed into the Zhang98 algorithm for parameter estimation.
 * </p>
 * 
 * <p>
 * Internally it supports status updates for a GUI and skips over bad images. Invoke functions
 * in the following order:
 * <ol>
 * <li>{@link #configure}</li> 
 * <li>{@link #reset}</li>
 * <li>{@link #addImage(String, java.awt.image.BufferedImage)}</li>
 * <li>{@link #process}</li>
 * <li>{@link #getFound}</li>
 * </ol>
 * </p>
 *
 * @author Peter Abeles
 */
public class CalibrateMonoPlanarApp {

	// detects calibration points inside of images
	protected PlanarCalibrationDetector detector;

	// computes calibration parameters
	protected CalibrationPlanarGridZhang98 zhang98;
	// calibration configuration
	protected PlanarCalibrationTarget target;
	protected boolean assumeZeroSkew;

	// computed parameters
	protected ParametersZhang98 found;

	// should it save the images in memory
	boolean saveImages;
	
	// Information on calibration targets and results
	protected List<String> imageNames = new ArrayList<String>();
	protected List<BufferedImage> images = new ArrayList<BufferedImage>();
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
	 * @param saveImages Save all images in a list.  Useful for displaying results, should be false otherwise.
	 */
	public CalibrateMonoPlanarApp(PlanarCalibrationDetector detector , boolean saveImages ) {
		this.detector = detector;
		this.saveImages = saveImages;
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
		zhang98 = new CalibrationPlanarGridZhang98(target,assumeZeroSkew,numRadialParam);
	}

	/**
	 * Resets internal data structures.  Must call before adding images
	 */
	public void reset() {
		state = 0;
		message = "";
		imageNames = new ArrayList<String>();
		images = new ArrayList<BufferedImage>();
		observations = new ArrayList<List<Point2D_F64>>();
	}

	public void addImage( String name , BufferedImage orig ) {
		ImageFloat32 image = ConvertBufferedImage.convertFrom(orig, (ImageFloat32) null);

		System.out.println("processing "+name);
		if( !detector.process(image) )
			System.err.println("  Failed to process image: "+name);
		else {
			imageNames.add(name);
			observations.add(detector.getPoints());
			if( saveImages ) {
				images.add(orig);
			}
			updateStatus(0,"Feature Extraction "+imageNames.size());
		}
	}

	public static List<String> directoryImageList( String directory ) {
		List<String> ret = new ArrayList<String>();
		
		File d = new File(directory);

		if( !d.isDirectory() )
			throw new IllegalArgumentException("Must specify an directory");

		File files[] = d.listFiles();


		for( File f : files ) {
			if( f.isDirectory() || f.isHidden() )
				continue;

			BufferedImage orig = UtilImageIO.loadImage(f.getAbsolutePath());
			if( orig == null ) {
				continue;
			}
			ret.add(f.getAbsolutePath());
		}
		
		return ret;
	}

	/**
	 * After calibration points have been found this invokes the Zhang98 algorithm to
	 * estimate calibration parameters.  Error statistics are also computed.
	 */
	public void process() {
		updateStatus(1,"Estimating Parameters");
		System.out.println("Estimating and optimizing numerical parameters");
		if( !zhang98.process(observations) ) {
			throw new RuntimeException("Zhang98 algorithm failed!");
		}

		found = zhang98.getOptimized();

		updateStatus(2,"Computing Errors");
		errors = computeErrors(observations,found,target.points,assumeZeroSkew);
		printErrors(errors);

		System.out.println("center x = "+found.x0);
		System.out.println("center y = "+found.y0);
		System.out.println("a = "+found.a);
		System.out.println("b = "+found.b);
		System.out.println("c = "+found.c);
		for( int i = 0; i < found.distortion.length; i++ ) {
			System.out.printf("radial[%d] = %6.2e\n",i,found.distortion[i]);
		}
		updateStatus(3,"Done");
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
													ParametersZhang98 param ,
													List<Point2D_F64> grid ,
													boolean assumeZeroSkew)
	{
		Zhang98OptimizationFunction function =
				new Zhang98OptimizationFunction(param,assumeZeroSkew,grid,observation);

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
	
	public List<String> getImageNames() {
		return imageNames;
	}

	public List<BufferedImage> getImages() {
		return images;
	}

	public List<List<Point2D_F64>> getObservations() {
		return observations;
	}

	public List<ImageResults> getErrors() {
		return errors;
	}

	public ParametersZhang98 getFound() {
		return found;
	}

	public static void main( String args[] ) {
//		PlanarCalibrationDetector detector = new WrapPlanarGridTarget();
		PlanarCalibrationDetector detector = new WrapPlanarChessTarget(8,8);

		PlanarCalibrationTarget target = FactoryPlanarCalibrationTarget.gridSquare(8, 8, 1, 7 / 18);

		CalibrateMonoPlanarApp app = new CalibrateMonoPlanarApp(detector,false);

		app.reset();
		app.configure(target,false,2);

		List<String> images = directoryImageList("../data/evaluation/calibration/mono/Sony_DSC-HX5V");
		
		for( String n : images ) {
			BufferedImage input = UtilImageIO.loadImage(n);
			if( n != null ) {
				app.addImage(n,input);
			}
		}
		app.process();
	}
}
