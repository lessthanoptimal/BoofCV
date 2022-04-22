/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.DistanceFromModelMultiView2;
import boofcv.struct.distort.Point3Transform2_F64;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ransac.Ransac;

/**
 * Extension of {@link Ransac} for two calibrated camera views. Input observations will be in pointing vector
 * coordinates.
 *
 * @author Peter Abeles
 */
public class RansacCalibrated2<Model, Point> extends Ransac<Model, Point>
		implements ModelMatcherMultiview2<Model, Point> {
	private DistanceFromModelMultiView2<Model, Point> modelDistance;

	public RansacCalibrated2( long randSeed, int maxIterations, double thresholdFit,
							  ModelManager<Model> modelManager,
							  ModelGenerator<Model, Point> modelGenerator,
							  DistanceFromModelMultiView2<Model, Point> modelDistance ) {
		super(randSeed, maxIterations, thresholdFit, modelManager, modelDistance.getPointType());
		this.modelDistance = modelDistance;
		setModel(() -> modelGenerator, () -> modelDistance);
	}

	@Override public void setDistortion( int view, Point3Transform2_F64 intrinsic ) {
		this.modelDistance.setDistortion(view, intrinsic);
	}

	@Override
	public int getNumberOfViews() {
		return modelDistance.getNumberOfViews();
	}
}
