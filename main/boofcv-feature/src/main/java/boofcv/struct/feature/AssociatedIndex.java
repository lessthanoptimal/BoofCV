/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.feature;

import lombok.Data;

/**
 * Indexes of two associated features and the fit score..
 *
 * @author Peter Abeles
 */
@Data
public class AssociatedIndex {
	/** index of the feature in the source image */
	public int src;
	/** index of the feature in the destination image */
	public int dst;
	/** The association score. Meaning will very depending on implementation */
	public double fitScore;

	public AssociatedIndex( AssociatedIndex original ) {
		setTo(original);
	}

	public AssociatedIndex( int src, int dst, double fitScore ) {
		this.src = src;
		this.dst = dst;
		this.fitScore = fitScore;
	}

	public AssociatedIndex( int src, int dst ) {
		this.src = src;
		this.dst = dst;
		this.fitScore = 0;
	}

	public AssociatedIndex() {}

	public void setTo( int src, int dst, double fitScore ) {
		this.src = src;
		this.dst = dst;
		this.fitScore = fitScore;
	}

	public void setTo( int src, int dst ) {
		this.src = src;
		this.dst = dst;
		this.fitScore = 0;
	}

	public void setTo( AssociatedIndex a ) {
		src = a.src;
		dst = a.dst;
		fitScore = a.fitScore;
	}

	public boolean isIdentical( AssociatedIndex o ) {
		return src == o.src && dst == o.dst && fitScore == o.fitScore;
	}

	public AssociatedIndex copy() {
		return new AssociatedIndex(this);
	}
}
