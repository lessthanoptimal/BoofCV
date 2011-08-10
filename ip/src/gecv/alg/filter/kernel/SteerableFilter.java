/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.kernel;

import gecv.struct.convolve.Kernel1D;
import gecv.struct.convolve.Kernel2D;
import gecv.struct.image.ImageBase;


/**
 * <p>
 * Computes the output of steerable filters.  TODO describe what steerable filters are
 * </p>
 *
 * <p>
 * W. Freeman and E. Adelson. "The design and use of steerable filters," PAMI, 13(9):891-906, 1991
 * </p>
 *
 * @author Peter Abeles
 */
public class SteerableFilter< I extends ImageBase, D extends ImageBase ,
		K1 extends Kernel1D, K2 extends Kernel2D> {

	public D process( I input , double angle ) {
		return null;
	}

	public void addBasis( double angle , K1 horizontal , K1 vertical ) {

	}

	public void addBasis( double angle , K2 kernel ) {

	}
}
