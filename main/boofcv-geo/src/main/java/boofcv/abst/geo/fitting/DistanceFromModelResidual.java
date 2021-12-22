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

package boofcv.abst.geo.fitting;

import boofcv.alg.geo.ModelObservationResidual;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;

/**
 * Computes the error using {@link boofcv.alg.geo.ModelObservationResidual} for {@link DistanceFromModel}.
 * 
 * @author Peter Abeles
 */
public class DistanceFromModelResidual<Model, Point>
		implements DistanceFromModel<Model,Point>
{
	ModelObservationResidual<Model,Point> function;

	public DistanceFromModelResidual(ModelObservationResidual<Model, Point> function) {
		this.function = function;
	}

	@Override
	public void setModel(Model F) {
		function.setModel(F);
	}

	@Override
	public double distance(Point pt) {
		double r = function.computeResidual(pt);
		return r*r;
	}

	@Override
	public void distances(List<Point> associatedPairs, double[] distance) {
		for( int i = 0; i < associatedPairs.size(); i++ ) {
			Point p = associatedPairs.get(i);
			
			double r = function.computeResidual(p);
			distance[i] = r*r;
		}
	}

	@Override
	public Class<Point> getPointType() {
		throw new RuntimeException("Not supported");
	}

	@Override
	public Class<Model> getModelType() {
		throw new RuntimeException("Not supported");
	}
}
