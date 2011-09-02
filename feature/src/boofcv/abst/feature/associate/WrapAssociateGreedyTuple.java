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

package boofcv.abst.feature.associate;

import boofcv.alg.feature.associate.AssociateGreedyTuple;
import boofcv.alg.feature.associate.ScoreAssociateTuple;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import pja.sorting.QuickSelectArray;


/**
 * Wrapper around algorithms contained inside of {@link AssociateGreedyTuple}.
 *
 * @author Peter Abeles
 */
public abstract class WrapAssociateGreedyTuple implements GeneralAssociation<TupleDesc_F64> {

	ScoreAssociateTuple score;
	FastQueue<AssociatedIndex> matches = new FastQueue<AssociatedIndex>(10,AssociatedIndex.class,true);
	double copy[];

	public void setScore(ScoreAssociateTuple score) {
		this.score = score;
	}

	@Override
	public FastQueue<AssociatedIndex> getMatches() {
		return matches;
	}

	protected void extractMatches( int sizeSrc,
								   int pairs[] )
	{
		matches.reset();
		for( int i = 0; i < sizeSrc; i++ ) {
			int index = pairs[i];
			if( index >= 0 ) {
				matches.pop().setAssociation(i,index);
			}
		}
	}

	protected void extractMatches( int sizeSrc,
								   int pairs[] ,
								   double matchScore[],
								   int maxMatches )
	{
		matches.reset();
		if( sizeSrc < maxMatches ) {
			// put all the matches in
			addAllMatches(sizeSrc, pairs);
			return;
		}

		// copy the score so that the original is not modified
		if( copy == null || copy.length < sizeSrc ) {
			copy = matchScore.clone();
		} else {
			System.arraycopy(matchScore,0,copy,0,sizeSrc);
		}
		double threshold = QuickSelectArray.select(copy,maxMatches,sizeSrc);

		for( int i = 0; i < sizeSrc && matches.size() < maxMatches; i++ ) {
			int index = pairs[i];
			double s = matchScore[i];
			if( index >= 0 && s <= threshold ) {
				matches.pop().setAssociation(i,index);
			}
		}
	}

	private void addAllMatches(int sizeSrc, int pairs[]) {
		matches.reset();
		for( int i = 0; i < sizeSrc ; i++ ) {
			int index = pairs[i];
			if( index >= 0 ) {
				matches.pop().setAssociation(i,index);
			}
		}
	}

	public static class Basic extends WrapAssociateGreedyTuple
	{
		int pairs[];
		double maxFitError;

		public Basic(double maxFitError) {
			this.maxFitError = maxFitError;
		}

		@Override
		public void associate(FastQueue<TupleDesc_F64> listSrc, FastQueue<TupleDesc_F64> listDst) {
			if( pairs == null || pairs.length < listSrc.size ) {
				pairs = new int[ listSrc.size ];
			}
			AssociateGreedyTuple.basic(listSrc,listDst,score,maxFitError,pairs);
			extractMatches(listSrc.size,pairs);
		}
	}

	public static class FitIsError extends WrapAssociateGreedyTuple
	{
		int pairs[];
		double fitScore[];
		int maxMatches;

		public FitIsError(int maxMatches) {
			this.maxMatches = maxMatches;
		}

		@Override
		public void associate(FastQueue<TupleDesc_F64> listSrc, FastQueue<TupleDesc_F64> listDst) {
			if( pairs == null || pairs.length < listSrc.size ) {
				pairs = new int[ listSrc.size ];
				fitScore = new double[ listSrc.size ];
			}
			AssociateGreedyTuple.fitIsError(listSrc,listDst, score,pairs, fitScore);
			extractMatches(listSrc.size,pairs,fitScore,maxMatches);
		}
	}

	public static class TotalCloseMatches extends WrapAssociateGreedyTuple
	{
		int pairs[];
		double fitScore[];
		double workBuffer[];
		int maxMatches;
		double containmentScale;

		public TotalCloseMatches(int maxMatches, double containmentScale ) {
			this.maxMatches = maxMatches;
			this.containmentScale = containmentScale;
		}

		@Override
		public void associate(FastQueue<TupleDesc_F64> listSrc, FastQueue<TupleDesc_F64> listDst) {
			if( pairs == null || pairs.length < listSrc.size ) {
				pairs = new int[ listSrc.size ];
				fitScore = new double[ listSrc.size ];
				workBuffer = new double[ listDst.size ];
			}
			if( workBuffer.length < listDst.size ) {
				workBuffer = new double[ listDst.size ];
			}
			AssociateGreedyTuple.totalCloseMatches(listSrc,listDst, score,containmentScale,workBuffer,pairs, fitScore);
			extractMatches(listSrc.size,pairs,fitScore,maxMatches);
		}
	}

	public static class ForwardBackwards extends WrapAssociateGreedyTuple
	{
		int pairs[];
		double fitScore[];
		double workBuffer[];
		int maxMatches;

		public ForwardBackwards(int maxMatches) {
			this.maxMatches = maxMatches;
		}

		@Override
		public void associate(FastQueue<TupleDesc_F64> listSrc, FastQueue<TupleDesc_F64> listDst) {
			if( pairs == null || pairs.length < listSrc.size ) {
				pairs = new int[ listSrc.size ];
				fitScore = new double[ listSrc.size ];
			}
			if( workBuffer == null || workBuffer.length < listSrc.size*listDst.size ) {
				workBuffer = new double[ listSrc.size*listDst.size ];
			}
			AssociateGreedyTuple.forwardBackwards(listSrc,listDst, score,workBuffer,pairs, fitScore);
			extractMatches(listSrc.size,pairs,fitScore,maxMatches);
		}
	}
}
