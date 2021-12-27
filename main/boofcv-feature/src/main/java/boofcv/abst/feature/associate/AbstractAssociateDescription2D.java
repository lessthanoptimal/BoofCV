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
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;

/**
 * Default implementations for all functions in {@link AssociateDescription2D}
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway"})
public class AbstractAssociateDescription2D<Desc> implements AssociateDescription2D<Desc> {
	// @formatter:off
	@Override public void initialize(int imageWidth, int imageHeight) {}
	@Override public void setSource(FastAccess<Point2D_F64> location, FastAccess<Desc> descriptions) {}
	@Override public void setDestination(FastAccess<Point2D_F64> location, FastAccess<Desc> descriptions) {}
	@Override public Class<Desc> getDescriptionType() {return null;}
	@Override public void associate() {}
	@Override public FastAccess<AssociatedIndex> getMatches() {return null;}
	@Override public DogArray_I32 getUnassociatedSource() {return null;}
	@Override public DogArray_I32 getUnassociatedDestination() {return null;}
	@Override public void setMaxScoreThreshold(double score) {}
	@Override public MatchScoreType getScoreType() {return null;}
	@Override public boolean uniqueSource() {return false;}
	@Override public boolean uniqueDestination() {return false;}
	// @formatter:on
}
