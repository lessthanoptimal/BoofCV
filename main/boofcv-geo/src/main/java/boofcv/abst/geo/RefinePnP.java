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

package boofcv.abst.geo;

import boofcv.struct.geo.Point2D3D;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ModelFitter;

/**
 * Refines a pose estimate given a set of observations and associated 3D point in world coordinates. Observations
 * are in normalized image coordinates.
 *
 * @author Peter Abeles
 */
public interface RefinePnP extends ModelFitter<Se3_F64, Point2D3D> {}
