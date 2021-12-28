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

package boofcv.abst.feature.detdesc;

import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

/**
 * Abstract class with default implementations of functions. Primarily for testing
 *
 * @author Peter Abeles
 */
public abstract class DetectDescribePointAbstract<T extends ImageBase<T>, Desc extends TupleDesc<Desc>>
		implements DetectDescribePoint<T, Desc> {
	// @formatter:off
	@Override public Desc getDescription(int index) { throw new RuntimeException("implement"); }
	@Override public void detect(T input) { }
	@Override public boolean hasScale() { return false; }
	@Override public boolean hasOrientation() { return false; }
	@Override public ImageType<T> getInputType() { throw new RuntimeException("Implement"); }
	@Override public Desc createDescription() { throw new RuntimeException("Implement"); }
	@Override public Class<Desc> getDescriptionType() { throw new RuntimeException("Implement"); }
	@Override public int getNumberOfSets() { return 1; }
	@Override public int getSet(int index) { return 0; }
	@Override public int getNumberOfFeatures() { return 0; }
	@Override public Point2D_F64 getLocation(int featureIndex) { throw new RuntimeException("Implement"); }
	@Override public double getRadius(int featureIndex) { return 0; }
	@Override public double getOrientation(int featureIndex) { return 0; }
	// @formatter:on
}
