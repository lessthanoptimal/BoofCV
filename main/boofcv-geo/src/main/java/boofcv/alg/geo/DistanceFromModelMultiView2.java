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

package boofcv.alg.geo;

import boofcv.struct.distort.Point3Transform2_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;

/**
 * Computes the observation errors in pixels when the input is in point vector coordinates.
 *
 * @author Peter Abeles
 */
public interface DistanceFromModelMultiView2<Model, Point> extends DistanceFromModel<Model, Point> {

	void setDistortion( int view, Point3Transform2_F64 intrinsic );

	int getNumberOfViews();
}
