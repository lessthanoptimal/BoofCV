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

import boofcv.struct.geo.GeoModelEstimator1;
import boofcv.struct.geo.Point2D4D;
import org.ejml.data.DMatrixRMaj;

/**
 * <p>
 * Interface for computing multiple solution to the Projective N-Point (PrNP) problem. Given a set of
 * observations from a single view and the known 3D homogenous location of the points being observed, estimate
 * the projective camera transform.
 * </p>
 * <p>
 * <b>Observations are in pixel coordinates</b>
 * </p>
 *
 * @author Peter Abeles
 */
public interface Estimate1ofPrNP extends GeoModelEstimator1<DMatrixRMaj, Point2D4D> {
}
