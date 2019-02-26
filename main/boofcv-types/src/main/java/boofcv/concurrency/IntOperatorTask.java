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
public abstract class IntOperatorTask extends ForkJoinTask<Number> {

	final int value;
	final int max;
	final IntProducerNumber consumer;

	boolean master = true;
	Number result;
	Class primitiveType;

	IntOperatorTask next = null;

	public IntOperatorTask(int value , int max ,
						   Class primitiveType,
						   IntProducerNumber consumer ) {
		this.value = value;
		this.max = max;
		this.primitiveType = primitiveType;
		this.consumer = consumer;
	}

	@Override
	public Number getRawResult() {return result;}

	@Override
	protected void setRawResult(Number value) {
		this.result = value;
	}

	@Override
	protected boolean exec() {
		IntOperatorTask root = null;
		IntOperatorTask previous = null;

		// spawn children until there's no more to spawn
		if( master ) {
			// process all but the last value
			int value = this.value;
			while( value+1 < max ) {
				IntOperatorTask child = newInstance(value, max, primitiveType, consumer);
				child.master = false;
				if( root == null ) {
					root = child;
					previous = child;
				} else {
					previous.next = child;
					previous = child;
				}
				child.fork();
				value += 1;
			}
			// process the last value in this thread
			result = consumer.accept(value);
		} else {
			result = consumer.accept(value);
		}


		while( root != null ) {
			// perform the operation on the current result and the result from this thread
			root.join();
			operator(root.result);
			IntOperatorTask next = root.next;
			root.next = null; // free memory.
			root = next;
		}

		return true;
	}

	protected abstract IntOperatorTask newInstance( int value , int max ,
													Class primitiveType,
													IntProducerNumber consumer );

	protected abstract void operator(Number next);

	public static class Sum extends IntOperatorTask {

		public Sum(int value, int max, Class primitiveType, IntProducerNumber consumer) {
			super(value, max, primitiveType, consumer);
		}

		@Override
		protected IntOperatorTask newInstance(int value, int max, Class primitiveType, IntProducerNumber consumer) {
			return new Sum(value, max, primitiveType, consumer);
		}

		@Override
		protected void operator(Number next) {
			if( primitiveType == byte.class ) {
				result = result.byteValue()+next.byteValue();
			} else if( primitiveType == short.class ) {
				result = result.shortValue() + next.shortValue();
			} else if( primitiveType == int.class ) {
				result = result.intValue() + next.intValue();
			} else if( primitiveType == long.class ) {
				result = result.longValue() + next.longValue();
			} else if( primitiveType == float.class ) {
				result = result.floatValue() + next.floatValue();
			} else if( primitiveType == double.class ) {
				result = result.doubleValue() + next.doubleValue();
			} else {
				throw new RuntimeException("Unknown primitive type "+ primitiveType);
			}
		}
	}

	public static class Max extends IntOperatorTask {

		public Max(int value, int max, Class primitiveType, IntProducerNumber consumer) {
			super(value, max, primitiveType, consumer);
		}

		@Override
		protected IntOperatorTask newInstance(int value, int max, Class primitiveType, IntProducerNumber consumer) {
			return new Max(value, max, primitiveType, consumer);
		}

		@Override
		protected void operator(Number next) {
			if( primitiveType == byte.class ) {
				result = Math.max(result.byteValue(),next.byteValue());
			} else if( primitiveType == short.class ) {
				result = Math.max(result.shortValue(),next.shortValue());
			} else if( primitiveType == int.class ) {
				result = Math.max(result.intValue(),next.intValue());
			} else if( primitiveType == long.class ) {
				result = Math.max(result.longValue(),next.longValue());
			} else if( primitiveType == float.class ) {
				result = Math.max(result.floatValue(),next.floatValue());
			} else if( primitiveType == double.class ) {
				result = Math.max(result.doubleValue(),next.doubleValue());
			} else {
				throw new RuntimeException("Unknown primitive type "+ primitiveType);
			}
		}
	}

	public static class Min extends IntOperatorTask {

		public Min(int value, int max, Class primitiveType, IntProducerNumber consumer) {
			super(value, max, primitiveType, consumer);
		}

		@Override
		protected IntOperatorTask newInstance(int value, int max, Class primitiveType, IntProducerNumber consumer) {
			return new Min(value, max, primitiveType, consumer);
		}

		@Override
		protected void operator(Number next) {
			if( primitiveType == byte.class ) {
				result = Math.min(result.byteValue(),next.byteValue());
			} else if( primitiveType == short.class ) {
				result = Math.min(result.shortValue(),next.shortValue());
			} else if( primitiveType == int.class ) {
				result = Math.min(result.intValue(),next.intValue());
			} else if( primitiveType == long.class ) {
				result = Math.min(result.longValue(),next.longValue());
			} else if( primitiveType == float.class ) {
				result = Math.min(result.floatValue(),next.floatValue());
			} else if( primitiveType == double.class ) {
				result = Math.min(result.doubleValue(),next.doubleValue());
			} else {
				throw new RuntimeException("Unknown primitive type "+ primitiveType);
			}
		}
	}
}
