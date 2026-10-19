/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.chainrule;

import boofcv.abst.geo.bundle.BundleCameraState;

import java.util.Map;

public class DummyState implements BundleCameraState {
	// Encode 3 different data types
	String a;
	double b;
	int c;

	public DummyState( String a, double b, int c ) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	public DummyState() {}

	@Override public BundleCameraState setTo( Map<String, Object> src ) {
		this.a = (String)src.get("a");
		this.b = (double)src.get("b");
		this.c = (Integer)src.get("c");
		return this;
	}

	@Override public Map<String, Object> toMap() {
		return Map.of("a", a, "b", b, "c", c);
	}

	@Override public boolean isIdentical( BundleCameraState cmp ) {
		DummyState other = (DummyState)cmp;
		return a.equals(other.a) && b == other.b && c == other.c;
	}
}
