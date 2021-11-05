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

package boofcv.struct;

import lombok.Getter;
import lombok.Setter;

/**
 * Specifies a length as a fixed length or relative to the total size of some other object.
 *
 * @author Peter Abeles
 */
@Getter @Setter
public class ConfigLength implements Configuration {
	/**
	 * If in fixed mode this is the length or it's the minimum length of a relative length is being specified
	 */
	public double length = -1;
	/**
	 * If &ge; 0 the length is relative to the total size and the 'fixed' number is treated
	 * as a minium size.
	 */
	public double fraction = -1;

	public ConfigLength( double length, double fraction ) {
		this.length = length;
		this.fraction = fraction;
	}

	public ConfigLength() {}

	public static ConfigLength fixed( double length ) {
		return new ConfigLength(length, -1);
	}

	public static ConfigLength relative( double fraction, double minimum ) {
		return new ConfigLength(minimum, fraction);
	}

	public void setFixed( double length ) {
		this.fraction = -1;
		this.length = length;
	}

	public void setRelative( double fraction, double minimum ) {
		this.fraction = fraction;
		this.length = minimum;
	}

	/**
	 * Computes the length. If a relative length is used then it will be relative to the input.
	 *
	 * @param totalLength What a relative value is relative to
	 * @return The length
	 */
	public double compute( double totalLength ) {

		double size;
		if (fraction >= 0) {
			size = fraction*totalLength;
			size = Math.max(size, length);
		} else {
			size = length;
		}

		return size;
	}

	/** True if a relative length is specified */
	public boolean isRelative() {
		return fraction >= 0;
	}

	/** True if a fixed length is specified */
	public boolean isFixed() {
		return fraction < 0;
	}

	public int computeI( double totalLength ) {
		double size = compute(totalLength);
		if (size >= 0)
			return (int)Math.round(size);
		else
			return -1;
	}

	/** Returns the length as a rounded integer. */
	public int getLengthI() {
		return (int)Math.round(length);
	}

	@Override public void checkValidity() {
		if (length < 0 && fraction < 0)
			throw new IllegalArgumentException("length and/or fraction must be >= 0");
		if (isRelative())
			if (fraction < 0 || fraction > 1.0)
				throw new IllegalArgumentException("Fractional value must be from 0.0maxFeatures to 1.0, inclusive");
	}

	public ConfigLength setTo( ConfigLength src ) {
		this.length = src.length;
		this.fraction = src.fraction;
		return this;
	}

	public ConfigLength copy() {
		return new ConfigLength().setTo(this);
	}

	@Override public String toString() {
		String out = "ConfigLength{";
		if (fraction >= 0)
			out += "fraction=" + fraction + ", minimum=" + length;
		else
			out += ", length=" + length;
		out += '}';
		return out;
	}
}
