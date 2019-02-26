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

	Number result;
	Class primitiveType;

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
		IntOperatorTask nextTask = null;
		if( value < max-1 ) {
			nextTask = newInstance(value+1,max, primitiveType,consumer);
			nextTask.fork();
		}
		result = consumer.accept(value);
		if( nextTask != null ) {
			nextTask.join();

			operator(nextTask);
		}

		return true;
	}

	protected abstract IntOperatorTask newInstance( int value , int max ,
													Class primitiveType,
													IntProducerNumber consumer );

	protected abstract void operator(IntOperatorTask nextTask);

	public static class Sum extends IntOperatorTask {

		public Sum(int value, int max, Class primitiveType, IntProducerNumber consumer) {
			super(value, max, primitiveType, consumer);
		}

		@Override
		protected IntOperatorTask newInstance(int value, int max, Class primitiveType, IntProducerNumber consumer) {
			return new Sum(value, max, primitiveType, consumer);
		}

		@Override
		protected void operator(IntOperatorTask nextTask) {
			if( primitiveType == byte.class ) {
				result = result.byteValue()+nextTask.getRawResult().byteValue();
			} else if( primitiveType == short.class ) {
				result = result.shortValue() + nextTask.getRawResult().shortValue();
			} else if( primitiveType == int.class ) {
				result = result.intValue() + nextTask.getRawResult().intValue();
			} else if( primitiveType == long.class ) {
				result = result.longValue() + nextTask.getRawResult().longValue();
			} else if( primitiveType == float.class ) {
				result = result.floatValue() + nextTask.getRawResult().floatValue();
			} else if( primitiveType == double.class ) {
				result = result.doubleValue() + nextTask.getRawResult().doubleValue();
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
		protected void operator(IntOperatorTask nextTask) {
			if( primitiveType == byte.class ) {
				result = Math.max(result.byteValue(),nextTask.getRawResult().byteValue());
			} else if( primitiveType == short.class ) {
				result = Math.max(result.shortValue(),nextTask.getRawResult().shortValue());
			} else if( primitiveType == int.class ) {
				result = Math.max(result.intValue(),nextTask.getRawResult().intValue());
			} else if( primitiveType == long.class ) {
				result = Math.max(result.longValue(),nextTask.getRawResult().longValue());
			} else if( primitiveType == float.class ) {
				result = Math.max(result.floatValue(),nextTask.getRawResult().floatValue());
			} else if( primitiveType == double.class ) {
				result = Math.max(result.doubleValue(),nextTask.getRawResult().doubleValue());
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
		protected void operator(IntOperatorTask nextTask) {
			if( primitiveType == byte.class ) {
				result = Math.min(result.byteValue(),nextTask.getRawResult().byteValue());
			} else if( primitiveType == short.class ) {
				result = Math.min(result.shortValue(),nextTask.getRawResult().shortValue());
			} else if( primitiveType == int.class ) {
				result = Math.min(result.intValue(),nextTask.getRawResult().intValue());
			} else if( primitiveType == long.class ) {
				result = Math.min(result.longValue(),nextTask.getRawResult().longValue());
			} else if( primitiveType == float.class ) {
				result = Math.min(result.floatValue(),nextTask.getRawResult().floatValue());
			} else if( primitiveType == double.class ) {
				result = Math.min(result.doubleValue(),nextTask.getRawResult().doubleValue());
			} else {
				throw new RuntimeException("Unknown primitive type "+ primitiveType);
			}
		}
	}
}
