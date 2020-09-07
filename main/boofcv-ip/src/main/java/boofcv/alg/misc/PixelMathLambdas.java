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

package boofcv.alg.misc;

/**
 * Functions for lambdas that can be applied to images on an element-wise basis
 *
 * @author Peter Abeles
 */
public abstract class PixelMathLambdas {
	private PixelMathLambdas() {}

	public interface Function1 {}

	public interface Function2 {}

	@FunctionalInterface
	public interface Function2_I8 extends Function2 {
		byte process( byte a, byte b );
	}

	@FunctionalInterface
	public interface Function2_I16 extends Function2 {
		short process( short a, short b );
	}

	@FunctionalInterface
	public interface Function2_S32 extends Function2 {
		int process( int a, int b );
	}

	@FunctionalInterface
	public interface Function2_S64 extends Function2 {
		long process( long a, long b );
	}

	@FunctionalInterface
	public interface Function2_F32 extends Function2 {
		float process( float a, float b );
	}

	@FunctionalInterface
	public interface Function2_F64 extends Function2 {
		double process( double a, double b );
	}

	@FunctionalInterface
	public interface Function1_I8 extends Function1 {
		byte process( byte a );
	}

	@FunctionalInterface
	public interface Function1_I16 extends Function1 {
		short process( short a );
	}

	@FunctionalInterface
	public interface Function1_S32 extends Function1 {
		int process( int a );
	}

	@FunctionalInterface
	public interface Function1_S64 extends Function1 {
		long process( long a );
	}

	@FunctionalInterface
	public interface Function1_F32 extends Function1 {
		float process( float a );
	}

	@FunctionalInterface
	public interface Function1_F64 extends Function1 {
		double process( double a );
	}
}
