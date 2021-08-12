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

import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;

/**
 * <p>Matches features using a {@link NearestNeighbor} search from DDogleg. The source features are processed
 * as a lump using {@link NearestNeighbor#setPoints(java.util.List, boolean)} while destination features
 * are matched one at time using {@link NearestNeighbor.Search#findNearest(Object, double, org.ddogleg.nn.NnData)}.
 * Typically the processing of source features is more expensive and should be minimized while looking up
 * destination features is fast. Multiple matches for source features are possible while there will only
 * be a unique match for each destination feature.</p>
 *
 * <p>An optional ratio test inspired from [1] can be used. The ratio between the best and second best score is found.
 * if the difference is significant enough then the match is accepted. This this is a ratio test, knowing if the score
 * is squared is important. Please set the flag correctly. Almost always the score is Euclidean distance squared.</p>
 *
 * <p>[1] Lowe, David G. "Distinctive image features from scale-invariant keypoints."
 * International journal of computer vision 60.2 (2004): 91-110.</p>
 *
 * @author Peter Abeles
 */
public class AssociateNearestNeighbor_ST<D>
		extends AssociateNearestNeighbor<D> {
	// Nearest Neighbor algorithm and storage for the results
	private final NearestNeighbor.Search<D> search;
	NnData<D> result = new NnData<>();
	DogArray<NnData<D>> result2 = new DogArray<>(NnData<D>::new);

	// The type of description it can process
	Class<D> descType;

	public AssociateNearestNeighbor_ST( NearestNeighbor<D> alg, Class<D> descType ) {
		super(alg);
		this.search = alg.createSearch();
		this.descType = descType;
	}

	@Override
	public void setSource( FastAccess<D> listSrc ) {
		super.setSource(listSrc);
	}

	@Override
	public void setDestination( FastAccess<D> listDst ) {
		this.listDst = listDst;
	}

	@Override
	public void associate() {

		matchesAll.resize(listDst.size);
		matchesAll.reset();
		if (scoreRatioThreshold >= 1.0) {
			// if score ratio is not turned on then just use the best match
			for (int i = 0; i < listDst.size; i++) {
				if (!search.findNearest(listDst.data[i], maxDistance, result))
					continue;
				matchesAll.grow().setTo(result.index, i, result.distance);
			}
		} else {
			for (int i = 0; i < listDst.size; i++) {
				search.findNearest(listDst.data[i], maxDistance, 2, result2);

				if (result2.size == 1) {
					NnData<D> r = result2.getTail();
					matchesAll.grow().setTo(r.index, i, r.distance);
				} else if (result2.size == 2) {
					NnData<D> r0 = result2.get(0);
					NnData<D> r1 = result2.get(1);

					// ensure that r0 is the closest
					if (r0.distance > r1.distance) {
						NnData<D> tmp = r0;
						r0 = r1;
						r1 = tmp;
					}

					double foundRatio = ratioUsesSqrt ? Math.sqrt(r0.distance)/Math.sqrt(r1.distance) : r0.distance/r1.distance;
					if (foundRatio <= scoreRatioThreshold) {
						matchesAll.grow().setTo(r0.index, i, r0.distance);
					}
				} else if (result2.size != 0) {
					throw new RuntimeException("BUG! 0,1,2 are acceptable not " + result2.size);
				}
			}
		}
	}

	@Override public Class<D> getDescriptionType() {
		return descType;
	}
}
