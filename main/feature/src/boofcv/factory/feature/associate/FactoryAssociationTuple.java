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

package boofcv.factory.feature.associate;

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.associate.WrapAssociateGreedy;
import boofcv.alg.feature.associate.AssociateGreedy;
import boofcv.alg.feature.associate.ScoreAssociation;
import boofcv.struct.feature.TupleDesc_F64;


/**
 * Creates algorithms for associating {@link boofcv.struct.feature.TupleDesc_F64} features.
 *
 * @author Peter Abeles
 */
public class FactoryAssociationTuple {

	public static GeneralAssociation<TupleDesc_F64>
	greedy( ScoreAssociation<TupleDesc_F64> score ,
			double maxError ,
			int maxMatches ,
			boolean backwardsValidation )
	{
		AssociateGreedy<TupleDesc_F64> alg = new AssociateGreedy<TupleDesc_F64>(score,maxError,backwardsValidation);
		WrapAssociateGreedy<TupleDesc_F64> ret = new WrapAssociateGreedy<TupleDesc_F64>(alg,maxMatches);
		return ret;
	}
}
