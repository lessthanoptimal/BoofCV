/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.pose.PnPLepetitEPnP;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link PnPLepetitEPnP} for {@link boofcv.abst.geo.Estimate1ofPnP}.
 * 
 * @author Peter Abeles
 */
public class WrapPnPLepetitEPnP implements Estimate1ofPnP {

	PnPLepetitEPnP alg;

	List<Point3D_F64> worldPts = new ArrayList<>();
	List<Point2D_F64> observed = new ArrayList<>();

	public WrapPnPLepetitEPnP(PnPLepetitEPnP alg ) {
		this.alg = alg;
	}

	@Override
	public boolean process(List<Point2D3D> inputs , Se3_F64 solution ) {
		for( int i = 0; i < inputs.size(); i++ ) {
			Point2D3D pp = inputs.get(i);
			
			worldPts.add(pp.location);
			observed.add(pp.observation);
		}
		
		alg.process(worldPts,observed,solution);
		
		worldPts.clear();
		observed.clear();

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinPoints();
	}
}
