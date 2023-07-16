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
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import static boofcv.misc.BoofMiscOps.getOrThrow;

/**
 * Camera state for storing the zoom value
 */
public class BundleZoomState implements BundleCameraState {
	/** Value of zoom for the lens */
	@Getter @Setter public double zoom = 0.0;

	public BundleZoomState( double zoom ) {
		this.zoom = zoom;
	}

	public BundleZoomState() {}

	public BundleZoomState setTo( BundleZoomState src ) {
		this.zoom = src.zoom;
		return this;
	}

	@Override public BundleCameraState setTo( Map<String, Object> src ) {
		try {
			zoom = getOrThrow(src, "zoom");
			return this;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override public Map<String, Object> toMap() {
		var map = new HashMap<String, Object>();
		map.put("zoom", zoom);
		return map;
	}

	@Override public boolean isIdentical( BundleCameraState b ) {
		try {
			var bb = (BundleZoomState)b;
			return zoom == bb.zoom;
		} catch (Exception e) {
			return false;
		}
	}
}
