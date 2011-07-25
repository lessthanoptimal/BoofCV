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
 * Compares different image interest point detection algorithms stability under noise and image transformations.
 *
 * @author Peter Abeles
 */
// todo multiple images
// todo print as a function of noise level
// todo add other interest point types
public class BenchmarkInterestStability_Noise <T extends ImageBase, D extends ImageBase>  {

	Random rand = new Random(23234);
	int numTrials = 50;
	double sigma = 10;
	double matchTolerance = 3;

	int radius = 2;
	static int maxFeatures = 40;
	Class<T> imageType = (Class<T>)ImageFloat32.class;
	Class<D> derivType = (Class<D>)ImageFloat32.class;

	public void process( BufferedImage input  ) {
		if( input == null )
			throw new IllegalArgumentException("Image didn't load.");

		List<Helper<T>> algs = createAlgs();

		T original = ConvertBufferedImage.convertFrom(input,null,imageType);

		System.out.println("Finding initial feature locations");
		// get the list of where the features where initially found
		for( Helper<T> h : algs ) {
			List<Point2D_I32> l = h.detector.detect(original);
			h.setOriginal(l);
		}

		for( int i = 0; i < numTrials; i++ ) {
			T noisy = (T)original.clone();
			GeneralizedImageOps.addGaussian(noisy,rand,sigma);

			for( Helper<T> h : algs ) {
				List<Point2D_I32> l = h.detector.detect(noisy);
				computeScore( l , h );
			}
		}

		for( Helper<T> h : algs ) {
			h.printResults();
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
		h.scores.add(total/(h.original.size()-numMissed));
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
		List<Double> scores = new ArrayList<Double>();
		List<Integer> numMissed = new ArrayList<Integer>();

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

		public void printResults() {
			double totalMissed = 0;
			double totalScore = 0;
			int scoreDivisor = 0;

			for( int i = 0; i < scores.size(); i++ ) {
				totalMissed += numMissed.get(i);
				double s = scores.get(i);
				if( Double.isNaN(s) || Double.isInfinite(s))
					continue;
				totalScore += s;
				scoreDivisor++;
			}

			totalMissed /= scores.size();
			totalScore /= scoreDivisor;
			System.out.printf("%10s missed = %5.2f%% score %5.2f\n",name,100.0*(totalMissed/maxFeatures),totalScore);
		}
	}

	public static void main( String args[] ) {
		BufferedImage image = UtilImageIO.loadImage("evaluation/data/outdoors01.jpg");

		BenchmarkInterestStability_Noise alg = new BenchmarkInterestStability_Noise();
		alg.process(image);

		image = UtilImageIO.loadImage("evaluation/data/indoors01.jpg");
		alg.process(image);
	}
}
