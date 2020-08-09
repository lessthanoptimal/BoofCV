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

package boofcv.abst.geo.selfcalib;

import boofcv.alg.geo.MetricCameras;
import boofcv.struct.geo.AssociatedTuple;
import boofcv.struct.image.ImageDimension;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Interface for going from a set of projective cameras and pixel observations into calibrated metric cameras
 *
 * @author Peter Abeles
 */
public interface ProjectiveToMetricCameras {
	/**
	 * Computes metric upgrade from projective cameras.
	 *
	 * @param dimensions (Input) Dimension of input image in each view, including the first
	 * @param views (Input) List of projective camera matrices. First view is P=[I|0] implicitly and is not included
	 * @param observations (Input) Observations of common features among all the views. Observations are in pixels.
	 *                     some implementations might require the pixel observations be offset by the principle point.
	 * @param results (Output) Storage for found metric upgrade
	 * @return true if successful or false if it failed
	 */
	boolean process(List<ImageDimension> dimensions, List<DMatrixRMaj> views, List<AssociatedTuple> observations,
					MetricCameras results);

	/**
	 * Returns the minimum number of views required to estimate the metric upgrade
	 */
	int getMinimumViews();
}
