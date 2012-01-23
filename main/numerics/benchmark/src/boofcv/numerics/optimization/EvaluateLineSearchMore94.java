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

import boofcv.numerics.optimization.impl.LineSearchMore94;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class EvaluateLineSearchMore94 extends LineSearchEvaluator {

	double ftol, gtol,xtol=1e-10;

	public EvaluateLineSearchMore94(boolean verbose) {
		super(verbose);
	}

	@Override
	protected LineSearch createSearch() {
		return new LineSearchMore94(ftol, gtol,xtol,0);
	}

	@Override
	public List<Results> fletcher1() {
		ftol = 1e-3;
		gtol = 0.1;
		return super.fletcher1();
	}

	@Override
	public List<Results> more1() {
		ftol = 1e-3;
		gtol = 0.1;
		return super.more1();
	}

	@Override
	public List<Results> more2() {
		ftol = 0.1;
		gtol = 0.1;
		return super.more2();
	}

	@Override
	public List<Results> more3() {
		ftol = 0.1;
		gtol = 0.1;
		return super.more3();
	}

	@Override
	public List<Results> more4() {
		ftol = 0.001;
		gtol = 0.001;
		return super.more4();
	}

	@Override
	public List<Results> more5() {
		ftol = 0.001;
		gtol = 0.001;
		return super.more5();
	}

	@Override
	public List<Results> more6() {
		ftol = 0.001;
		gtol = 0.001;
		return super.more6();
	}

	public static void main( String []args ) {
		EvaluateLineSearchMore94 eval = new EvaluateLineSearchMore94(true);
		System.out.println("----------------- fletcher 1");
		eval.fletcher1();
		System.out.println("----------------- more 1");
		eval.more1();
		System.out.println("----------------- more 2");
		eval.more2();
		System.out.println("----------------- more 3");
		eval.more3();
		System.out.println("----------------- more 4");
		eval.more4();
		System.out.println("----------------- more 5");
		eval.more5();
		System.out.println("----------------- more 6");
		eval.more6();
	}
}
