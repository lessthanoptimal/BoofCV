/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;


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
	 * @param backwardsValidation If true associations are validated by associating in the reverse direction.  If the 
	 *                  forward and reverse matches fit an association is excepted.
	 * @param <D> Data structure being associated
	 * @return AssociateDescription
	 */
	public static <D> AssociateDescription<D>
	greedy( ScoreAssociation<D> score ,
			double maxError ,
			boolean backwardsValidation )
	{
		AssociateGreedy<D> alg = new AssociateGreedy<>(score, backwardsValidation);
		alg.setMaxFitError(maxError);
		WrapAssociateGreedy<D> ret = new WrapAssociateGreedy<>(alg);
		return ret;
	}


	/**
	 * Approximate association using a K-D tree degree of moderate size (10-15) that uses a best-bin-first search
	 * order.
	 *
	 * @see AssociateNearestNeighbor
	 * @see org.ddogleg.nn.alg.KdTreeSearch1Bbf
	 *
	 * @param dimension Number of elements in the feature vector
	 * @param maxNodesSearched  Maximum number of nodes it will search.  Controls speed and accuracy.
	 * @return Association using approximate nearest neighbor
	 */
	public static AssociateDescription<TupleDesc_F64> kdtree( int dimension, int maxNodesSearched ) {
		NearestNeighbor nn = FactoryNearestNeighbor.kdtree(maxNodesSearched);

		return new AssociateNearestNeighbor<>(nn, dimension);
	}

	/**
	 * Approximate association using multiple random K-D trees (random forest) for descriptors with a high degree of
	 * freedom, e.g. &gt; 20
	 *
	 * @see AssociateNearestNeighbor
	 * @see org.ddogleg.nn.wrap.KdForestBbfSearch
	 *
	 * @param dimension Number of elements in the feature vector
	 * @param maxNodesSearched  Maximum number of nodes it will search.  Controls speed and accuracy.
	 * @param numTrees Number of trees that are considered.  Try 10 and tune.
	 * @param numConsiderSplit Number of nodes that are considered when generating a tree.  Must be less than the
	 *                         point's dimension.  Try 5
	 * @param randomSeed Seed used by random number generator
	 * @return Association using approximate nearest neighbor
	 */
	public static AssociateDescription<TupleDesc_F64> kdRandomForest( int dimension,
																	  int maxNodesSearched ,
																	  int numTrees ,
																	  int numConsiderSplit ,
																	  long randomSeed) {
		NearestNeighbor nn = FactoryNearestNeighbor.kdRandomForest(
				maxNodesSearched,numTrees,numConsiderSplit,randomSeed);

		return new AssociateNearestNeighbor<>(nn, dimension);
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
