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

package boofcv.factory.fiducial;

/**
 * List of pre-generated dictionaries
 */
public enum HammingDictionary {
	/** Custom dictionary */
	CUSTOM,
	/** ArUco's original dictionary. 5x5 grid with 1024 ids. */
	ARUCO_ORIGINAL,
	/** ArUco 4x4 grid with 250 ids. From ArUco 3 */
	ARUCO_MIP_16h3,
	/** ArUco 5x5 grid with 100 ids. From ArUco 3 */
	ARUCO_MIP_25h7,
	/** ArUco 6x6 grid with 250 ids. From ArUco 3 */
	ARUCO_MIP_36h12,
	/** ArUco 4x4 grid with 1000 ids from OpenCV. Not recommended as there are multiple duplicates */
	ARUCO_OCV_4x4_1000,
	/** ArUco 5x5 grid with 1000 ids from OpenCV. */
	ARUCO_OCV_5x5_1000,
	/** ArUco 6x6 grid with 1000 ids from OpenCV. */
	ARUCO_OCV_6x6_1000,
	/** ArUco 7x7 grid with 1000 ids from OpenCV. */
	ARUCO_OCV_7x7_1000,
	/** AprilTag 4x4 grid with 30 ids. From ArUco 3 */
	APRILTAG_16h5,
	/** AprilTag 5x5 grid with 242 ids. From ArUco 3 */
	APRILTAG_25h7,
	/** AprilTag 5x5 grid with 35 ids. From ArUco 3 */
	APRILTAG_25h9,
	/** AprilTag 6x6 grid with 2320 ids. From ArUco 3 */
	APRILTAG_36h10,
	/** AprilTag 6x6 grid with 587 ids. From ArUco 3 */
	APRILTAG_36h11;

	/**
	 * Returns all dictionaries but custom
	 */
	public static HammingDictionary[] allPredefined() {
		HammingDictionary[] all = values();
		HammingDictionary[] ret = new HammingDictionary[all.length - 1];
		System.arraycopy(all, 1, ret, 0, all.length - 1);
		return ret;
	}
}
