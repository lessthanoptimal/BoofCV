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

package boofcv.abst.feature.associate;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;

/**
 * @author Peter Abeles
 */
public abstract class StandardAssociateDescription2DChecks<Desc> extends StandardAssociateDescriptionChecks<Desc>{

	protected StandardAssociateDescription2DChecks(Class<Desc> descType) {
		super(descType);
	}

	public abstract AssociateDescription2D<Desc> createAssociate2D();

	@Override
	public AssociateDescription<Desc> createAssociate() {
		AssociateDescription2D<Desc> a = createAssociate2D();
		a.initialize(100,100);
		return new From2DTo1D<>(a);
	}

	public static class From2DTo1D<Desc>
			implements AssociateDescription<Desc>
	{
		AssociateDescription2D<Desc> alg;
		public From2DTo1D(AssociateDescription2D<Desc> alg) { this.alg = alg; }

		@Override
		public void setSource(FastAccess<Desc> listSrc) {
			var points = new DogArray<>(Point2D_F64::new);
			for (int i = 0; i < listSrc.size; i++) {
				points.grow().setTo(5,5);
			}
			alg.setSource(points,listSrc);
		}

		@Override
		public void setDestination(FastAccess<Desc> listDst) {
			var points = new DogArray<>(Point2D_F64::new);
			for (int i = 0; i < listDst.size; i++) {
				points.grow().setTo(5,5);
			}
			alg.setDestination(points,listDst);

		}

		// @formatter:off
		@Override public void associate() { alg.associate(); }
		@Override public FastAccess<AssociatedIndex> getMatches() { return alg.getMatches(); }
		@Override public DogArray_I32 getUnassociatedSource() { return alg.getUnassociatedSource(); }
		@Override public DogArray_I32 getUnassociatedDestination() { return alg.getUnassociatedDestination(); }
		@Override public void setMaxScoreThreshold(double score) { alg.setMaxScoreThreshold(score); }
		@Override public MatchScoreType getScoreType() { return alg.getScoreType(); }
		@Override public boolean uniqueSource() { return alg.uniqueSource(); }
		@Override public boolean uniqueDestination() { return alg.uniqueDestination(); }
		@Override public Class<Desc> getDescriptionType() { return alg.getDescriptionType(); }
		// @formatter:on
	}
}
