
package utexas.cockrell.distributedsystems.housingallocation;

/**
 *
 * @author jsobocinski
 */
public class Main {

    public static boolean DEBUG = false;
    
    //We mostly focus on the Las Vegas cycle detection, so this should stay true for optimal performance
    public static boolean USE_LAS_VEGAS = true;
    
    public static final int NUM_HOUSE_AND_AGENTS = 5;

    public static void main(String[] args) {

        Agency agency = Agency.getInstance();
    }

}
