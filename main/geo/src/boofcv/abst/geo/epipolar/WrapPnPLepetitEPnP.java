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

package boofcv.abst.geo.epipolar;

import boofcv.alg.geo.PointPositionPair;
import boofcv.alg.geo.epipolar.pose.PnPLepetitEPnP;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link PnPLepetitEPnP} for {@link PerspectiveNPoint}.
 * 
 * @author Peter Abeles
 */
public class WrapPnPLepetitEPnP implements PerspectiveNPoint {

	PnPLepetitEPnP alg;

	List<Point3D_F64> worldPts = new ArrayList<Point3D_F64>();
	List<Point2D_F64> observed = new ArrayList<Point2D_F64>();

	Se3_F64 motion;

	public WrapPnPLepetitEPnP(PnPLepetitEPnP alg ) {
		this.alg = alg;
	}

	@Override
	public void process(List<PointPositionPair> inputs) {
		for( int i = 0; i < inputs.size(); i++ ) {
			PointPositionPair pp = inputs.get(i);
			
			worldPts.add(pp.location);
			observed.add(pp.observed);
		}
		
		alg.process(worldPts,observed);
		motion = alg.getSolutionMotion();
		
		worldPts.clear();
		observed.clear();
	}

	@Override
	public Se3_F64 getPose() {
		return motion;
	}

	@Override
	public int getMinPoints() {
		return alg.getMinPoints();
	}
}
