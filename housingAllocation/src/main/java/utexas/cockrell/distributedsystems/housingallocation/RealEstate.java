/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utexas.cockrell.distributedsystems.housingallocation;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author jsobocinski
 */
public class RealEstate {

    private static boolean init = false;
    private static Random random = new Random();

    private static RealEstate realEstate;

    CopyOnWriteArrayList housingList = new CopyOnWriteArrayList();

    private RealEstate() {
        for (int i = 0; i < Main.NUM_HOUSES; i++) {
            housingList.add(new House());
        }
    }

    public static RealEstate getInstance() {
        if (!init) {
            realEstate = new RealEstate();
            init = true;
        }
        return realEstate;
    }

    public class House {

        private final int preferenceOne;

        private House() {
            preferenceOne = random.nextInt(Main.NUM_HOUSE_VARIATIONS);
        }
    }
}
