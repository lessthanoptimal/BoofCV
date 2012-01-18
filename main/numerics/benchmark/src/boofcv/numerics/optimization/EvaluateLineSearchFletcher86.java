/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.optimization;

/**
 * @author Peter Abeles
 */
public class EvaluateLineSearchFletcher86 extends LineSearchEvaluator {

	double c1,c2;

	@Override
	protected LineSearch createSearch() {
		return new LineSearchFletcher86(c1,c2,9,0.1,0.5,100);
	}


	@Override
	public void fletcher1() {
		c1=1e-3;
		c2=0.1;
		super.fletcher1();
	}

	@Override
	public void more1() {
		c1=1e-4;
		c2=0.1;
		super.more1();
		System.out.println("==========");
		c1=0.0999;
		c2=0.1;
		super.more1();
	}

	@Override
	public void more2() {
		c1=1e-4;
		c2=0.1;
		super.more2();
	}

	public static void main( String []args ) {
		EvaluateLineSearchFletcher86 eval = new EvaluateLineSearchFletcher86();
		eval.fletcher1();
//		System.out.println("-----------------");
//		eval.more1();
//		System.out.println("-----------------");
//		eval.more2();
	}
}
