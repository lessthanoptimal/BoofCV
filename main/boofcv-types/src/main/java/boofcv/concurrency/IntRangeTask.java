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

	public IntRangeTask(int step, int min , int max , int stepLength , IntRangeConsumer consumer ) {
		this.step = step;
		this.min = min;
		this.max = max;
		this.stepLength = stepLength;
		this.consumer = consumer;
	}

	@Override
	public Void getRawResult() {return null;}

	@Override
	protected void setRawResult(Void value) {}

	@Override
	protected boolean exec() {
		int N = (max-min)/stepLength;
		int index0 = step*stepLength + min;
		int index1 = step==(N-1) ? max : index0 + stepLength;

		IntRangeTask nextTask = null;
		if( index1 != max ) {
			nextTask = new IntRangeTask(step+1,min,max,stepLength, consumer);
			nextTask.fork();
		}
//		System.out.println("Working "+index0+" "+index1);
		consumer.accept(index0,index1);
//		System.out.println("Done    "+index0+" "+index1);
		if( nextTask != null )
			nextTask.join();
		return true;
	}
}
