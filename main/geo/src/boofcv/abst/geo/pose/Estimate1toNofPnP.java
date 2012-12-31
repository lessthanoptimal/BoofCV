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

package boofcv.abst.geo.pose;

import boofcv.abst.geo.EstimateNofPnP;
import boofcv.abst.geo.GeoModelEstimator1toN;
import boofcv.struct.geo.GeoModelEstimator1;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.se.Se3_F64;

/**
 * Implementation of {@link GeoModelEstimator1toN} for PnP.
 *
 * @author Peter Abeles
 */
public class Estimate1toNofPnP extends GeoModelEstimator1toN<Se3_F64,Point2D3D>
		implements EstimateNofPnP
{
	public Estimate1toNofPnP(GeoModelEstimator1<Se3_F64, Point2D3D> alg) {
		super(alg);
	}
}
