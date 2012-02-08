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

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.associate.WrapAssociateGreedy;
import boofcv.alg.feature.associate.AssociateGreedy;
import boofcv.alg.feature.associate.ScoreAssociation;


/**
 * Creates algorithms for associating {@link boofcv.struct.feature.TupleDesc_F64} features.
 *
 * @author Peter Abeles
 */
public class FactoryAssociation {

	/**
	 * Returns an algorithm for associating features together which uses a brute force greedy algorithm.
	 * See {@link AssociateGreedy} for details.
	 * 
	 * @param score Computes the fit score between two features.
	 * @param maxError Maximum allowed error/fit score between two features
	 * @param maxMatches  Maximum number of matches returned.  If more than this are found then only the ones with the 
	 *                  best fit score are returned.
	 * @param backwardsValidation If true associations are validated by associating in the reverse direction.  If the 
	 *                  forward and reverse matches fit an association is excepted.
	 * @param <D> Data structure being associated
	 * @return 
	 */
	public static <D> GeneralAssociation<D>
	greedy( ScoreAssociation<D> score ,
			double maxError ,
			int maxMatches ,
			boolean backwardsValidation )
	{
		AssociateGreedy<D> alg = new AssociateGreedy<D>(score,maxError,backwardsValidation);
		WrapAssociateGreedy<D> ret = new WrapAssociateGreedy<D>(alg,maxMatches);
		return ret;
	}
}
