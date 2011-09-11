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

package boofcv.alg.feature.describe.stability;

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.describe.ExtractFeatureDescription;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.alg.feature.associate.ScoreAssociateEuclideanSq;
import boofcv.alg.feature.associate.ScoreAssociateTuple;
import boofcv.alg.feature.benchmark.StabilityAlgorithm;
import boofcv.alg.feature.benchmark.StabilityEvaluatorPoint;
import boofcv.factory.feature.associate.FactoryAssociationTuple;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDescQueue;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_I32;

import java.util.List;


/**
 * Evaluates a descriptors ability to select the correct corresponding point.
 *
 * @author Peter Abeles
 */
public class DescribeAssociateEvaluator<T extends ImageBase>
	extends StabilityEvaluatorPoint<T>
{
	// list of descriptions from the initial image
	TupleDescQueue initial;
	// list of descriptions from the current image being considered
	TupleDescQueue current;
	// 
	int initIndexes[];
	int currentIndexes[];

	GeneralAssociation<TupleDesc_F64> matcher;

	public DescribeAssociateEvaluator(int borderSize, InterestPointDetector<T> detector) {
		super(borderSize, detector);

		ScoreAssociateTuple scorer = new ScoreAssociateEuclideanSq();

		matcher = FactoryAssociationTuple.maxError(scorer,100000);
	}

	@Override
	public void extractInitial(StabilityAlgorithm alg, T image, List<Point2D_I32> points)
	{
		if( initIndexes == null || initIndexes.length < points.size() ) {
			initIndexes = new int[ points.size() ];
			currentIndexes = new int[ points.size() ];
		}

		ExtractFeatureDescription<T> extract = alg.getAlgorithm()
				;
		int descLength = extract.getDescriptionLength();

		initial = new TupleDescQueue(descLength, true);
		current = new TupleDescQueue(descLength, true);

		extract.setImage(image);
		createDescriptionList(points, 1.0 , extract, initial , initIndexes );
	}

	private void createDescriptionList(List<Point2D_I32> points,
									   double scale ,
									   ExtractFeatureDescription<T> extract ,
									   TupleDescQueue queue ,
									   int indexes[] ) {
		queue.reset();
		TupleDesc_F64 f = queue.pop();
		for( int i = 0; i < points.size(); i++  ) {
			Point2D_I32 p = points.get(i);
			TupleDesc_F64 result = extract.process(p.x,p.y,0,scale,f);
			if( result != null ) {
				indexes[queue.size()-1] = i;
				f = queue.pop();
			}
		}
		queue.removeTail();
	}

	@Override
	public double[] evaluateImage(StabilityAlgorithm alg, T image,
							   double scale , double theta ,
							   List<Point2D_I32> points, List<Integer> indexes)
	{
		ExtractFeatureDescription<T> extract = alg.getAlgorithm();

		extract.setImage(image);
		createDescriptionList(points, scale , extract, current , currentIndexes );

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

		return new double[]{matches.size(),100.0*((double)numCorrect/(double)matches.size())};
	}


	@Override
	public String[] getMetricNames() {
		return new String[]{"# Matches","Correct %"};
	}
}
