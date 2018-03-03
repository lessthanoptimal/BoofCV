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

package boofcv.app.calib;

import boofcv.alg.geo.calibration.CalibrationObservation;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;

/**
 * used to detect when the user is holding the fiducial still.
 *
 * @author Peter Abeles
 */
public class DetectUserActions {

	//-------------- motion parameters
	double thresholdDistance;

	double stationaryStart;
	double stationaryTime;

	CalibrationObservation previous = new CalibrationObservation(0,0);
	CalibrationObservation first = new CalibrationObservation(0,0);

	CalibrationObservation points;
	int numMissed;

	public void setImageSize( int width , int height ) {
		int size = Math.min(width,height);

		thresholdDistance = size*0.005;
	}

	public void update( boolean detected , CalibrationObservation points ) {
		if( detected ) {
			this.points = points;
			stationaryTime = checkStationary();
			numMissed = 0;
		} else {
			if( ++numMissed > 5 ) {
				previous.reset();
				stationaryStart = System.currentTimeMillis();
			}
		}
	}

	public double checkStationary( ) {
		if( previous.size() != points.size() ) {
			previous.setTo(points);
			first.setTo(points);
			stationaryStart = System.currentTimeMillis();
			return 0;
		} else {
			double averagePrev = averageDifference(previous);
			double averageFirst = averageDifference(first);

			previous.setTo(points);
			if( averagePrev > thresholdDistance || averageFirst > thresholdDistance*10 ) {
				stationaryStart = System.currentTimeMillis();
				first.setTo(points);
			}
			return (System.currentTimeMillis()-stationaryStart)/1000.0;
		}
	}

	private double averageDifference( CalibrationObservation reference ) {
		double average = 0;
		for( int i = 0; i < points.size(); i++ ) {
			double difference = reference.points.get(i).distance(points.points.get(i));

			average += difference;
		}
		average /= points.size();
		return average;
	}

	public void resetStationary() {
		stationaryTime = System.currentTimeMillis();
	}

	public double getStationaryTime() {
		return stationaryTime;
	}

	public boolean isAtLocation( double x , double y ) {
		double centerX = 0, centerY = 0;

		for (int i = 0; i < points.size(); i++) {
			Point2D_F64 p = points.points.get(i);

			centerX += p.x;
			centerY += p.y;
		}

		centerX /= points.size();
		centerY /= points.size();

		double distance = UtilPoint2D_F64.distanceSq(centerX,centerY,x,y);

		return distance <= thresholdDistance*4;
	}
}
