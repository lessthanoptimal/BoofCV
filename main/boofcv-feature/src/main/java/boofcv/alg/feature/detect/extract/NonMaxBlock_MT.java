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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Concurrent implementation of {@link NonMaxBlock_MT}. Every row of block is run in its own threads.
 * All threads keep track of all the found mins/maxs in seperate lists which are then combined after
 * a thread has finished running. All searches and point lists declared for each thread
 * are saved for future use</p>
 *
 * @author Peter Abeles
 */
public class NonMaxBlock_MT extends NonMaxBlock {

	// lock for variables below - which are lists used to store work space for individual threads
	final Object lock = new Object();
	final List<Search> searches = new ArrayList<>();
	final List<QueueCorner> cornerLists = new ArrayList<>();

	public NonMaxBlock_MT(Search search) {
		super(search);
	}

	/**
	 * Detects local minimums and/or maximums in the provided intensity image.
	 *
	 * @param intensityImage (Input) Feature intensity image.
	 * @param localMin (Output) storage for found local minimums.
	 * @param localMax (Output) storage for found local maximums.
	 */
	@Override
	public void process(GrayF32 intensityImage, @Nullable QueueCorner localMin, @Nullable QueueCorner localMax) {

		if( localMin != null )
			localMin.reset();
		if( localMax != null )
			localMax.reset();

		// the defines the region that can be processed
		int endX = intensityImage.width - border;
		int endY = intensityImage.height - border;

		int step = configuration.radius+1;

		search.initialize(configuration,intensityImage,localMin,localMax);

		// Compute number of y iterations
		int N = (endY-border)/step;
		if( (endY-border) - N*step > 0 )
			N += 1;

		BoofConcurrency.loopFor(0,N, iterY -> {

			NonMaxBlock.Search search;
			QueueCorner threadMin=null,threadMax=null;

			// get work space for this thread
			synchronized (lock) {
				if( searches.isEmpty() ) {
					search = this.search.newInstance();
				} else {
					search = searches.remove( searches.size()-1 );
				}
				if( search.isDetectMinimums() ) {
					threadMin = pop();
				}
				if( search.isDetectMaximums() ) {
					threadMax = pop();
				}
			}
			search.initialize(configuration,intensityImage,threadMin,threadMax);

			// search for local peaks along this block row
			int y = border + iterY*step;
			int y1 = y + step;
			if( y1 > endY) y1 = endY;

			for(int x = border; x < endX; x += step ) {
				int x1 = x + step;
				if( x1 > endX) x1 = endX;
				search.searchBlock(x,y,x1,y1);
			}

			// Save the results and recycle thread working space
			synchronized (lock) {
				saveResults(localMin, threadMin);
				saveResults(localMax, threadMax);

				searches.add(search);
				if( threadMin != null )
					cornerLists.add(threadMin);
				if( threadMax != null )
					cornerLists.add(threadMax);
			}
		});
	}

	private QueueCorner pop() {
		QueueCorner queue;
		if (cornerLists.isEmpty()) {
			queue = new QueueCorner();
		} else {
			queue = cornerLists.remove(cornerLists.size() - 1);
		}
		queue.reset();
		return queue;
	}

	private void saveResults(QueueCorner output, QueueCorner thread) {
		if( thread != null ) {
			for (int i = 0; i < thread.size; i++) {
				output.grow().set(thread.get(i));
			}
		}
	}
}
