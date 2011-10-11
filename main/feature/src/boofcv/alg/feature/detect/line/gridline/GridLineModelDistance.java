/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.line.gridline;


import boofcv.numerics.fitting.modelset.DistanceFromModel;
import georegression.struct.line.LinePolar2D_F32;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class GridLineModelDistance implements DistanceFromModel<LinePolar2D_F32,Edgel> {

	LinePolar2D_F32 lineParam;

	@Override
	public void setModel(LinePolar2D_F32 lineParam) {
		this.lineParam = lineParam;
	}

	@Override
	public double computeDistance(Edgel pt) {
		// todo see if edge orientation and point orientation are compatible


		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void computeDistance(List<Edgel> edgels, double[] distance) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
