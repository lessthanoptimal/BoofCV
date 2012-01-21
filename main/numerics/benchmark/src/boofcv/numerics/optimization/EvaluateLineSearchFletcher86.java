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

import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvaluateLineSearchFletcher86 extends LineSearchEvaluator {

	double c1,c2;

	public EvaluateLineSearchFletcher86(boolean verbose) {
		super(verbose);
	}

	@Override
	protected LineSearch createSearch( double alpha0 ) {
		return new LineSearchFletcher86(c1,c2,0,9,0.1,0.5,100);
	}


	@Override
	public List<Results> fletcher1() {
		c1=1e-3;
		c2=0.1;
		return super.fletcher1();
	}

	@Override
	public List<Results> more1() {
		c1=1e-4;
		c2=0.1;
		return super.more1();
	}

	@Override
	public List<Results> more2() {
		c1=1e-4;
		c2=0.1;
		return super.more2();
	}

	@Override
	public List<Results> more3() {
		c1=0.1;
		c2=0.1;
		return super.more3();
	}

	@Override
	public List<Results> more4() {
		c1=0.001;
		c2=0.001;
		return super.more4();
	}

	@Override
	public List<Results> more5() {
		c1=0.001;
		c2=0.001;
		return super.more5();
	}

	@Override
	public List<Results> more6() {
		c1=0.001;
		c2=0.001;
		return super.more6();
	}

	public static void main( String []args ) {
		EvaluateLineSearchFletcher86 eval = new EvaluateLineSearchFletcher86(true);
		eval.fletcher1();
		System.out.println("----------------- More 1");
		eval.more1();
		System.out.println("----------------- More 2");
		eval.more2();
		System.out.println("----------------- More 3");
		eval.more3();
		System.out.println("----------------- More 4");
		eval.more4();
		System.out.println("----------------- More 5");
		eval.more5();
		System.out.println("----------------- More 6");
		eval.more6();
	}
}
