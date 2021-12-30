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

package boofcv.alg.geo;

import boofcv.struct.calib.CameraPinhole;
import org.ddogleg.fitting.modelset.DistanceFromModel;

/**
 * Computes the observation errors in pixels when the input is in normalized image coordinates.
 * The most basic camera model {@link CameraPinhole} is used because errors can be computed very quickly using
 * this model. An approximate camera model should be used when the known one isn't a pinhole.
 *
 * @author Peter Abeles
 */
public interface DistanceFromModelMultiView<Model, Point> extends DistanceFromModel<Model, Point> {

	void setIntrinsic( int view, CameraPinhole intrinsic );

	int getNumberOfViews();
}
