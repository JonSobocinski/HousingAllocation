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
public class Agency {

    private static boolean init = false;
    private static Agency agency;
    private static Random random = new Random();

    CopyOnWriteArrayList agentList = new CopyOnWriteArrayList();

    private Agency() {
        for (int i = 0; i < Main.NUM_AGENTS; i++) {
            agentList.add(new Agent());
        }
    }

    public static Agency getInstance() {
        if (!init) {
            agency = new Agency();
            init = true;
        }
        return agency;
    }

    public class Agent {

        private final int preferenceOne;

        private Agent() {
            preferenceOne = random.nextInt(Main.NUM_AGENT_VARIATRIONS);
        }

    }

}
