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

package gecv.abst.feature.associate;

import gecv.alg.feature.associate.AssociateGreedyTuple;
import gecv.alg.feature.associate.ScoreAssociateTuple;
import gecv.struct.FastArray;
import gecv.struct.feature.AssociatedIndex;
import gecv.struct.feature.TupleDesc_F64;
import pja.sorting.QuickSelectArray;


/**
 * Wrapper around algorithms contained inside of {@link AssociateGreedyTuple}.
 *
 * @author Peter Abeles
 */
public abstract class WrapAssociateGreedyTuple implements GeneralAssociation<TupleDesc_F64> {

	ScoreAssociateTuple score;
	FastArray<AssociatedIndex> matches = new FastArray<AssociatedIndex>(10,AssociatedIndex.class);

	public void setScore(ScoreAssociateTuple score) {
		this.score = score;
	}

	@Override
	public FastArray<AssociatedIndex> getMatches() {
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
		double threshold = QuickSelectArray.select(matchScore,maxMatches,sizeSrc);

		matches.reset();
		for( int i = 0; i < sizeSrc; i++ ) {
			int index = pairs[i];
			double s = matchScore[i];
			if( index >= 0 && s <= threshold ) {
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
		public void associate(FastArray<TupleDesc_F64> listSrc, FastArray<TupleDesc_F64> listDst) {
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
		public void associate(FastArray<TupleDesc_F64> listSrc, FastArray<TupleDesc_F64> listDst) {
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
		public void associate(FastArray<TupleDesc_F64> listSrc, FastArray<TupleDesc_F64> listDst) {
			if( pairs == null || pairs.length < listSrc.size ) {
				pairs = new int[ listSrc.size ];
				fitScore = new double[ listSrc.size ];
				workBuffer = new double[ listSrc.size ];
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
		public void associate(FastArray<TupleDesc_F64> listSrc, FastArray<TupleDesc_F64> listDst) {
			if( pairs == null || pairs.length < listSrc.size ) {
				pairs = new int[ listSrc.size ];
				fitScore = new double[ listSrc.size ];
			}
			if( workBuffer == null || workBuffer.length < listSrc.size*listDst.size ) {
				workBuffer = new double[ listSrc.size*listDst.size ];
			}
			AssociateGreedyTuple.forwardBackwards(listSrc,listDst, score,workBuffer,pairs, fitScore);
			// todo make sure unmatched pairs have a high score
			extractMatches(listSrc.size,pairs,fitScore,maxMatches);
		}
	}
}
