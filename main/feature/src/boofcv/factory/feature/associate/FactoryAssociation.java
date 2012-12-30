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

package boofcv.factory.feature.associate;

import boofcv.abst.feature.associate.*;
import boofcv.alg.feature.associate.AssociateGreedy;
import boofcv.struct.feature.*;


/**
 * Creates algorithms for associating {@link boofcv.struct.feature.TupleDesc_F64} features.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class FactoryAssociation {

	/**
	 * Returns an algorithm for associating features together which uses a brute force greedy algorithm.
	 * See {@link AssociateGreedy} for details.
	 * 
	 * @param score Computes the fit score between two features.
	 * @param maxError Maximum allowed error/fit score between two features.  To disable set to Double.MAX_VALUE
	 * @param maxMatches  Maximum number of matches returned.  If more than this are found then only the ones with the 
	 *                  best fit score are returned.  To disable set to a value <= 0.
	 * @param backwardsValidation If true associations are validated by associating in the reverse direction.  If the 
	 *                  forward and reverse matches fit an association is excepted.
	 * @param <D> Data structure being associated
	 * @return 
	 */
	public static <D> AssociateDescription<D>
	greedy( ScoreAssociation<D> score ,
			double maxError ,
			int maxMatches ,
			boolean backwardsValidation )
	{
		AssociateGreedy<D> alg = new AssociateGreedy<D>(score,maxError,backwardsValidation);
		WrapAssociateGreedy<D> ret = new WrapAssociateGreedy<D>(alg,maxMatches);
		return ret;
	}

	/**
	 * Given a feature descriptor type it returns a "reasonable" default {@link ScoreAssociation}.
	 *
	 * @param tupleType Class type which extends {@link boofcv.struct.feature.TupleDesc}
	 * @return A class which can score two potential associations
	 */
	public static <D>
	ScoreAssociation<D> defaultScore( Class<D> tupleType ) {
		if( NccFeature.class.isAssignableFrom(tupleType) ) {
			return (ScoreAssociation)new ScoreAssociateNccFeature();
		} else if( TupleDesc_F64.class.isAssignableFrom(tupleType) ) {
			return (ScoreAssociation)new ScoreAssociateEuclideanSq_F64();
		} else if( tupleType == TupleDesc_F32.class ) {
			return (ScoreAssociation)new ScoreAssociateEuclideanSq_F32();
		} else if( tupleType == TupleDesc_U8.class ) {
			return (ScoreAssociation)new ScoreAssociateSad_U8();
		} else if( tupleType == TupleDesc_B.class  ) {
			return (ScoreAssociation)new ScoreAssociateHamming_B();
		} else {
			throw new IllegalArgumentException("Unknown tuple type: "+tupleType);
		}
	}

	/**
	 * Scores features based on Sum of Absolute Difference (SAD).
	 *
	 * @param tupleType Type of descriptor being scored
	 * @return SAD scorer
	 */
	public static <D>
	ScoreAssociation<D> scoreSad( Class<D> tupleType ) {
		if( TupleDesc_F64.class.isAssignableFrom(tupleType) ) {
			return (ScoreAssociation)new ScoreAssociateSad_F64();
		} else if( tupleType == TupleDesc_F32.class ) {
			return (ScoreAssociation)new ScoreAssociateSad_F32();
		} else if( tupleType == TupleDesc_U8.class ) {
			return (ScoreAssociation)new ScoreAssociateSad_U8();
		} else if( tupleType == TupleDesc_S8.class ) {
			return (ScoreAssociation)new ScoreAssociateSad_S8();
		} else {
			throw new IllegalArgumentException("SAD score not supported for type "+tupleType.getSimpleName());
		}
	}

	/**
	 * Scores features based on their Normalized Cross-Correlation (NCC).
	 *
	 * @return NCC score
	 */
	public static ScoreAssociation<NccFeature> scoreNcc() {
		return new ScoreAssociateNccFeature();
	}

	/**
	 * Scores features based on the Euclidean distance between them.  The square is often used instead
	 * of the Euclidean distance since it is much faster to compute.
	 *
	 * @param tupleType Type of descriptor being scored
	 * @param squared IF true the distance squared is returned.  Usually true
	 * @return Euclidean distance measure
	 */
	public static <D>
	ScoreAssociation<D> scoreEuclidean( Class<D> tupleType , boolean squared ) {
		if( TupleDesc_F64.class.isAssignableFrom(tupleType) ) {
			if( squared )
				return (ScoreAssociation)new ScoreAssociateEuclideanSq_F64();
			else
				return (ScoreAssociation)new ScoreAssociateEuclidean_F64();
		} else if( tupleType == TupleDesc_F32.class ) {
			if( squared )
				return (ScoreAssociation)new ScoreAssociateEuclideanSq_F32();
		}

		throw new IllegalArgumentException("Euclidean score not yet supported for type "+tupleType.getSimpleName());
	}

	/**
	 * Hamming distance between two binary descriptors.
	 *
	 * @param tupleType Type of descriptor being scored
	 * @return Hamming distance measure
	 */
	public static <D>
	ScoreAssociation<D> scoreHamming( Class<D> tupleType ) {
		if( tupleType == TupleDesc_B.class ) {
			return (ScoreAssociation)new ScoreAssociateHamming_B();
		}

		throw new IllegalArgumentException("Hamming distance not yet supported for type "+tupleType.getSimpleName());
	}
}
