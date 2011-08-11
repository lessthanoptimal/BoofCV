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

package gecv.alg.detect.interest.benchmark;

import gecv.abst.detect.interest.FactoryInterestPoint;
import gecv.abst.detect.interest.InterestPointDetector;
import gecv.abst.detect.point.FactoryBlobDetector;
import gecv.abst.detect.point.FactoryCornerDetector;
import gecv.abst.detect.point.GeneralFeatureDetector;
import gecv.alg.detect.intensity.HessianBlobIntensity;
import gecv.alg.detect.interest.*;
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
	int numTrials = 5;
	// noise increment
	double sigmaLevel = 5;
	// two points are considered to be the same if they are within this tolerance
	double matchTolerance = 3;

	BenchmarkInterestParameters<T,D> param;

	/**
	 * Evaluating input images of the specified type.
	 *
	 * @param imageType Original input image type.
	 * @param derivType Type of the image derivative.
	 */
	public BenchmarkInterestStability_Noise(Class<T> imageType, Class<D> derivType) {
		param = new BenchmarkInterestParameters<T,D>();
		param.imageType = imageType;
		param.derivType = derivType;
	}

	/**
	 * Check stability across different amounts of Gaussian noise for the specified image.
	 *
	 * @param input Image gthat
	 */
	public void process( BufferedImage input  ) {
		if( input == null )
			throw new IllegalArgumentException("Image didn't load.");

		List<InterestMetrics<T>> algs = createAlgs(param);

		T original = ConvertBufferedImage.convertFrom(input,null,param.imageType);

		// get the list of where the features where initially found
		// Each algorithm is scored based on how similar features found after this are
		for( InterestMetrics<T> h : algs ) {
			h.setMetric(new StabilityMetric() );
			h.detector.detect(original);
			StabilityMetric m = h.getMetric();
			m.setOriginal(createDetectionList(h.detector));
		}

		// Add various amounts of noise to
		double sigmas[] = new double[5];
		for( int noiseLevel = 1; noiseLevel <= sigmas.length; noiseLevel++ ) {
			System.out.print("*");
			double sigma = sigmas[noiseLevel-1] = noiseLevel*sigmaLevel;
//			System.out.println("compute noise "+sigma);
			
			for( int i = 0; i < numTrials; i++ ) {
				T noisy = (T)original.clone();
				GeneralizedImageOps.addGaussian(noisy,rand,sigma);

				for( InterestMetrics<T> h : algs ) {
					h.detector.detect(noisy);
					StabilityMetric m = h.getMetric();
					computeScore( createDetectionList(h.detector) , m );
				}
			}

			for( InterestMetrics<T> h : algs ) {
				StabilityMetric m = h.getMetric();
				StabilityMetric.Score s = m.computeScore();
				m.scores.add(s);
				m.reset();
			}
		}

		System.out.println();
		printPercentMissed(algs, sigmas);
		printAverageMatched(algs,sigmas);

	}

	private void printPercentMissed(List<InterestMetrics<T>> algs, double[] sigmas) {
		// print the percent missed for each noise level
		System.out.println("Percent Missed:");
		System.out.print("Sigma      ");
		for( int i = 0; i < sigmas.length; i++ ) {
			System.out.printf("     %2d",(int)sigmas[i]);
		}
		System.out.println();
		for( InterestMetrics<T> h : algs ) {
			StabilityMetric m = h.getMetric();
			System.out.printf("%12s ",h.name);
			for( StabilityMetric.Score s : m.scores ) {
				System.out.printf(" %5.2f%%",100.0*s.fracMissed);
			}
			System.out.println();
		}
	}

	private void printAverageMatched(List<InterestMetrics<T>> algs, double[] sigmas) {
		// print the percent missed for each noise level
		System.out.println("Average Matched:");
		System.out.print("Sigma       ");
		for( int i = 0; i < sigmas.length; i++ ) {
			System.out.printf("    %2d",(int)sigmas[i]);
		}
		System.out.println();
		for( InterestMetrics<T> h : algs ) {
			StabilityMetric m = h.getMetric();
			System.out.printf("%12s ",h.name);
			for( StabilityMetric.Score s : m.scores ) {
				System.out.printf(" %5.1f",s.aveMatched);
			}
			System.out.println();
		}
	}

	private void computeScore(List<Point2D_I32> l, StabilityMetric h) {

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

		h.numMatched.add(h.original.size()-numMissed);
		h.numMissed.add(numMissed);
		h.error.add(total/(h.original.size()-numMissed));
	}

	public static <T extends ImageBase,D extends ImageBase>
	List<InterestMetrics<T>> createAlgs( BenchmarkInterestParameters<T,D> param )
	{
	    int radius = param.radius;
		Class<T> imageType = param.imageType;
		Class<D> derivType = param.derivType;
		int maxFeatures = param.maxFeatures;
		int maxScaleFeatures = param.maxScaleFeatures;
		double[] scales = param.scales;

		List<InterestMetrics<T>> ret = new ArrayList<InterestMetrics<T>>();

		GeneralFeatureDetector<T,D> alg;

		alg = FactoryCornerDetector.createFast(radius,20,maxFeatures,imageType);
		ret.add( new InterestMetrics<T>("Fast",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createHarris(radius,20,maxFeatures,derivType);
		ret.add( new InterestMetrics<T>("Harris",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createKlt(radius,20,maxFeatures,derivType);
		ret.add( new InterestMetrics<T>("KLT",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createKitRos(radius,20,maxFeatures,derivType);
		ret.add( new InterestMetrics<T>("KitRos",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryCornerDetector.createMedian(radius,20,maxFeatures,imageType);
		ret.add( new InterestMetrics<T>("Median",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryBlobDetector.createLaplace(radius,20,maxFeatures,derivType, HessianBlobIntensity.Type.DETERMINANT);
		ret.add( new InterestMetrics<T>("Hessian",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		alg = FactoryBlobDetector.createLaplace(radius,20,maxFeatures,derivType, HessianBlobIntensity.Type.TRACE);
		ret.add( new InterestMetrics<T>("Laplace",FactoryInterestPoint.fromCorner(alg,imageType,derivType)) );
		

		FeatureLaplaceScaleSpace<T,D> flss = FactoryInterestPointAlgs.hessianLaplace(radius,20,maxScaleFeatures,imageType,derivType);
		ret.add( new InterestMetrics<T>("Hess Lap SS",FactoryInterestPoint.fromFeatureLaplace(flss,scales,imageType)) );
		FeatureLaplacePyramid<T,D> flp = FactoryInterestPointAlgs.hessianLaplacePyramid(radius,20,maxScaleFeatures,imageType,derivType);
		ret.add( new InterestMetrics<T>("Hess Lap P",FactoryInterestPoint.fromFeatureLaplace(flp,scales,imageType)) );
		FeatureScaleSpace<T,D> fss = FactoryInterestPointAlgs.hessianScaleSpace(radius,20,maxScaleFeatures,imageType,derivType);
		ret.add( new InterestMetrics<T>("Hessian SS",FactoryInterestPoint.fromFeature(fss,scales,imageType)) );
		FeaturePyramid<T,D> fp = FactoryInterestPointAlgs.hessianPyramid(radius,20,maxScaleFeatures,imageType,derivType);
		ret.add( new InterestMetrics<T>("Hessian P",FactoryInterestPoint.fromFeature(fp,scales,imageType)) );
		ret.add( new InterestMetrics<T>("FastHessian",FactoryInterestPoint.fromFastHessian(maxScaleFeatures,9,4,4,imageType)) );

		return ret;
	}

	public List<Point2D_I32> createDetectionList( InterestPointDetector det ) {
		List<Point2D_I32> list = new ArrayList<Point2D_I32>();
		for( int i = 0; i < det.getNumberOfFeatures(); i++ ) {
			list.add(det.getLocation(i).copy());
		}
		return list;
	}


	public static void main( String args[] ) {
		// specify test images
		String imageNames[] = new String[]{"outdoors01.jpg","indoors01.jpg","scale/beach01.jpg","sunflowers.png"};

		// evaluate each image individually
		for( String s : imageNames ) {
			BufferedImage image = UtilImageIO.loadImage("evaluation/data/"+s);
			BenchmarkInterestStability_Noise<ImageFloat32,ImageFloat32> alg = new BenchmarkInterestStability_Noise<ImageFloat32,ImageFloat32>(ImageFloat32.class,ImageFloat32.class);

			System.out.println("\nEvaluating Image: "+s);
			alg.process(image);
		}
	}
}
