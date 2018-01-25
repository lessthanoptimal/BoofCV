/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.calibration;

/**
 * Statistics on how accurately the found model fit each image during calibration.
 *
 * @author Peter Abeles
 */
public class ImageResults {
	public double meanError;
	public double maxError;
	public double biasX;
	public double biasY;
	
	public double[] pointError;

	public ImageResults( int numPoints ) {
		pointError = new double[numPoints];
	}

	public double getMeanError() {
		return meanError;
	}

	public void setMeanError(double meanError) {
		this.meanError = meanError;
	}

	public double getMaxError() {
		return maxError;
	}

	public void setMaxError(double maxError) {
		this.maxError = maxError;
	}

	public double getBiasX() {
		return biasX;
	}

	public void setBiasX(double biasX) {
		this.biasX = biasX;
	}

	public double getBiasY() {
		return biasY;
	}

	public void setBiasY(double biasY) {
		this.biasY = biasY;
	}

	public double[] getPointError() {
		return pointError;
	}

	public void setPointError(double[] pointError) {
		this.pointError = pointError;
	}
}
