/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.misc;

import java.util.List;

/**
 * Set of commonly used functions for Lambdas
 *
 * @author Peter Abeles
 */
public interface BoofLambdas {

	@FunctionalInterface
	interface MassageString {
		String process( String input );
	}

	@FunctionalInterface
	interface Factory<T> {
		T newInstance();
	}

	@FunctionalInterface
	interface Process {
		void process();
	}

	@FunctionalInterface
	interface ProcessObject<T> {
		void process(T object);
	}

	@FunctionalInterface
	interface ProcessI {
		void process( int a );
	}

	@FunctionalInterface
	interface ProcessII {
		void process( int a, int b );
	}

	@FunctionalInterface
	interface SelectElement<T> {
		int select(List<T> list);
	}
}
