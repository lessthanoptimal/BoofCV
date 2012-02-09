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

import boofcv.alg.geo.calibration.CalibrationGridConfig;
import boofcv.alg.geo.calibration.CalibrationPlanarGridZhang98;
import boofcv.alg.geo.calibration.ParametersZhang98;
import boofcv.alg.geo.calibration.Zhang98OptimizationFunction;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_F64;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO Status bar showing what is being processed
// TODO Show rectified and unrectified images side by side
// TODO print point error
// todo show image coverage, recommend more images or not
public class CalibrateMonoPlanarApp {

	protected CalibrationGridInterface detector;

	protected CalibrationPlanarGridZhang98 zhang98;
	protected CalibrationGridConfig configGrid;
	protected boolean assumeZeroSkew;

	protected ParametersZhang98 found;

	boolean saveImages;
	
	protected List<String> imageNames;
	protected List<BufferedImage> images;
	protected List<List<Point2D_F64>> observations;
	protected List<ImageResults> errors;
	
	public int state;
	public String message;

	public CalibrateMonoPlanarApp(CalibrationGridInterface detector , boolean saveImages ) {
		this.detector = detector;
		this.saveImages = saveImages;
	}
	
	public void configure( CalibrationGridConfig config ,
						   boolean assumeZeroSkew ,
						   int numRadialParam )
	{
		this.assumeZeroSkew = assumeZeroSkew;
		this.configGrid = config;
		detector.configure(config);
		zhang98 = new CalibrationPlanarGridZhang98(config,assumeZeroSkew,numRadialParam);
	}

	public void reset() {
		state = 0;
		message = "";
	}
	
	protected List<ImageResults> computeErrors( List<List<Point2D_F64>> observation , ParametersZhang98 param )
	{
		List<Point2D_F64> grid = configGrid.computeGridPoints();
		
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
	
	public void printErrors( List<ImageResults> results ) {
		double totalError = 0;
		for( int i = 0; i < results.size(); i++ ) {
			ImageResults r = results.get(i);
			totalError += r.meanError;
			
			System.out.printf("image %3d Euclidean ( mean = %7.1e max = %7.1e ) bias ( X = %8.1e Y %8.1e )\n",i,r.meanError,r.maxError,r.biasX,r.biasY);
		}
		System.out.println("Total Mean Error = "+totalError);
	}

	public void loadImages( String directory ) {
		File d = new File(directory);

		if( !d.isDirectory() )
			throw new IllegalArgumentException("Must specify an directory");

		File files[] = d.listFiles();

		imageNames = new ArrayList<String>();
		images = new ArrayList<BufferedImage>();
		observations = new ArrayList<List<Point2D_F64>>();

		for( File f : files ) {
			if( f.isDirectory() || f.isHidden() )
				continue;

			BufferedImage orig = UtilImageIO.loadImage(f.getAbsolutePath());
			if( orig == null ) {
				continue;
			}
			System.out.println("Processing "+f.getName());
			ImageFloat32 image = ConvertBufferedImage.convertFrom(orig, (ImageFloat32) null);

			if( !detector.process(image) )
				System.err.println("  Failed to process image: "+f.getName());
			else {
				imageNames.add(f.getName());
				observations.add(detector.getPoints());
				if( saveImages ) {
					images.add(orig);
				}
				updateStatus(0,"Feature Extraction "+imageNames.size());
			}
		}

		if( observations.size() == 0 )
			throw new RuntimeException("No images found in "+directory+"!");
	}

	public void process() {
		updateStatus(1,"Estimating Parameters");
		System.out.println("Estimating and optimizing numerical parameters");
		if( !zhang98.process(observations) ) {
			throw new RuntimeException("Zhang98 algorithm failed!");
		}

		found = zhang98.getOptimized();

		updateStatus(2,"Computing Errors");
		errors = computeErrors(observations,found);
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

	public void updateStatus( int state , String message ) {
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
		CalibrationGridInterface detector = new WrapPlanarGridTarget();

		CalibrationGridConfig config = new CalibrationGridConfig(8,6,30);

		CalibrateMonoPlanarApp app = new CalibrateMonoPlanarApp(detector,false);

		app.configure(config,false,2);

//		app.loadImages("../data/evaluation/calibration/mono/Sony_DSC-HX5V");
		app.loadImages("/home/pja/saved/a");
		app.process();
	}
}
