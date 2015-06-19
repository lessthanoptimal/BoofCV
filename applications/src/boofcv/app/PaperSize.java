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

package boofcv.app;

/**
 * Used to specify the size of different standard pieces of paper
 *
 * @author Peter Abeles
 */
public enum PaperSize {
	A0(841 , 1189, Unit.MILLIMETER),
	A1(594 , 841, Unit.MILLIMETER),
	A2(420 , 594, Unit.MILLIMETER),
	A3(297 , 420, Unit.MILLIMETER),
	A4(210 , 297, Unit.MILLIMETER),
	LEGAL(8.5 , 14.0, Unit.INCH),
	LETTER(8.5 , 11.0, Unit.INCH);

	PaperSize(double width, double height, Unit unit) {
		this.width = width;
		this.height = height;
		this.unit = unit;
	}

	Unit unit;
	double width;
	double height;

	public Unit getUnit() {
		return unit;
	}

	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}
}
