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

package boofcv.alg.geo.robust;

import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.struct.calib.CameraPinhole;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ransac.Ransac;

/**
 * Extension of {@link Ransac} for two calibrated camera views. Input point will be in normalized image coordinates
 *
 * @author Peter Abeles
 */
public class RansacCalibrated<Model, Point> extends Ransac<Model, Point>
		implements ModelMatcherMultiview<Model, Point> {
	private DistanceFromModelMultiView<Model, Point> modelDistance;

	public RansacCalibrated( long randSeed, int maxIterations, double thresholdFit,
							 ModelManager<Model> modelManager,
							 ModelGenerator<Model, Point> modelGenerator,
							 DistanceFromModelMultiView<Model, Point> modelDistance ) {
		super(randSeed, maxIterations, thresholdFit, modelManager, modelDistance.getPointType());
		this.modelDistance = modelDistance;
		setModel(() -> modelGenerator, () -> modelDistance);
	}

	@Override
	public void setIntrinsic( int view, CameraPinhole intrinsic ) {
		this.modelDistance.setIntrinsic(view, intrinsic);
	}

	@Override
	public int getNumberOfViews() {
		return modelDistance.getNumberOfViews();
	}
}
