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
import boofcv.struct.feature.*;

/**
 * Scores association using Sum-of-Absolute-Difference error metric.
 *
 * @author Peter Abeles
 */
public interface ScoreAssociateSad<Desc> extends ScoreAssociation<Desc> {
	class F32 implements ScoreAssociateSad<TupleDesc_F32> {
		@Override public double score( TupleDesc_F32 a, TupleDesc_F32 b ) {return DescriptorDistance.sad(a, b);}

		@Override public MatchScoreType getScoreType() {return MatchScoreType.NORM_ERROR;}

		@Override public Class<TupleDesc_F32> getDescriptorType() {return TupleDesc_F32.class;}
	}

	class F64 implements ScoreAssociateSad<TupleDesc_F64> {
		@Override public double score( TupleDesc_F64 a, TupleDesc_F64 b ) {return DescriptorDistance.sad(a, b);}

		@Override public MatchScoreType getScoreType() {return MatchScoreType.NORM_ERROR;}

		@Override public Class<TupleDesc_F64> getDescriptorType() {return TupleDesc_F64.class;}
	}

	class U8 implements ScoreAssociateSad<TupleDesc_U8> {
		@Override public double score( TupleDesc_U8 a, TupleDesc_U8 b ) {return DescriptorDistance.sad(a, b);}

		@Override public MatchScoreType getScoreType() {return MatchScoreType.NORM_ERROR;}

		@Override public Class<TupleDesc_U8> getDescriptorType() {return TupleDesc_U8.class;}
	}

	class S8 implements ScoreAssociateSad<TupleDesc_S8> {
		@Override public double score( TupleDesc_S8 a, TupleDesc_S8 b ) {return DescriptorDistance.sad(a, b);}

		@Override public MatchScoreType getScoreType() {return MatchScoreType.NORM_ERROR;}

		@Override public Class<TupleDesc_S8> getDescriptorType() {return TupleDesc_S8.class;}
	}
}
