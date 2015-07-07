/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.feature.NccFeature;

/**
 * Association scorer for NccFeatures.  Computes the normalized cross correlation score.
 *
 * NOTE: The score's sign is flipped in order to comply with {@link ScoreAssociation}'s requirements that lower
 * values be preferred.
 *
 * @see DescriptorDistance#ncc
 *
 * @author Peter Abeles
 */
public class ScoreAssociateNccFeature implements ScoreAssociation<NccFeature> {
	@Override
	public double score(NccFeature a, NccFeature b) {
		return -DescriptorDistance.ncc(a, b);
	}

	@Override
	public MatchScoreType getScoreType() {
		return MatchScoreType.CORRELATION;
	}
}
