/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utexas.cockrell.distributedsystems.housingallocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    CopyOnWriteArrayList<Agent> agentList = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<Integer> availableHouses = new CopyOnWriteArrayList<>();

    private Agency() {
        for (int i = 0; i < Main.NUM_HOUSE_AND_AGENTS; i++) {
            agentList.add(new Agent(i));
            availableHouses.add(i);
        }
    }

    public static Agency getInstance() {
        if (!init) {
            agency = new Agency();
            init = true;
        }
        return agency;
    }

    public class Agent implements Runnable {

        private final List<Integer> preferenceList;
        private int currentHouse;
        
        private boolean firstPreference = false;

        private Agent(int currentHouse) {
            this.currentHouse = currentHouse;

            preferenceList = new ArrayList<>();

            for (int i = 0; i < Main.NUM_HOUSE_AND_AGENTS; i++) {
                preferenceList.add(i);
            }
            Collections.shuffle(preferenceList);
            
            firstPreference = preferenceList.get(0) == currentHouse;
            
            System.out.println(this.toString());
        }

        @Override
        public void run() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
        public boolean hasPreference(){
            return firstPreference;
        }

        @Override
        public String toString() {
            return "Agent{" + "preferenceList=" + preferenceList + ", currentHouse=" + currentHouse + ", firstPreference=" + firstPreference + '}';
        }

       

    }

}
