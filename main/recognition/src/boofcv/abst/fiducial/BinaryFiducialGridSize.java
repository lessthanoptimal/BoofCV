package boofcv.abst.fiducial;

/**
 * Represents the number of squares in a binary fiducial. The more squares, the higher the number
 * of distinct fiducials you can have. Of course, nothing is free, the higher the number
 * the less accurate recognition is. 3 Pratical types are supported: 3,4 and 5. The way to
 * calculate how many distinct fiducials you could generate is the following : 2^(N * N - 4).
 * The -4 is because the four corners are used to correctly orient the square before
 * decoding the number in it. So, a 3x3 matrix supports 32 markers, 4x4 4096 markers
 * and 5x5 2,097,152 markers. These three sizes should be good enough for most pratical
 * purposes.
 *
 * @author Nathan Pahucki for Capta360, <a href="mailto:npahucki@gmail.com"> npahucki@gmail.com</a>
 */
public enum BinaryFiducialGridSize {

    THREE_BY_THREE(3),
    FOUR_BY_FOUR(4),
    FIVE_BY_FIVE(5);


    private int elementsWide;

    BinaryFiducialGridSize(final int elements) {
            elementsWide = elements;
    }

    public int getWidth() {
            return elementsWide;
    }

    public int getNumberOfElements() {
            return elementsWide * elementsWide;
    }


    public int getNumberOfDistinctFiducials() {
        return (int) Math.pow(2, elementsWide * elementsWide - 4);
    }

    public static BinaryFiducialGridSize gridSizeForWidth(int gridWidth) {
        switch (gridWidth) {
            case 3:
                return THREE_BY_THREE;
            case 4:
                return FOUR_BY_FOUR;
            case 5:
                return FIVE_BY_FIVE;
            default:
                throw new IllegalArgumentException("3-5 are the only valid values for gridWidth");
        }
    }

    @Override
    public String toString() {
        return elementsWide + "x" + elementsWide;
    }
}


