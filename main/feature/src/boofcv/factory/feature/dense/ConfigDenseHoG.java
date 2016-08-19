/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.dense;

import boofcv.struct.Configuration;

/**
 * Configuration for {@link FactoryDescribeImageDense}
 *
 * @author Peter Abeles
 */
public class ConfigDenseHoG implements Configuration {

	/**
	 * Number of orientation bins.
	 */
	public int orientationBins = 9;
	/**
	 * Number of pixels in a cell.
	 */
	public int pixelsPerCell = 8;
	/**
	 * Number of cells wide a block is along x-axis
	 */
	public int cellsPerBlockX = 3;
	/**
	 * Number of cells wide a block is along x-axis
	 */
	public int cellsPerBlockY = 3;

	/**
	 * Number of cells that are skipped between two blocks.
	 */
	public int stepBlock = 1;

	/**
	 * If set to true a faster variant of HOG will be used which doesn't apply spatial normalization.
	 * Set to false to replicate the version of HOG proposed in the original paper.
	 */
	public boolean fastVariant = true;

	@Override
	public void checkValidity() {

	}
}
