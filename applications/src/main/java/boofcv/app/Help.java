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

package boofcv.app;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Application which provides help for command-line applications
 *
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes")
public class Help {

	static Class[] options = new Class[]{
			CreateFiducialSquareImage.class,
			CreateFiducialSquareBinary.class,
			CreateFiducialSquareHamming.class,
			CreateFiducialRandomDot.class,
			BatchRemoveLensDistortion.class,
			BatchDownsizeImage.class,
			FiducialDetection.class,
			CameraCalibrationMono.class,
			CameraCalibrationStereo.class,
			BatchScanQrCodes.class,
			CreateCalibrationTarget.class,
			CreateQrCodeDocument.class,
			DownSelectVideoFramesFor3DApp.class,
			SceneReconstruction.class,
			PointCloudViewerApp.class,
	};

	public static void printHelp() {
		System.out.println("Trying to run a command-line application?  Here are your options!");
		System.out.println();

		for (Class c : options) {
			System.out.println("  " + c.getSimpleName());
		}
		System.out.println("Example:");
		System.out.println("java -jar applications.jar " + options[0].getSimpleName());
	}

	public static void main( String[] args ) {
		if (args.length == 1) {
			if (args[0].equals("--GUI")) {
				new ApplicationLauncherGui();
				return;
			}
		}
		if (args.length > 0) {
			String[] truncated = new String[args.length - 1];
			System.arraycopy(args, 1, truncated, 0, truncated.length);

			try {
				Class appClass = Class.forName("boofcv.app." + args[0]);
				Method m = appClass.getMethod("main", String[].class);

				m.invoke(null, new Object[]{truncated});
			} catch (ClassNotFoundException e) {
				printHelp();
				System.out.println();
				System.out.println("Can't find application for " + args[0]);
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		} else {
			printHelp();
		}
	}
}
