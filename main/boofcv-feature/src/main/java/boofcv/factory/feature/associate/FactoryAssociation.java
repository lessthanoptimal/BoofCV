/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.describe.DescriptorInfo;
import boofcv.alg.descriptor.KdTreeTuple_F32;
import boofcv.alg.descriptor.KdTreeTuple_F64;
import boofcv.alg.feature.associate.*;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.ConfigLength;
import boofcv.struct.feature.*;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.alg.KdTreeDistance;
import org.jetbrains.annotations.Nullable;

/**
 * Creates algorithms for associating {@link boofcv.struct.feature.TupleDesc_F64} features.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class FactoryAssociation {
	public static <D extends TupleDesc<D>> AssociateDescription<D> generic( ConfigAssociate config, DescriptorInfo<D> info ) {
		int DOF = info.createDescription().size();

		return switch (config.type) {
			case GREEDY -> {
				ScoreAssociation<D> scorer = FactoryAssociation.defaultScore(info.getDescriptionType());
				yield FactoryAssociation.greedy(config.greedy, scorer);
			}
			case KD_TREE -> (AssociateDescription)FactoryAssociation.kdtree(config.nearestNeighbor, DOF);
			case RANDOM_FOREST -> (AssociateDescription)FactoryAssociation.kdRandomForest(
					config.nearestNeighbor, DOF, 10, 5, 1233445565);
			default -> throw new IllegalArgumentException("Unknown association: " + config.type);
		};
	}

	/**
	 * Creates a generic association algorithm that uses descriptors as well as the 2D location of
	 * image features.
	 */
	public static <D> AssociateDescription2D<D> generic2( ConfigAssociate config, DescriptorInfo info ) {
		if (config.maximumDistancePixels.fraction == 1.0) {
			return new AssociateDescTo2D<>(generic(config, info));
		}

		// only greedy is supported at this time
		if (config.type == ConfigAssociate.AssociationType.GREEDY) {
			ScoreAssociation<D> scorer = FactoryAssociation.defaultScore(info.getDescriptionType());
			return FactoryAssociation.greedy2D(config.greedy, config.maximumDistancePixels, scorer);
		}
		throw new IllegalArgumentException("Unknown association: " + config.type);
	}

	/**
	 * Checks and if necessary wraps the association to ensure that it returns only unique associations
	 */
	public static <D> AssociateDescription<D> ensureUnique( AssociateDescription<D> associate ) {
		if (!associate.uniqueDestination() || !associate.uniqueSource()) {
			return new EnforceUniqueByScore.Describe<>(associate, true, true);
		} else {
			return associate;
		}
	}

	/**
	 * Checks and if necessary wraps the association to ensure that it returns only unique associations
	 */
	public static <D> AssociateDescription2D<D> ensureUnique( AssociateDescription2D<D> associate ) {
		if (!associate.uniqueDestination() || !associate.uniqueSource()) {
			return new EnforceUniqueByScore.Describe2D<>(associate, true, true);
		} else {
			return associate;
		}
	}

	/**
	 * Returns an algorithm for associating features together which uses a brute force greedy algorithm.
	 * See {@link AssociateGreedyDesc} for details.
	 *
	 * @param score Computes the fit score between two features.
	 * @param config Configuration
	 * @param <D> Data structure being associated
	 * @return AssociateDescription
	 */
	public static <D> AssociateDescription<D>
	greedy( @Nullable ConfigAssociateGreedy config, ScoreAssociation<D> score ) {
		if (config == null)
			config = new ConfigAssociateGreedy();
		AssociateGreedyDescBase<D> alg;

		if (BoofConcurrency.USE_CONCURRENT) {
			alg = new AssociateGreedyDesc_MT<>(score);
		} else {
			alg = new AssociateGreedyDesc<>(score);
		}

		alg.setBackwardsValidation(config.forwardsBackwards);
		alg.setMaxFitError(config.maxErrorThreshold);
		alg.setRatioTest(config.scoreRatioThreshold);
		return new WrapAssociateGreedy<>(alg);
	}

	/**
	 * Returns an algorithm for associating features together which uses a brute force greedy algorithm.
	 * See {@link AssociateGreedyDesc} for details.
	 *
	 * @param score Computes the fit score between two features.
	 * @param config Configuration
	 * @param maxDistance Specifies maximum distance allowed.
	 * @param <D> Data structure being associated
	 * @return AssociateDescription
	 */
	public static <D> AssociateDescription2D<D>
	greedy2D( @Nullable ConfigAssociateGreedy config, ConfigLength maxDistance, ScoreAssociation<D> score ) {
		if (config == null)
			config = new ConfigAssociateGreedy();

		AssociateImageDistanceFunction distance = new AssociateImageDistanceEuclideanSq();

		AssociateGreedyBase2D<D> alg;
		if (BoofConcurrency.USE_CONCURRENT) {
			alg = new AssociateGreedyBruteForce2D_MT<>(score, distance);
		} else {
			alg = new AssociateGreedyBruteForce2D<>(score, distance);
		}

		// square distance since that's what the distance measure returns
		alg.getMaxDistanceLength().setTo(maxDistance);
		alg.setBackwardsValidation(config.forwardsBackwards);
		alg.setMaxFitError(config.maxErrorThreshold);
		alg.setRatioTest(config.scoreRatioThreshold);

		return new WrapAssociateGreedy2D<D>(alg);
	}

	/**
	 * Approximate association using a K-D tree degree of moderate size (10-15) that uses a best-bin-first search
	 * order.
	 *
	 * @param dimension Number of elements in the feature vector
	 * @return Association using approximate nearest neighbor
	 * @see AssociateNearestNeighbor_ST
	 * @see org.ddogleg.nn.alg.searches.KdTreeSearch1Bbf
	 */
	public static AssociateDescription<TupleDesc_F64> kdtree(
			@Nullable ConfigAssociateNearestNeighbor configNN, int dimension ) {
		if (configNN == null)
			configNN = new ConfigAssociateNearestNeighbor();
		NearestNeighbor nn = FactoryNearestNeighbor.kdtree(new KdTreeTuple_F64(dimension), configNN.maxNodesSearched);

		return associateNearestNeighbor(configNN, nn);
	}

	/**
	 * Approximate association using multiple random K-D trees (random forest) for descriptors with a high degree of
	 * freedom, e.g. &gt; 20
	 *
	 * @param dimension Number of elements in the feature vector
	 * @param numTrees Number of trees that are considered. Try 10 and tune.
	 * @param numConsiderSplit Number of nodes that are considered when generating a tree. Must be less than the
	 * point's dimension. Try 5
	 * @param randomSeed Seed used by random number generator
	 * @return Association using approximate nearest neighbor
	 * @see AssociateNearestNeighbor_ST
	 * @see org.ddogleg.nn.wrap.KdForestBbfNearestNeighbor
	 */
	public static AssociateDescription<TupleDesc_F64> kdRandomForest( @Nullable ConfigAssociateNearestNeighbor configNN,
																	  int dimension,
																	  int numTrees,
																	  int numConsiderSplit,
																	  long randomSeed ) {
		if (configNN == null)
			configNN = new ConfigAssociateNearestNeighbor();
		NearestNeighbor nn = FactoryNearestNeighbor.kdRandomForest(
				new KdTreeTuple_F64(dimension),
				configNN.maxNodesSearched, numTrees, numConsiderSplit, randomSeed);

		return associateNearestNeighbor(configNN, nn);
	}

	public static <TD extends TupleDesc<TD>> KdTreeDistance<TD> kdtreeDistance( int dof, Class<TD> type ) {
		if (type == TupleDesc_F64.class) {
			return (KdTreeDistance)new KdTreeTuple_F64(dof);
		} else if (type == TupleDesc_F32.class) {
			return (KdTreeDistance)new KdTreeTuple_F32(dof);
		} else {
			throw new IllegalArgumentException("Type isn't known yet");
		}
	}

	public static AssociateNearestNeighbor<TupleDesc_F64>
	associateNearestNeighbor( @Nullable ConfigAssociateNearestNeighbor config, NearestNeighbor nn ) {
		if (config == null)
			config = new ConfigAssociateNearestNeighbor();

		config.checkValidity();

		AssociateNearestNeighbor<TupleDesc_F64> assoc;
		if (BoofConcurrency.USE_CONCURRENT) {
			assoc = new AssociateNearestNeighbor_MT<>(nn, TupleDesc_F64.class);
		} else {
			assoc = new AssociateNearestNeighbor_ST<>(nn, TupleDesc_F64.class);
		}
		assoc.setRatioUsesSqrt(config.distanceIsSquared);
		assoc.setMaxScoreThreshold(config.maxErrorThreshold);
		assoc.setScoreRatioThreshold(config.scoreRatioThreshold);
		return assoc;
	}

	/**
	 * Given a feature descriptor type it returns a "reasonable" default {@link ScoreAssociation}.
	 *
	 * @param tupleType Class type which extends {@link boofcv.struct.feature.TupleDesc}
	 * @return A class which can score two potential associations
	 */
	public static <D> ScoreAssociation<D> defaultScore( Class<D> tupleType ) {
		if (NccFeature.class.isAssignableFrom(tupleType)) {
			return (ScoreAssociation)new ScoreAssociateNccFeature();
		} else if (TupleDesc_F64.class.isAssignableFrom(tupleType)) {
			return (ScoreAssociation)new ScoreAssociateEuclideanSq.F64();
		} else if (tupleType == TupleDesc_F32.class) {
			return (ScoreAssociation)new ScoreAssociateEuclideanSq.F32();
		} else if (tupleType == TupleDesc_U8.class) {
			return (ScoreAssociation)new ScoreAssociateSad.U8();
		} else if (tupleType == TupleDesc_B.class) {
			return (ScoreAssociation)new ScoreAssociateHamming_B();
		} else {
			throw new IllegalArgumentException("Unknown tuple type: " + tupleType);
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
		if (TupleDesc_F64.class.isAssignableFrom(tupleType)) {
			return (ScoreAssociation)new ScoreAssociateSad.F64();
		} else if (tupleType == TupleDesc_F32.class) {
			return (ScoreAssociation)new ScoreAssociateSad.F32();
		} else if (tupleType == TupleDesc_U8.class) {
			return (ScoreAssociation)new ScoreAssociateSad.U8();
		} else if (tupleType == TupleDesc_S8.class) {
			return (ScoreAssociation)new ScoreAssociateSad.S8();
		} else {
			throw new IllegalArgumentException("SAD score not supported for type " + tupleType.getSimpleName());
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
	 * Scores features based on the Euclidean distance between them. The square is often used instead
	 * of the Euclidean distance since it is much faster to compute.
	 *
	 * @param tupleType Type of descriptor being scored
	 * @param squared IF true the distance squared is returned. Usually true
	 * @return Euclidean distance measure
	 */
	public static <D> ScoreAssociation<D> scoreEuclidean( Class<D> tupleType, boolean squared ) {
		if (TupleDesc_F64.class.isAssignableFrom(tupleType)) {
			if (squared)
				return (ScoreAssociation)new ScoreAssociateEuclideanSq.F64();
			else
				return (ScoreAssociation)new ScoreAssociateEuclidean_F64();
		} else if (tupleType == TupleDesc_F32.class) {
			if (squared)
				return (ScoreAssociation)new ScoreAssociateEuclideanSq.F32();
		}

		throw new IllegalArgumentException("Euclidean score not yet supported for type " + tupleType.getSimpleName());
	}

	/**
	 * Hamming distance between two binary descriptors.
	 *
	 * @param tupleType Type of descriptor being scored
	 * @return Hamming distance measure
	 */
	public static <D> ScoreAssociation<D> scoreHamming( Class<D> tupleType ) {
		if (tupleType == TupleDesc_B.class) {
			return (ScoreAssociation)new ScoreAssociateHamming_B();
		}

		throw new IllegalArgumentException("Hamming distance not yet supported for type " + tupleType.getSimpleName());
	}
}
