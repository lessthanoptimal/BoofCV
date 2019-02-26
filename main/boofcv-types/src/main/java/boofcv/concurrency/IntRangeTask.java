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

package boofcv.concurrency;

import java.util.concurrent.ForkJoinTask;

/**
 * @author Peter Abeles
 */
public class IntRangeTask extends ForkJoinTask<Void> {

	final int min;
	final int max;
	final int stepLength;
	final int step;
	final IntRangeConsumer consumer;
	IntRangeTask next;

	/**
	 *
	 * @param step which step is to be processed. the master task should have this set to -1
	 * @param min
	 * @param max
	 * @param stepLength
	 * @param consumer
	 */
	public IntRangeTask(int step, int min , int max , int stepLength , IntRangeConsumer consumer ) {
		this.step = step;
		this.min = min;
		this.max = max;
		this.stepLength = stepLength;
		this.consumer = consumer;
	}

	public IntRangeTask( int min , int max , int stepLength , IntRangeConsumer consumer ) {
		this(-1,min,max,stepLength,consumer);
	}

	@Override
	public Void getRawResult() {return null;}

	@Override
	protected void setRawResult(Void value) {}

	@Override
	protected boolean exec() {
		int N = (max-min)/stepLength;

		if( step == -1 ) {
			// this is the first task, spawn all the others
			IntRangeTask root=null;
			IntRangeTask previous=null;
			int step;
			for ( step = 0; step < N - 1; step++) {
				IntRangeTask task = new IntRangeTask(step,min,max,stepLength, consumer);
				if( root == null ) {
					root = previous = task;
				} else {
					previous.next = task;
					previous = task;
				}
				task.fork();
			}
			// process the last segment in this thread
			int index0 = step*stepLength + min;
			consumer.accept(index0,max);

			// wait until all the other threads are done
			while( root != null ) {
				root.join();
				root = root.next;
			}
		} else {
			int index0 = step*stepLength + min;
			int index1 = index0 + stepLength;
			consumer.accept(index0,index1);
		}
		return true;
	}
}
