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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.ScoreAssociation;
import org.ddogleg.struct.FastAccess;

/**
 * Greedy association for matching descriptors
 *
 * @author Peter Abeles
 */
public abstract class AssociateGreedyDescBase<D> extends AssociateGreedyBase<D> {
	AssociateGreedyDescBase( ScoreAssociation<D> score ) {
		super(score);
	}

	/**
	 * Associates the two sets objects against each other by minimizing fit score.
	 *
	 * @param src Source list.
	 * @param dst Destination list.
	 */
	public abstract void associate( FastAccess<D> src, FastAccess<D> dst );
}
