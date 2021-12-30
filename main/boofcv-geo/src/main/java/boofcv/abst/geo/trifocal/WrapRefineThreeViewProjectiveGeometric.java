/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.RefineThreeViewProjective;
import boofcv.alg.geo.trifocal.RefineThreeViewProjectiveGeometric;
import boofcv.struct.geo.AssociatedTriple;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Wrapper around {@link RefineThreeViewProjectiveGeometric}
 *
 * @author Peter Abeles
 */
public class WrapRefineThreeViewProjectiveGeometric implements RefineThreeViewProjective {

	RefineThreeViewProjectiveGeometric alg;

	public WrapRefineThreeViewProjectiveGeometric( RefineThreeViewProjectiveGeometric alg ) {
		this.alg = alg;
	}

	@Override
	public boolean process( List<AssociatedTriple> observations,
							DMatrixRMaj P2, DMatrixRMaj P3,
							DMatrixRMaj refinedP2, DMatrixRMaj refinedP3 ) {

		refinedP2.setTo(P2);
		refinedP3.setTo(P3);

		return alg.refine(observations, refinedP2, refinedP3);
	}

	public RefineThreeViewProjectiveGeometric getAlg() {
		return alg;
	}
}
