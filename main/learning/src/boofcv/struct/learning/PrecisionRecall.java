/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.learning;

/**
 * Statistics related to precision and recall.
 *
 * @author Peter Abeles
 */
public class PrecisionRecall {
	public double TP;
	public double TN;
	public double FP;
	public double FN;

	public PrecisionRecall(double TP, double TN, double FP, double FN) {
		this.TP = TP;
		this.TN = TN;
		this.FP = FP;
		this.FN = FN;
	}

	public PrecisionRecall() {
	}

	public double getTruePositive() {
		return TP;
	}

	public double getTrueNegative() {
		return TN;
	}

	public double getFalsePositive() {
		return FP;
	}

	public double getFalseNegative() {
		return FN;
	}

	public double getFMeasure() {
		double P = getPrecision();
		double R = getRecall();

		return 2.0*(P*R)/(P+R);
	}

	public double getPrecision() {
		return TP/(TP+FP);
	}

	public double getRecall() {
		return TP/(TP+FN);
	}

}
