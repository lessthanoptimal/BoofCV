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

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.feature.AssociatedIndex;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastAccess;
import pabeles.concurrency.IntRangeConsumer;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Parallel associate version of {@link AssociateNearestNeighbor_ST}.</p>
 *
 * @author Peter Abeles
 */
public class AssociateNearestNeighbor_MT<D>
		extends AssociateNearestNeighbor<D> {
	// Nearest Neighbor algorithm and storage for the results
	private final List<Helper> available = new ArrayList<>();

	private final Class<D> descType;

	public AssociateNearestNeighbor_MT( NearestNeighbor<D> alg, Class<D> descType ) {
		super(alg);
		this.descType = descType;
	}

	@Override
	public void setSource( FastAccess<D> listSrc ) {
		this.sizeSrc = listSrc.size;
		alg.setPoints(listSrc.toList(), true);
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
			BoofConcurrency.loopBlocks(0, listDst.size, new InnerConsumer() {
				@Override
				public void innerAccept( Helper h, int index0, int index1 ) {
					for (int i = index0; i < index1; i++) {
						if (!h.search.findNearest(listDst.data[i], maxDistance, h.result))
							continue;
						h.matches.grow().setTo(h.result.index, i, h.result.distance);
					}
				}
			});
		} else {
			BoofConcurrency.loopBlocks(0, listDst.size, new InnerConsumer() {
				@Override
				public void innerAccept( Helper h, int index0, int index1 ) {
					for (int i = index0; i < index1; i++) {
						h.search.findNearest(listDst.data[i], maxDistance, 2, h.result2);

						if (h.result2.size == 1) {
							NnData<D> r = h.result2.getTail();
							h.matches.grow().setTo(r.index, i, r.distance);
						} else if (h.result2.size == 2) {
							NnData<D> r0 = h.result2.get(0);
							NnData<D> r1 = h.result2.get(1);

							// ensure that r0 is the closest
							if (r0.distance > r1.distance) {
								NnData<D> tmp = r0;
								r0 = r1;
								r1 = tmp;
							}

							double foundRatio = ratioUsesSqrt ? Math.sqrt(r0.distance)/Math.sqrt(r1.distance) : r0.distance/r1.distance;
							if (foundRatio <= scoreRatioThreshold) {
								h.matches.grow().setTo(r0.index, i, r0.distance);
							}
						} else if (h.result2.size != 0) {
							throw new RuntimeException("BUG! 0,1,2 are acceptable not " + h.result2.size);
						}
					}
				}
			});
		}
	}

	@Override public Class<D> getDescriptionType() {
		return descType;
	}

	/**
	 * Consumes a block of matches
	 */
	private abstract class InnerConsumer implements IntRangeConsumer {

		@Override
		public void accept( int index0, int index1 ) {
			// Recycle a helper if possible
			Helper h;
			synchronized (available) {
				if (available.isEmpty()) {
					h = new Helper();
				} else {
					h = available.remove(available.size() - 1);
					h.initialize();
				}
			}

			// do the for loop inside of this and pass in the helper for this block
			innerAccept(h, index0, index1);

			// synchronize the data again
			synchronized (matchesAll) {
				for (int i = 0; i < h.matches.size; i++) {
					AssociatedIndex a = h.matches.get(i);
					matchesAll.grow().setTo(a);
				}
			}

			// Put the helper back into the available list
			synchronized (available) {
				available.add(h);
			}
		}

		public abstract void innerAccept( Helper h, int index0, int index1 );
	}

	/**
	 * Contains data structures for a specific thread.
	 */
	private class Helper {
		NearestNeighbor.Search<D> search;
		DogArray<AssociatedIndex> matches = new DogArray<>(10, AssociatedIndex::new);
		private final NnData<D> result = new NnData<>();
		private final DogArray<NnData<D>> result2 = new DogArray(NnData::new);

		Helper() {
			search = alg.createSearch();
		}

		public void initialize() {
			matches.reset();
			result2.reset();
		}
	}
}
