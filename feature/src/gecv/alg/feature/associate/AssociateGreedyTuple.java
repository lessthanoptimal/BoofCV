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

package gecv.alg.feature.associate;

import gecv.struct.FastArray;
import gecv.struct.feature.TupleDesc_F64;


/**
 * Different variants of greedy association for objects described by a {@link TupleDesc_F64}.  An
 * object is associated with whichever object has the best fit score.  Each variant is different
 * in how it scores this fit.
 *
 * @author Peter Abeles
 */
public class AssociateGreedyTuple {

	/**
	 * Straight forward implementation where no fit score is returned and an association
	 * does not happen if the maximum error has been exceeded.
	 * 
	 * @param src The set of features which are being associated with 'dst'.
	 * @param dst A set of features.
	 * @param score Function used to score the quality of a possible association.
	 * @param maxFitError The minimum allowed fit score.
	 * @param pairs Index of the 'dst' feature a 'src' feature is associated with.  If an element
	 * has a value of -1 then no association was performed.  Must be src.size() long.
	 */
	public static void basic( FastArray<TupleDesc_F64> src ,
							  FastArray<TupleDesc_F64> dst ,
							  ScoreAssociateTuple score ,
							  double maxFitError ,
							  int pairs[] )
	{
		for( int i = 0; i < src.size; i++ ) {
			TupleDesc_F64 a = src.data[i];
			double bestScore = maxFitError;
			int bestIndex = -1;

			for( int j = 0; j < dst.size; j++ ) {
				TupleDesc_F64 b = src.data[j];

				double fit = score.score(a,b);

				if( fit < bestScore ) {
					bestIndex = j;
					bestScore = fit;
				}
			}
			pairs[i] = bestIndex;
		}
	}

	/**
	 * The features with the lowest error is associated.  The returned score is also the association
	 * error.
	 *
	 * @param src The set of features which are being associated with 'dst'.
	 * @param dst A set of features.
	 * @param score Function used to score the quality of a possible association.
	 * @param pairs Index of the 'dst' feature a 'src' feature is associated with.  Must be src.size() long.
	 * @param fitScore Quality of the associations fit.  Must be src.size() long.
	 */
	public static void fitIsError( FastArray<TupleDesc_F64> src ,
								   FastArray<TupleDesc_F64> dst ,
								   ScoreAssociateTuple score ,
								   int pairs[] ,
								   double fitScore[] )
	{
		for( int i = 0; i < src.size; i++ ) {
			TupleDesc_F64 a = src.data[i];
			double bestScore = Double.MAX_VALUE;
			int bestIndex = -1;

			for( int j = 0; j < dst.size; j++ ) {
				TupleDesc_F64 b = src.data[j];

				double fit = score.score(a,b);
				
				if( fit < bestScore ) {
					bestIndex = j;
					bestScore = fit;
				}
			}
			pairs[i] = bestIndex;
			fitScore[i] = bestScore;
		}
	}

	/**
	 * The features with the lowest error is associated.  The returned score is also the number
	 * of features which has a score closer to the best fit score.
	 *
	 * @param src The set of features which are being associated with 'dst'.
	 * @param dst A set of features.
	 * @param score Function used to score the quality of a possible association.
	 * @param containmentScale A feature is considered to be close to the best fit if it has a score that is containmentScale
	 * times as large.
	 * @param workBuffer Internal work buffer. Must be dst.size() long.
	 * @param pairs Index of the 'dst' feature a 'src' feature is associated with.  Must be src.size() long.
	 * @param fitScore Quality of the associations fit.  Must be src.size() long.
	 */
	public static void totalCloseMatches( FastArray<TupleDesc_F64> src ,
										  FastArray<TupleDesc_F64> dst ,
										  ScoreAssociateTuple score ,
										  double containmentScale ,
										  double workBuffer[] ,
										  int pairs[] ,
										  double fitScore[] )
	{
		for( int i = 0; i < src.size; i++ ) {
			TupleDesc_F64 a = src.data[i];
			double bestScore = Double.MAX_VALUE;
			int bestIndex = -1;

			for( int j = 0; j < dst.size; j++ ) {
				TupleDesc_F64 b = src.data[j];

				double fit = workBuffer[j] = score.score(a,b);

				if( fit < bestScore ) {
					bestIndex = j;
					bestScore = fit;
				}
			}

			// count the number of close matches
			double threshold = bestScore*containmentScale;
			int count = 0;
			for( int j = 0; j < dst.size; j++ ) {
				if( workBuffer[j] <= threshold )
					count++;
			}

			pairs[i] = bestIndex;
			fitScore[i] = count;
		}
	}

	/**
	 * Associates two the two features with the best score.  Then performs a reverse association and if
	 * a better fit is found the association is discarded.
	 *
	 * @param src The set of features which are being associated with 'dst'.
	 * @param dst A set of features.
	 * @param score Function used to score the quality of a possible association.
	 * @param workBuffer Internal work buffer. Must be dst.size()*src.size() long.
	 * @param pairs Index of the 'dst' feature a 'src' feature is associated with.  Must be src.size() long.
	 */
	public static void forwardBackwards( FastArray<TupleDesc_F64> src ,
										 FastArray<TupleDesc_F64> dst ,
										 ScoreAssociateTuple score ,
										 double workBuffer[] ,
										 int pairs[] )
	{
		// forward association
		int index = 0;
		for( int i = 0; i < src.size; i++ ) {
			TupleDesc_F64 a = src.data[i];

			double bestScore = Double.MAX_VALUE;
			int bestIndex = -1;
			for( int j = 0; j < dst.size; j++ ) {
				double s = workBuffer[index++] = score.score(a,src.data[j]);
				if( s < bestScore ) {
					bestScore = s;
					bestIndex = j;
				}
			}
			pairs[i] = bestIndex;
		}

		// validate by seeing if the reverse association has a better fit score or not
		for( int i = 0; i < src.size; i++ ) {
			int match = pairs[i];
			double scoreToBeat = workBuffer[i*dst.size+match];

			for( int j = 0; j < src.size; j++ , match += dst.size ) {
				if( workBuffer[match] < scoreToBeat ) {
					pairs[i] = -1;
					break;
				}
			}
		}
	}
}
