package gecv;

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
			double opsPerSecond = profileOpsPerSec(performer,minTestTime);

			System.out.printf("%30s  ops/sec = %7.3f\n",performer.getClass().getSimpleName(),opsPerSecond);
		} catch( RuntimeException e ) {
			System.out.printf("%30s  FAILED\n",performer.getClass().getSimpleName());
		}
	}

	public static double profileOpsPerSec( Performer performer , long minTestTime )
	{
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
