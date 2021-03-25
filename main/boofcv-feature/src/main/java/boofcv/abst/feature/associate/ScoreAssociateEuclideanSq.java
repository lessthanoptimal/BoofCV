/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.associate;

import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.feature.TupleDesc_F64;

/**
 * Scores based on Euclidean distance squared
 *
 * @author Peter Abeles
 * @see DescriptorDistance#euclideanSq(TupleDesc_F32, TupleDesc_F32)
 */
public interface ScoreAssociateEuclideanSq<Desc> extends ScoreAssociation<Desc> {

	// @formatter:off
	class F32 implements ScoreAssociateEuclideanSq<TupleDesc_F32> {
		@Override public double score(TupleDesc_F32 a, TupleDesc_F32 b) { return DescriptorDistance.euclideanSq(a, b); }
		@Override public MatchScoreType getScoreType()                  { return MatchScoreType.NORM_ERROR; }
		@Override public Class<TupleDesc_F32> getDescriptorType()       { return TupleDesc_F32.class; }
	}

	class F64 implements ScoreAssociateEuclideanSq<TupleDesc_F64> {
		@Override public double score(TupleDesc_F64 a, TupleDesc_F64 b) { return DescriptorDistance.euclideanSq(a, b); }
		@Override public MatchScoreType getScoreType()                  { return MatchScoreType.NORM_ERROR; }
		@Override public Class<TupleDesc_F64> getDescriptorType()       { return TupleDesc_F64.class; }
	}
	// @formatter:on
}
