/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.trifocal;

import boofcv.abst.geo.Estimate1ofTrifocalTensor;
import boofcv.alg.geo.trifocal.TrifocalLinearPoint7;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;

import java.util.List;

/**
 * Wrapper around either {@link boofcv.alg.geo.trifocal.TrifocalLinearPoint7} for {@link boofcv.abst.geo.Estimate1ofTrifocalTensor}.
 *
 * @author Peter Abeles
 */
public class WrapTrifocalLinearPoint7 implements Estimate1ofTrifocalTensor {

	TrifocalLinearPoint7 alg = new TrifocalLinearPoint7();

	@Override
	public boolean process(List<AssociatedTriple> points, TrifocalTensor estimatedModel) {
		return( alg.process(points,estimatedModel) );
	}

	@Override
	public int getMinimumPoints() {
		return 7;
	}
}
