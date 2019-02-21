/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.extract;

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_I16;

import java.util.ArrayList;
import java.util.List;

/**
 * Concurrent implementation of {@link NonMaxCandidate}.
 *
 *
 * @author Peter Abeles
 */
public class NonMaxCandidate_MT extends NonMaxCandidate {

	final Object lock = new Object();
	final List<NonMaxCandidate.Search> searches = new ArrayList<>();
	final List<QueueCorner> cornerLists = new ArrayList<>();

	public NonMaxCandidate_MT(Search search) {
		super(search);
	}

	@Override
	protected void examineMinimum(GrayF32 intensityImage , QueueCorner candidates , QueueCorner found ) {
		final int stride = intensityImage.stride;
		final float inten[] = intensityImage.data;

		// little cost to creating a thread so let it select the minimum block size
		BoofConcurrency.blocks(0,candidates.size,(idx0,idx1)->{
			NonMaxCandidate.Search search;
			QueueCorner threadCorners;
			synchronized ( lock ) {
				search = searches.isEmpty() ? this.search.newInstance() : searches.remove(searches.size()-1);
				threadCorners = cornerLists.isEmpty() ? new QueueCorner() : cornerLists.remove(cornerLists.size()-1);
				threadCorners.reset();
			}
			search.initialize(intensityImage);

			for (int iter = idx0; iter < idx1; iter++) {
				Point2D_I16 pt = candidates.data[iter];

				if( pt.x < ignoreBorder || pt.y < ignoreBorder || pt.x >= endBorderX || pt.y >= endBorderY)
					continue;

				int center = intensityImage.startIndex + pt.y * stride + pt.x;

				float val = inten[center];
				if (val > thresholdMin || val == -Float.MAX_VALUE ) continue;

				int x0 = Math.max(0,pt.x - radius);
				int y0 = Math.max(0,pt.y - radius);
				int x1 = Math.min(intensityImage.width, pt.x + radius + 1);
				int y1 = Math.min(intensityImage.height, pt.y + radius + 1);

				if( search.searchMin(x0,y0,x1,y1,center,val) )
					threadCorners.add(pt.x,pt.y);
			}

			synchronized (lock) {
				found.addAll(threadCorners);
				searches.add(search);
				cornerLists.add(threadCorners);
			}
		});
	}

	@Override
	protected void examineMaximum(GrayF32 intensityImage , QueueCorner candidates , QueueCorner found ) {
		final int stride = intensityImage.stride;
		final float inten[] = intensityImage.data;

		// little cost to creating a thread so let it select the minimum block size
		BoofConcurrency.blocks(0,candidates.size,(idx0,idx1)-> {
			NonMaxCandidate.Search search;
			QueueCorner threadCorners;
			synchronized ( lock ) {
				search = searches.isEmpty() ? this.search.newInstance() : searches.remove(searches.size()-1);
				threadCorners = cornerLists.isEmpty() ? new QueueCorner() : cornerLists.remove(cornerLists.size()-1);
				threadCorners.reset();
			}
			search.initialize(intensityImage);

			for (int iter = idx0; iter < idx1; iter++) {
				Point2D_I16 pt = candidates.data[iter];

				if (pt.x < ignoreBorder || pt.y < ignoreBorder || pt.x >= endBorderX || pt.y >= endBorderY)
					continue;

				int center = intensityImage.startIndex + pt.y * stride + pt.x;

				float val = inten[center];
				if (val < thresholdMax || val == Float.MAX_VALUE) continue;

				int x0 = Math.max(0, pt.x - radius);
				int y0 = Math.max(0, pt.y - radius);
				int x1 = Math.min(intensityImage.width, pt.x + radius + 1);
				int y1 = Math.min(intensityImage.height, pt.y + radius + 1);

				if (search.searchMax(x0, y0, x1, y1, center, val))
					threadCorners.add(pt.x, pt.y);
			}

			synchronized (lock) {
				found.addAll(threadCorners);
				searches.add(search);
				cornerLists.add(threadCorners);
			}
		});
	}
}
