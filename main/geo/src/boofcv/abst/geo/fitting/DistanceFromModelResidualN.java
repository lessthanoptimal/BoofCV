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

package boofcv.abst.geo.fitting;

import boofcv.alg.geo.ModelObservationResidualN;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;

/**
 * Computes the error using {@link boofcv.alg.geo.ModelObservationResidual} for {@link org.ddogleg.fitting.modelset.DistanceFromModel}.
 *
 * @author Peter Abeles
 */
public class DistanceFromModelResidualN<Model, Point>
		implements DistanceFromModel<Model,Point>
{
	ModelObservationResidualN<Model,Point> function;

	double residuals[];
	
	public DistanceFromModelResidualN(ModelObservationResidualN<Model, Point> function) {
		this.function = function;
		residuals = new double[ function.getN() ];
	}

	@Override
	public void setModel(Model F) {
		function.setModel(F);
	}

	@Override
	public double computeDistance(Point pt) {
		function.computeResiduals(pt,residuals,0);
		
		double dist = 0;
		for( int i = 0; i < residuals.length; i++ ) {
			double r = residuals[i];
			dist += r*r;
		}
		return dist;
	}

	@Override
	public void computeDistance(List<Point> associatedPairs, double[] distance) {
		for( int i = 0; i < associatedPairs.size(); i++ ) {
			Point p = associatedPairs.get(i);

			function.computeResiduals(p,residuals,0);

			double dist = 0;
			for( int j = 0; j < residuals.length; j++ ) {
				double r = residuals[j];
				dist += r*r;
			}
			distance[i] = dist;
		}
	}
}
