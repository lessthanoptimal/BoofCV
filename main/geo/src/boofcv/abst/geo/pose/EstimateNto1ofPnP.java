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

import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.abst.geo.GeoModelEstimatorNto1;
import boofcv.alg.geo.pose.PnPDistanceReprojectionSq;
import boofcv.struct.geo.GeoModelEstimatorN;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;

/**
 * Implementation of {@link GeoModelEstimatorNto1} for PnP problem.
 *
 * @author Peter Abeles
 */
public class EstimateNto1ofPnP extends GeoModelEstimatorNto1<Se3_F64,Point2D3D>
		implements Estimate1ofPnP
{
	public EstimateNto1ofPnP(GeoModelEstimatorN<Se3_F64, Point2D3D> alg, FastQueue<Se3_F64> solutions, int numTest) {
		super(alg, new PnPDistanceReprojectionSq(), solutions, numTest);
	}

	@Override
	protected void copy(Se3_F64 src, Se3_F64 dst) {
		dst.set(src);
	}
}
