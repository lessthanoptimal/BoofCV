/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.associate.AssociateGreedy;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.MatchScoreType;

/**
 * @author Peter Abeles
 */
public class TestWrapAssociateGreedy extends StandardAssociateDescriptionChecks<Double> {

	@Override
	public AssociateDescription<Double> createAlg() {
		AssociateGreedy<Double> greedy = new AssociateGreedy<Double>(new DoubleScore(),-1,false);
		return new WrapAssociateGreedy<Double>(greedy,-1);
	}

	@Override
	public void addFeature(FastQueue<Double> listSrc, FastQueue<Double> listDst, double error) {

		int i = listSrc.size;

		listSrc.add((double)i);
		listDst.add(i+error);
	}

	@Override
	public Class<Double> getDescType() {
		return Double.class;
	}

	private class DoubleScore implements ScoreAssociation<Double> {

		@Override
		public double score(Double a, Double b) {
			return Math.abs(a-b);
		}

		@Override
		public MatchScoreType getScoreType() {
			return MatchScoreType.NORM_ERROR;
		}
	}
}
