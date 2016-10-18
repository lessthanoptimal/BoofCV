/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv;

import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Benchmark to see how much faster it is to work with raw arrays than using the list interface
 *
 * @author Peter Abeles
 */
public class ArrayVsListAccess {

	static long TEST_TIME = 1000;

	private static class ListAccess extends PerformerBase
	{
		List<Double> data = new ArrayList<>();
		int ret;

		public ListAccess( double data [] ) {
			for( double d : data ) {
				this.data.add(d);
			}
		}
		
		@Override
		public void process() {
			ret = 0;

			final int N = data.size();
			for( int i = 0; i < N; i++ ) {
				double a = data.get(i);
				for( int j = 0; j < N; j++ ) {
					if( data.get(j) < a ) {
						ret++;
					}
				}
			}
		}
	}

	private static class ArrayAccess extends PerformerBase
	{
		Double data[];
		int ret;

		public ArrayAccess( double data [] ) {
			this.data = new Double[ data.length ];
			int i = 0;
			for( Double d : data ) {
				this.data[i++] = d;
			}
		}

		@Override
		public void process() {
			ret = 0;

			final int N = data.length;
			for( int i = 0; i < N; i++ ) {
				double a = data[i];
				for( int j = 0; j < N; j++ ) {
					if( data[j] < a ) {
						ret++;
					}
				}
			}
		}
	}

	private static class FastQueueAccess extends PerformerBase
	{
		FastQueue<Double> queue = new FastQueue<>(100, Double.class, false);
		int ret;

		public FastQueueAccess( double data [] ) {
			for( Double d : data ) {
				queue.add(d);
			}
		}

		@Override
		public void process() {
			ret = 0;

			final int N = queue.size;
			for( int i = 0; i < N; i++ ) {
				double a = queue.get(i);
				for( int j = 0; j < N; j++ ) {
					if( queue.get(j) < a ) {
						ret++;
					}
				}
			}
		}
	}

	private static class FastQueueAccessRaw extends PerformerBase
	{
		FastQueue<Double> queue = new FastQueue<>(100, Double.class, false);
		int ret;

		public FastQueueAccessRaw( double data [] ) {
			for( Double d : data ) {
				queue.add(d);
			}
		}

		@Override
		public void process() {
			ret = 0;

			final int N = queue.size;
			for( int i = 0; i < N; i++ ) {
				double a = queue.data[i];
				for( int j = 0; j < N; j++ ) {
					if( queue.data[j] < a ) {
						ret++;
					}
				}
			}
		}
	}

	public static void main( String args[] ) {
		Random rand = new Random(2342);
		double data[] = new double[5000];
		for( int i = 0; i < data.length; i++ ) {
			data[i] = rand.nextDouble()*100;
		}

		ListAccess list = new ListAccess(data);
		ArrayAccess array = new ArrayAccess(data);
		FastQueueAccess fastQueue = new FastQueueAccess(data);
		FastQueueAccessRaw fastRaw = new FastQueueAccessRaw(data);

		ProfileOperation.printOpsPerSec(fastRaw, TEST_TIME);
		ProfileOperation.printOpsPerSec(fastQueue, TEST_TIME);
		ProfileOperation.printOpsPerSec(list, TEST_TIME);
		ProfileOperation.printOpsPerSec(array, TEST_TIME);

		System.out.println("list.ret = "+list.ret);
		System.out.println("array.ret = "+array.ret);
		System.out.println("fast.ret = "+fastQueue.ret);
		System.out.println("raw.ret = "+fastRaw.ret);
	}
}
