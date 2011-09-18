/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.describe.ExtractFeatureDescription;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.associate.ScoreAssociateEuclideanSq;
import boofcv.alg.feature.associate.ScoreAssociation;
import boofcv.alg.feature.orientation.OrientationImageAverage;
import boofcv.benchmark.feature.BenchmarkAlgorithm;
import boofcv.benchmark.feature.distort.StabilityEvaluatorPoint;
import boofcv.evaluation.ErrorStatistics;
import boofcv.factory.feature.associate.FactoryAssociationTuple;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDescQueue;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;


/**
 * Evaluates a descriptors using association and error based metrics.  Both "true" scale and
 * orientation are provided to the description extractors.
 *
 * @author Peter Abeles
 */
public class DescribeEvaluator<T extends ImageBase>
	extends StabilityEvaluatorPoint<T>
{
	// list of descriptions from the initial image
	TupleDescQueue initial;
	// list of descriptions from the current image being considered
	TupleDescQueue current;
	// list of features which maintain the original indexes
	List<TupleDesc_F64> initList = new ArrayList<TupleDesc_F64>();
	List<TupleDesc_F64> currentList = new ArrayList<TupleDesc_F64>();

	// transform from original index to the index in the queues
	int initIndexes[];
	int currentIndexes[];

	// estimates feature orientation
	OrientationImageAverage<T> orientationAlg;

	// saved feature orientation in first frame
	double theta[];
	
	// associates features together
	GeneralAssociation<TupleDesc_F64> matcher;

	// computes error statistics
	ErrorStatistics errors = new ErrorStatistics(500);

	public DescribeEvaluator(int borderSize, InterestPointDetector<T> detector,
									  OrientationImageAverage<T> orientationAlg ) {
		super(borderSize, detector);

		this.orientationAlg = orientationAlg;
		ScoreAssociation scorer = new ScoreAssociateEuclideanSq();

		matcher = FactoryAssociationTuple.maxError(scorer,100000);
	}

	@Override
	public void extractInitial(BenchmarkAlgorithm alg, T image, List<Point2D_I32> points)
	{
		if( initIndexes == null || initIndexes.length < points.size() ) {
			initIndexes = new int[ points.size() ];
			currentIndexes = new int[ points.size() ];
		}

		if( theta == null || theta.length < points.size() )
			theta = new double[ points.size() ];

		ExtractFeatureDescription<T> extract = alg.getAlgorithm();

		int descLength = extract.getDescriptionLength();

		initial = new TupleDescQueue(descLength, true);
		current = new TupleDescQueue(descLength, true);

		initialDescriptions(image, points, extract);
	}

	@Override
	public double[] evaluateImage(BenchmarkAlgorithm alg, T image,
							   double scale , double theta ,
							   List<Point2D_I32> points, List<Integer> indexes)
	{
		ExtractFeatureDescription<T> extract = alg.getAlgorithm();

		// extract descriptions from the current image
		currentDescriptions(image,extract,scale,theta,points,indexes);

		// compute association error
		double associationScore = computeAssociationScore(indexes);
		// compute description change error
		computeErrorScore(indexes);
		double p50 = errors.getFraction(0.5);
		double p90 = errors.getFraction(0.9);

		return new double[]{associationScore,p50*10,p90*10};
	}

	/**
	 * Extract feature descriptions from the initial image.  Calculates the
	 * feature's orientation.
	 */
	private void initialDescriptions(T image, List<Point2D_I32> points, ExtractFeatureDescription<T> extract) {
		extract.setImage(image);
		orientationAlg.setImage(image);

		initial.reset();
		initList.clear();
		TupleDesc_F64 f = initial.pop();
		for( int i = 0; i < points.size(); i++  ) {
			Point2D_I32 p = points.get(i);
			theta[i] = orientationAlg.compute(p.x,p.y);
			TupleDesc_F64 result = extract.process(p.x,p.y,theta[i],1,f);
			if( result != null ) {
				initIndexes[initial.size()-1] = i;
				initList.add(f);
				f = initial.pop();
			} else {
				initList.add(null);
			}
		}
		initial.removeTail();
	}

	/**
	 * extracts feature description from the current image.  Provide the
	 * description extractor the "true" orientation of the feature.
	 */
	private void currentDescriptions( T image ,ExtractFeatureDescription<T> extract ,
									  double scale , double theta ,
									  List<Point2D_I32> points, List<Integer> indexes ) {
		extract.setImage(image);
		current.reset();
		currentList.clear();
		TupleDesc_F64 f = current.pop();
		for( int i = 0; i < points.size(); i++  ) {
			Point2D_I32 p = points.get(i);
			// calculate the true orientation of the feature
			double ang = UtilAngle.bound(this.theta[indexes.get(i)] + theta);
			// extract the description
			TupleDesc_F64 result = extract.process(p.x,p.y,ang,scale,f);
			if( result != null ) {
				currentIndexes[current.size()-1] = i;
				currentList.add(f);
				f = current.pop();
			} else {
				currentList.add(null);
			}
		}
		current.removeTail();
	}

	private double computeAssociationScore(List<Integer> indexes) {
		matcher.associate(initial,current);

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

	private void computeErrorScore( List<Integer> indexes ) {
		errors.reset();
		for( int i = 0; i < indexes.size(); i++ ) {

			TupleDesc_F64 f = currentList.get(i);
			TupleDesc_F64 e = initList.get(indexes.get(i));

			if( f != null && e != null ) {
				// normalize the error based on the magnitude of the descriptor in the first frame
				// this adjusts the error for descriptor length and magnitude
				double errorNorm = errorNorm(f,e);
				double initNorm = norm(e);
				errors.add( errorNorm/initNorm );
			}
		}
	}

	private double norm( TupleDesc_F64 desc ) {
		double total = 0;
		for( int i = 0; i < desc.value.length; i++ ) {
			total += desc.value[i]*desc.value[i];
		}
		return Math.sqrt(total);
	}

	private double errorNorm( TupleDesc_F64 a , TupleDesc_F64 b  ) {
		double total = 0;
		for( int i = 0; i < a.value.length; i++ ) {
			double error = a.value[i] - b.value[i];
			total += error*error;
		}
		return Math.sqrt(total);
	}

	@Override
	public String[] getMetricNames() {
		return new String[]{"Correct %","50% * 10","90% * 10"};
	}
}
