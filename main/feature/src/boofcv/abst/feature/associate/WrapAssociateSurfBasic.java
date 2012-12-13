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

package boofcv.abst.feature.associate;

import boofcv.alg.feature.associate.AssociateSurfBasic;
import boofcv.struct.FastQueue;
import boofcv.struct.GrowingArrayInt;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.SurfFeature;

/**
 * Wrapper around {@Link AssociateSurfBasic} for {@link AssociateDescription}.
 *
 * @author Peter Abeles
 */
public class WrapAssociateSurfBasic implements AssociateDescription<SurfFeature> {

	AssociateSurfBasic alg;

	FastQueue<SurfFeature> listSrc;

	public WrapAssociateSurfBasic(AssociateSurfBasic alg) {
		this.alg = alg;
	}

	@Override
	public void setSource(FastQueue<SurfFeature> listSrc) {
		alg.setSrc(listSrc);
		this.listSrc = listSrc;
	}

	@Override
	public void setDestination(FastQueue<SurfFeature> listDst) {
		alg.setDst(listDst);
	}

	@Override
	public void associate() {
		alg.associate();
	}

	@Override
	public FastQueue<AssociatedIndex> getMatches() {
		return alg.getMatches();
	}

	@Override
	public GrowingArrayInt getUnassociatedSource() {
		return alg.getUnassociated();
	}
}
