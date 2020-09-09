/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.robust;

import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.List;

/**
 * Wrapper around {@link DistanceFromModel} that allows it ot be used by {@link DistanceFromModelViews}.
 *
 * @author Peter Abeles
 */
public class DistanceFromModelIntoViews<Model, Point, Camera> implements DistanceFromModelViews<Model, Point, Camera> {

	DistanceFromModel<Model, Point> alg;
	int numberOfViews;

	public DistanceFromModelIntoViews( DistanceFromModel<Model, Point> alg, int numberOfViews ) {
		this.alg = alg;
		this.numberOfViews = numberOfViews;
	}

	@Override public double distance( Point pt ) {return alg.distance(pt); }
	@Override public void distances( List<Point> points, double[] results ) {alg.distances(points, results);}
	@Override public int getNumberOfViews() { return numberOfViews; }
	@Override public void setView( int view, Camera viewInfo ) {}
	@Override public void setModel( Model model ) {alg.setModel(model);}
	@Override public Class<Point> getPointType() {return alg.getPointType();}
	@Override public Class<Model> getModelType() {return alg.getModelType();}
}
