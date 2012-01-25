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

package boofcv.numerics.optimization.functions;

/**
 * Contains functions for optimization algorithms that perform a line search and require the
 * function's value and its gradient.  A single interface is provided for those functions
 * to encourage data reuse.  Any time setInput() is called for either the function or line search
 * all functions will use the same input and the old outputs are discarded.
 *
 * @author Peter Abeles
 */
public interface GradientLineFunction
		extends CoupledGradient , LineSearchFunction
{
}
