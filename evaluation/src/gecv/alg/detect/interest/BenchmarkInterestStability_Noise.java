/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.interest;

import gecv.abst.detect.corner.FactoryCornerDetector;
import gecv.abst.detect.corner.GeneralCornerDetector;
import gecv.abst.detect.interest.FactoryInterestPoint;
import gecv.abst.detect.interest.InterestPointDetector;
import gecv.core.image.ConvertBufferedImage;
import gecv.core.image.GeneralizedImageOps;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import jgrl.geometry.UtilPoint2D_I32;
import jgrl.struct.point.Point2D_I32;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Compares different image interest point detection algorithms stability under different amounts of
 * i.i.d. gaussian pixel noise.
 *
 * @author Peter Abeles
 */
public class BenchmarkInterestStability_Noise <T extends ImageBase, D extends ImageBase>  {

	Random rand = new Random(23234);
	// number of Monte-Carlo trials
	int numTrials = 50;
	// noise increment
	double sigmaLevel = 5;
	// two points are considered to be the same if they are within this tolerance
	double matchTolerance = 3;

	// radius of the feature it is detecting
	int radius = 2;
	// the number of features it will search for
	static int maxFeatures = 40;
	// types of images being processed
	Class<T> imageType;
	Class<D> derivType;

	/**
	 * Evaluating input images of the specified type.
	 *
	 * @param imageType Original input image type.
	 * @param derivType Type of the image derivative.
	 */
	public BenchmarkInterestStability_Noise(Class<T> imageType, Class<D> derivType) {
		this.imageType = imageType;
		this.derivType = derivType;
	}

	/**
	 * Check stability across different amounts of Gaussian noise for the specified image.
	 *
	 * @param input Image gthat
	 */
	public void process( BufferedImage input  ) {
		if( input == null )
			throw new IllegalArgumentException("Image didn't load.");

		List<Helper<T>> algs = createAlgs();

		T original = ConvertBufferedImage.convertFrom(input,null,imageType);

		// get the list of where the features where initially found
		// Each algorithm is scored based on how similar features found after this are
		for( Helper<T> h : algs ) {
			List<Point2D_I32> l = h.detector.detect(original);
			h.setOriginal(l);
		}

		// Add various amounts of noise to
		double sigmas[] = new double[5];
		for( int noiseLevel = 1; noiseLevel <= sigmas.length; noiseLevel++ ) {
			double sigma = sigmas[noiseLevel-1] = noiseLevel*sigmaLevel;
//			System.out.println("compute noise "+sigma);
			
			for( int i = 0; i < numTrials; i++ ) {
				T noisy = (T)original.clone();
				GeneralizedImageOps.addGaussian(noisy,rand,sigma);

				for( Helper<T> h : algs ) {
					List<Point2D_I32> l = h.detector.detect(noisy);
					computeScore( l , h );
				}
			}

			for( Helper<T> h : algs ) {
				Score s = h.computeScore();
				h.scores.add(s);
				h.reset();
			}
		}

		// print the percent missed for each noise level
		System.out.println("Percent Missed:");
		System.out.print("Sigma    ");
		for( int i = 0; i < sigmas.length; i++ ) {
			System.out.printf("     %2d",(int)sigmas[i]);
		}
		System.out.println();
		for( Helper<T> h : algs ) {
			System.out.printf("%10s ",h.name);
			for( Score s : h.scores ) {
				System.out.printf(" %5.2f%%",100.0*s.missed);
			}
			System.out.println();
		}
	}

	private void computeScore(List<Point2D_I32> l, Helper<T> h) {

		int numMissed = 0;
		double total = 0;

		for( Point2D_I32 origPt : h.original ) {
			double bestDist = Double.MAX_VALUE;
			for( Point2D_I32 p : l ) {
				double d = UtilPoint2D_I32.distance(origPt,p);

				if( d < bestDist ) {
					bestDist = d;
				}
			}

			if( bestDist > matchTolerance ) {
				numMissed++;
			} else {
				total += bestDist;
			}
		}

		h.numMissed.add(numMissed);
		h.error.add(total/(h.original.size()-numMissed));
	}

	public List<Helper<T>> createAlgs() {
		List<Helper<T>> ret = new ArrayList<Helper<T>>();

		GeneralCornerDetector<T,D> alg;

		alg = FactoryCornerDetector.createFast(radius,20,maxFeatures,imageType);
		ret.add( new Helper<T>("Fast",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createHarris(radius,20,maxFeatures,derivType);
		ret.add( new Helper<T>("Harris",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createKlt(radius,20,maxFeatures,derivType);
		ret.add( new Helper<T>("KLT",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createKitRos(radius,20,maxFeatures,derivType);
		ret.add( new Helper<T>("KitRos",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createMedian(radius,20,maxFeatures,imageType);
		ret.add( new Helper<T>("Median",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );

		// todo add Harris Scale
		// TODO add KIT

		return ret;
	}

	public static class Helper<T extends ImageBase>
	{
		String name;
		InterestPointDetector<T> detector;
		List<Point2D_I32> original;
		List<Double> error = new ArrayList<Double>();
		List<Integer> numMissed = new ArrayList<Integer>();

		List<Score> scores = new ArrayList<Score>();
		
		public Helper( String name , InterestPointDetector<T> detector) {
			this.name = name;
			this.detector = detector;
		}

		public void setOriginal(List<Point2D_I32> original) {
			this.original = new ArrayList<Point2D_I32>();
			for( Point2D_I32 p : original ) {
				this.original.add(p.copy());
			}
		}

		public void reset() {
			error.clear();
			numMissed.clear();
		}

		public Score computeScore() {
			double totalMissed = 0;
			double totalScore = 0;
			int scoreDivisor = 0;

			for( int i = 0; i < error.size(); i++ ) {
				totalMissed += numMissed.get(i);
				double s = error.get(i);
				if( Double.isNaN(s) || Double.isInfinite(s))
					continue;
				totalScore += s;
				scoreDivisor++;
			}

			totalMissed /= error.size();
			totalScore /= scoreDivisor;
			Score ret = new Score();
			ret.error = totalScore;
			ret.missed = (totalMissed/maxFeatures);

			return ret;
		}
	}

	public static class Score
	{
		double missed;
		double error;
	}

	public static void main( String args[] ) {
		// specify test images
		String imageNames[] = new String[]{"outdoors01.jpg","indoors01.jpg","scale/beach01.jpg"};

		// evaluate each image individually
		for( String s : imageNames ) {
			BufferedImage image = UtilImageIO.loadImage("evaluation/data/"+s);
			BenchmarkInterestStability_Noise<ImageFloat32,ImageFloat32> alg = new BenchmarkInterestStability_Noise<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);

			System.out.println("\nEvaluating Image: "+s);
			alg.process(image);
		}
	}
}
