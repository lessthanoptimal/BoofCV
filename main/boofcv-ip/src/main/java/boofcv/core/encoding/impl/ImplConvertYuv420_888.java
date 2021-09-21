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

package boofcv.core.encoding.impl;

import boofcv.core.encoding.ConvertYuv420_888.ProcessorYuv;
import boofcv.misc.BoofMiscOps;
import org.ddogleg.struct.DogArray_I8;
import pabeles.concurrency.GrowArray;

import java.nio.ByteBuffer;

// NOTES:
// Creating a concurrent version is a bit tricky since the input buffer needs to be locked AND
// the threads can't share the same processor, since it internally increments the pixel index

/**
 * Low level implementation of YUV-420 to RGB-888
 *
 * @author Peter Abeles
 */
public class ImplConvertYuv420_888 {
	public static void processYuv( ByteBuffer bufferY, ByteBuffer bufferU , ByteBuffer bufferV  ,
								   int width, int height, int strideY , int strideUV , int stridePixelUV,
								   GrowArray<DogArray_I8> workArrays, ProcessorYuv processor )
	{
		// U and V stride are the same by 420_888 specification

		// not sure the best way to compute this. The width of a plane should be used here and not the stride
		// but the plane's width isn't specified.
		int periodUV = (int)Math.round(width/(strideUV/(double)stridePixelUV));

		int workLength = strideY + strideUV + strideUV;
		workArrays.reset();
		byte[] work = BoofMiscOps.checkDeclare(workArrays.grow(),workLength,false);

		// Index of the start of the row in the buffer
		int positionY=0,positionUV=0;
		int rowBytesUV = ((width/periodUV)-1)*stridePixelUV+1;

		// start of each band in the work buffer
		int offsetU = strideY;
		int offsetV = strideY + strideUV;

		// save for debugging purposes
		int totalBytesY = bufferY.limit();
		int totalBytesU = bufferU.limit();
		int totalBytesV = bufferV.limit();

		int x=-1,y=-1,indexY=-1,indexU=-1,indexV=-1;
		try {
			for (y = 0; y < height; y++) {
				// Read all the data for this row from each plane
				bufferY.position(positionY);
				bufferY.get(work, 0, width);
				positionY += strideY;

				if (y % periodUV == 0) {
					bufferU.position(positionUV);
					bufferU.get(work, offsetU, rowBytesUV);
					bufferV.position(positionUV);
					bufferV.get(work, offsetV, rowBytesUV);
					positionUV += strideUV;
				}

				indexY = 0;
				indexU = offsetU;
				indexV = offsetV;

				for (x = 0; x < width; x++, indexY++) {
					processor.processYUV(work[indexY] & 0xFF, work[indexU] & 0xFF, work[indexV] & 0xFF);

					// this is intended to be a fast way to do if a == 0 ? 1 : 0
					int stepUV = stridePixelUV * ((x + 1) % periodUV == 0 ? 1 : 0);
					indexU += stepUV;
					indexV += stepUV;
				}
			}
		} catch( RuntimeException e ) {
			e.printStackTrace();

			String message = "Crashed in YUV. "+e.getMessage()+" bytes Y="+totalBytesY+" U="+totalBytesU+
					" V="+totalBytesV+" width="+width+" height="+height+" work.length="+work.length+
					" strideY="+strideY+" strideUV="+strideUV+" stridePixelUV="+stridePixelUV+" periodUV="+periodUV+
					" x="+x+" y="+y+" indexY="+indexY+" indexU="+indexU+" indexV="+indexV;
			throw new RuntimeException(message,e);
		}
	}
}
