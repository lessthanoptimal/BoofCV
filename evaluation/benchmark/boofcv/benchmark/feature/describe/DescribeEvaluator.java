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

package boofcv.benchmark.feature.describe;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.UtilFeature;
import boofcv.alg.feature.orientation.OrientationImageAverage;
import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.benchmark.feature.distort.StabilityEvaluatorPoint;
import boofcv.evaluation.ErrorStatistics;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;


/**
 * Evaluates a descriptors using association and error based metrics.  Both "true" scale and
 * orientation are provided to the description extractors.
 *
 * @author Peter Abeles
 */
public class DescribeEvaluator<T extends ImageSingleBand, D extends TupleDesc>
	extends StabilityEvaluatorPoint<T>
{
	// list of descriptions from the initial image
	FastQueue<D> initial;
	// list of descriptions from the current image being considered
	FastQueue<D> current;
	// list of features which maintain the original indexes
	List<D> initList = new ArrayList<D>();
	List<D> currentList = new ArrayList<D>();

	// transform from original index to the index in the queues
	int initIndexes[];
	int currentIndexes[];

	// estimates feature orientation, only in the initial image.
	// the true transform is used in the current image
	OrientationImageAverage<T> orientationAlg;

	// saved feature orientation in first frame
	double theta[];
	
	// associates features together
	AssociateDescription<D> matcher;

	// computes error statistics
	ErrorStatistics errors = new ErrorStatistics(500);

	public DescribeEvaluator(int borderSize, InterestPointDetector<T> detector,
									  OrientationImageAverage<T> orientationAlg ) {
		super(borderSize, detector);

		this.orientationAlg = orientationAlg;
	}

	@Override
	public void extractInitial(BenchmarkAlgorithm alg, T image, List<Point2D_F64> points)
	{
		if( initIndexes == null || initIndexes.length < points.size() ) {
			initIndexes = new int[ points.size() ];
			currentIndexes = new int[ points.size() ];
		}

		if( theta == null || theta.length < points.size() )
			theta = new double[ points.size() ];

		DescribeRegionPoint<T,D> describe = alg.getAlgorithm();
		ScoreAssociation<D> scorer = FactoryAssociation.defaultScore(describe.getDescriptionType());

		matcher = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true);

		initial = UtilFeature.createQueue(describe,10);
		current = UtilFeature.createQueue(describe,10);

		initialDescriptions(image, points, describe);
	}

	@Override
	public double[] evaluateImage(BenchmarkAlgorithm alg, T image,
								  double scale , double theta ,
								  List<Point2D_F64> points, List<Integer> indexes)
	{
		DescribeRegionPoint<T,D> extract = alg.getAlgorithm();

		// extract descriptions from the current image
		currentDescriptions(image,extract,scale,theta,points,indexes);

		// compute association error
		double associationScore = computeAssociationScore(indexes);
		// compute description change error
		computeErrorScore(indexes);
		double p50 = errors.getFraction(0.5);
		double p90 = errors.getFraction(0.9);

		return new double[]{current.size(),matcher.getMatches().size(),associationScore,p50*10,p90*10};
	}

	/**
	 * Extract feature descriptions from the initial image.  Calculates the
	 * feature's orientation.
	 */
	private void initialDescriptions(T image, List<Point2D_F64> points, DescribeRegionPoint<T,D> extract) {
		extract.setImage(image);
		orientationAlg.setImage(image);

		initial.reset();
		initList.clear();

		for( int i = 0; i < points.size(); i++  ) {
			Point2D_F64 p = points.get(i);
			theta[i] = orientationAlg.compute(p.x,p.y);

			D desc = initial.grow();
			if( extract.process(p.x, p.y, theta[i], 1, desc) ) {
				initList.add(initial.getTail());
				initIndexes[initial.size()-1] = i;
			} else {
				initial.removeTail();
				initList.add(null);
			}
		}
	}

	/**
	 * extracts feature description from the current image.  Provide the
	 * description extractor the "true" orientation of the feature.
	 */
	private void currentDescriptions( T image ,DescribeRegionPoint<T,D> extract ,
									  double scale , double theta ,
									  List<Point2D_F64> points, List<Integer> indexes ) {
		extract.setImage(image);
		current.reset();
		currentList.clear();

		for( int i = 0; i < points.size(); i++  ) {
			Point2D_F64 p = points.get(i);
			// calculate the true orientation of the feature
			double ang = UtilAngle.bound(this.theta[indexes.get(i)] + theta);

			// extract the description
			D desc = current.grow();
			if( extract.process(p.x, p.y, ang, scale, desc) ) {
				currentList.add(current.getTail());
				currentIndexes[current.size()-1] = i;
			} else {
				current.removeTail();
				currentList.add(null);
			}
		}
	}

	private double computeAssociationScore(List<Integer> indexes) {
		matcher.setSource(initial);
		matcher.setDestination(current);
		matcher.associate();

		FastQueue<AssociatedIndex> matches =  matcher.getMatches();

		int numCorrect = 0;
		for( int i = 0; i < matches.size() ; i++ ) {
			AssociatedIndex a = matches.get(i);

			int expected = initIndexes[a.src];
			int found = indexes.get(currentIndexes[a.dst]);

			if( expected == found ) {
				numCorrect++;
			}
		}

		if( matches.size() > 0 )
			return 100.0*((double)numCorrect/(double)matches.size());
		else
			return 0;
	}

	private void computeErrorScore( List<Integer> currentToInitialIndex ) {
		errors.reset();
		for( int i = 0; i < currentToInitialIndex.size(); i++ ) {

			D f = currentList.get(i);
			D e = initList.get(currentToInitialIndex.get(i));

			if( f != null && e != null ) {
				// normalize the error based on the magnitude of the descriptor in the first frame
				// this adjusts the error for descriptor length and magnitude
				double errorNorm = errorNorm(f,e);
				double initNorm = norm(e);
				errors.add( errorNorm/initNorm );

//				System.out.println("Error["+i+"] "+(errorNorm/initNorm));
			}
		}
	}

	private double norm( TupleDesc desc ) {
		double total = 0;
		for( int i = 0; i < desc.size(); i++ ) {
			total += desc.getDouble(i)*desc.getDouble(i);
		}
		return Math.sqrt(total);
	}

	private double errorNorm( TupleDesc a , TupleDesc b  ) {
		double total = 0;
		for( int i = 0; i < a.size(); i++ ) {
			double error = a.getDouble(i) - b.getDouble(i);
			total += error*error;
		}
		return Math.sqrt(total);
	}

	@Override
	public String[] getMetricNames() {
		return new String[]{"num curr","matches","Correct %","50% * 10","90% * 10"};
	}
}
