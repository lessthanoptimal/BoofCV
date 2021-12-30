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

package boofcv.struct.sparse;

/**
 * Specifies a pixel's gradient using double values.
 *
 * @author Peter Abeles
 */
public class GradientValue_F64 implements GradientValue {

	public double x;
	public double y;

	@Override
	public void setTo( /**/double dx, /**/double dy ) {
		this.x = (double)dx;
		this.y = (double)dy;
	}

	@Override
	public /**/double getX() {
		return x;
	}

	@Override
	public /**/double getY() {
		return y;
	}
}
