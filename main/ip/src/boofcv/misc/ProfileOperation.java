/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.misc;

/**
 * @author Peter Abeles
 */
public class ProfileOperation {

    /**
     * See how long it takes to run the process 'num' times and print the results
     * to standard out
     *
     */
    public static void profile( Performer performer , int num ) {

        long deltaTime = measureTime(performer,num);

        System.out.printf("%30s time = %8d ms per frame = %8.3f\n",
                performer.getName(),deltaTime,(deltaTime/(double)num));
//        System.out.println(performer.getClass().getSimpleName()+
//                " time = "+deltaTime+"  ms per frame "+(deltaTime/(double)num));
    }

	public static void printOpsPerSec( Performer performer , long minTestTime )
	{
		try {
			double opsPerSecond = profileOpsPerSec(performer,minTestTime, false);

			String name = performer.getName() == null ? performer.getClass().getSimpleName() : performer.getName();
			System.out.printf("%30s  ops/sec = %7.3f\n",name,opsPerSecond);
		} catch( RuntimeException e ) {
			e.printStackTrace();
			System.out.printf("%30s  FAILED\n",performer.getClass().getSimpleName());
		}
	}

	public static double profileOpsPerSec(Performer performer, long minTestTime, boolean warmUp)
	{
		if( warmUp )
			performer.process();

		int N = 1;
		long elapsedTime;
		while( true ) {
			elapsedTime = measureTime(performer,N);
			if(elapsedTime >= minTestTime)
				break;
			N = N*2;
		}

		return (double)N/(elapsedTime/1000.0);
	}

	public static long measureTime( Performer performer , int num )
	{
		long startTime = System.nanoTime();
		for( int i = 0; i < num; i++ ) {
			performer.process();
		}
		long stopTime = System.nanoTime();

		return (stopTime-startTime)/1000000L;
	}
}
