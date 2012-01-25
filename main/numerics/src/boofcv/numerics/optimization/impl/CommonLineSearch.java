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

package boofcv.numerics.optimization.impl;

import boofcv.numerics.optimization.LineSearch;
import boofcv.numerics.optimization.functions.FunctionStoS;
import org.ejml.UtilEjml;

/**
 * @author Peter Abeles
 */
public abstract class CommonLineSearch implements LineSearch {

	// step tolerance change
	protected double tolStep = UtilEjml.EPS;

	// function being minimized
	protected FunctionStoS function;
	// derivative of function being minimized
	protected FunctionStoS derivative;

	// function value at alpha = 0
	protected double valueZero;
	// function derivative at alpha = 0
	protected double derivZero;

	// current step length, function value, and derivative
	protected double alphaT;
	protected double valueT;
	protected double derivT;



}
