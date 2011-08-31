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

import gecv.alg.feature.associate.ScoreAssociateTuple;
import gecv.struct.feature.TupleDesc_F64;


/**
 * Creates algorithms for associating {@link gecv.struct.feature.TupleDesc_F64} features.
 *
 * @author Peter Abeles
 */
public class FactoryAssociationTuple {

	public static GeneralAssociation<TupleDesc_F64> maxError( ScoreAssociateTuple score , double maxError )
	{
		WrapAssociateGreedyTuple ret = new WrapAssociateGreedyTuple.Basic(maxError);
		ret.setScore(score);
		return ret;
	}

	public static GeneralAssociation<TupleDesc_F64> maxMatches( ScoreAssociateTuple score , int maxMatches )
	{
		WrapAssociateGreedyTuple ret =  new WrapAssociateGreedyTuple.FitIsError(maxMatches);
		ret.setScore(score);
		return ret;
	}

	public static GeneralAssociation<TupleDesc_F64> inlierError( ScoreAssociateTuple score ,
																 int maxMatches , double containmentScale )
	{
		WrapAssociateGreedyTuple ret =  new WrapAssociateGreedyTuple.TotalCloseMatches(maxMatches,containmentScale);
		ret.setScore(score);
		return ret;
	}

	public static GeneralAssociation<TupleDesc_F64> forwardBackwards( ScoreAssociateTuple score ,
																	  int maxMatches )
	{
		WrapAssociateGreedyTuple ret =  new WrapAssociateGreedyTuple.ForwardBackwards(maxMatches);
		ret.setScore(score);
		return ret;
	}
}
