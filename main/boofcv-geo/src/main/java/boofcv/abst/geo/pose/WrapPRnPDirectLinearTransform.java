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

package boofcv.abst.geo.pose;

import boofcv.abst.geo.Estimate1ofPrNP;
import boofcv.abst.geo.EstimateNofPrNP;
import boofcv.alg.geo.pose.PRnPDirectLinearTransform;
import boofcv.struct.geo.Point2D4D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link boofcv.alg.geo.pose.PRnPDirectLinearTransform} for {@link EstimateNofPrNP}
 *
 * @author Peter Abeles
 */
public class WrapPRnPDirectLinearTransform implements Estimate1ofPrNP {
	PRnPDirectLinearTransform alg;

	List<Point4D_F64> worldPts = new ArrayList<>();
	List<Point2D_F64> observed = new ArrayList<>();

	public WrapPRnPDirectLinearTransform( PRnPDirectLinearTransform alg ) {
		this.alg = alg;
	}

	@Override
	public boolean process( List<Point2D4D> inputs, DMatrixRMaj solution ) {
		for (int i = 0; i < inputs.size(); i++) {
			Point2D4D pp = inputs.get(i);

			worldPts.add(pp.location);
			observed.add(pp.observation);
		}

		alg.process(worldPts, observed, solution);

		worldPts.clear();
		observed.clear();

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints();
	}
}
