package utexas.cockrell.distributedsystems.housingallocation;

/**
 *
 * @author jsobocinski
 */
public class Main {

    public static boolean DEBUG = true;

    //We mostly focus on the Las Vegas cycle detection, so this should stay true for optimal performance
    public static boolean USE_LAS_VEGAS = true;

    public static final int NUM_HOUSE_AND_AGENTS = 10;

    public static void main(String[] args) throws InterruptedException {
        Agency agency = Agency.getInstance();//Agency should auto run on completion
    }
}
