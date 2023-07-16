/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle.cameras;

import boofcv.abst.geo.bundle.BundleCameraState;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestBundleZoomState extends BoofStandardJUnit {
	@Test void setToRegular() {
		var found = new BundleZoomState().setTo(new BundleZoomState(5.0));
		assertEquals(5.0, found.zoom);
	}

	@Test void toMap_setTo() {
		var found = (BundleZoomState)new BundleZoomState().setTo(new BundleZoomState(5.0).toMap());
		assertEquals(5.0, found.zoom);
	}

	@Test void isIdentical() {
		var a = new BundleZoomState(5.0);
		var b = new BundleZoomState(5.0);
		var c = new BundleZoomState(5.01);

		assertTrue(a.isIdentical(a));
		assertTrue(a.isIdentical(b));
		assertFalse(a.isIdentical(c));
		assertFalse(c.isIdentical(a));

		assertFalse(a.isIdentical(new BundleCameraState() {
			@Override public BundleCameraState setTo( Map<String, Object> src ) {return null;}

			@Override public Map<String, Object> toMap() {return null;}

			@Override public boolean isIdentical( BundleCameraState b ) {return false;}
		}));
	}
}
