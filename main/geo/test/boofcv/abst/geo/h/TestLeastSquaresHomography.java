/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.h;

import boofcv.abst.geo.GeneralTestRefineHomography;
import boofcv.alg.geo.ModelObservationResidualN;
import boofcv.alg.geo.h.HomographyResidualSampson;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelRefine;
import org.ejml.data.DenseMatrix64F;

/**
 * @author Peter Abeles
 */
public class TestLeastSquaresHomography extends GeneralTestRefineHomography {

	@Override
	public GeoModelRefine<DenseMatrix64F,AssociatedPair> createAlgorithm() {
		ModelObservationResidualN r = new HomographyResidualSampson();
		return new LeastSquaresHomography(1e-8,200,r);
	}
}
