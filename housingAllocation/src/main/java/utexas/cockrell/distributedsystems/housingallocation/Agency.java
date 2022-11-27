/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utexas.cockrell.distributedsystems.housingallocation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author jsobocinski
 */
public class Agency {

    private static final PropertyChangeSupport MESSAGE_SENDER = new PropertyChangeSupport(Agency.class.getName());

    private static final String SEND_STATUS = "SEND_STATUS";
    private static final String AVAILABLE_HOUSE_REMOVE = "AVAILABLE_HOUSE_REMOVE";
    private static final String NOTIFY_CHILDREN_OF_CYCLE = "NOTIFY_CHILDREN_OF_CYCLE";

    private static boolean init = false;
    private static Agency agency;
    private static Random random = new Random();

    CopyOnWriteArrayList<Agent> agentList = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<Integer> availableHouses = new CopyOnWriteArrayList<>();

    private Agency() {
        //Create a list of available houses
        for (int i = 0; i < Main.NUM_HOUSE_AND_AGENTS; i++) {
            availableHouses.add(i);
        }
        Collections.shuffle(availableHouses);

        for (int i = 0; i < Main.NUM_HOUSE_AND_AGENTS; i++) {
            Agent agent = new Agent(i);
            MESSAGE_SENDER.addPropertyChangeListener(agent);
            agentList.add(agent);
            System.out.println(agent);

//            removeAgentWithFirstPreference(agent); For now, we don't remove agents that have their first preference
        }

        //Now we need to create the graph but setting each Agent's connectedAgent, to the one that matches
        for (Agent agent : agentList) {
            for (Agent searchAgent : agentList) {
                if (agent.preferenceList.get(0) == searchAgent.currentHouse) {
                    agent.connectedAgent = searchAgent;
                    continue;
                }
            }
        }

        for (Agent ag: agentList) {
            lasVegasTTC(ag);
        }
        //lasVegasTTC(agentList.get(0));

        MESSAGE_SENDER.firePropertyChange(SEND_STATUS, null, null);

    }

    public static Agency getInstance() {
        if (!init) {
            agency = new Agency();
            init = true;
        }
        return agency;
    }

    public void lasVegasTTC(Agent agent) {
        while (agent.isActive) {
            //COIN FLIP STEP

            //Flip coin for current agent
            //Random random = new Random();
            int myflip = random.nextInt(2);
            if (myflip == 0) {
                agent.myCoin = Coin.Heads;
            } else if (myflip == 1) {
                agent.myCoin = Coin.Tails;
            }

            //Flip coin for successor
            int succflip = random.nextInt(2);
            if (succflip == 0) {
                agent.connectedAgent.myCoin = Coin.Heads;
            } else if (succflip == 1) {
                agent.connectedAgent.myCoin = Coin.Tails;
            }

            if (agent.myCoin == Coin.Heads && agent.connectedAgent.myCoin == Coin.Tails) {
                agent.isActive = false;
            }

            //EXPLORE STEP
            if (agent.isActive == true) {
                
                //perform another coin flip to get the active status of the connected agent
                succflip = random.nextInt(2);
                if (succflip == 0) {
                    agent.connectedAgent.connectedAgent.myCoin = Coin.Heads;
                } else if (succflip == 1) {
                    agent.connectedAgent.connectedAgent.myCoin = Coin.Tails;
                }
                if (agent.connectedAgent.myCoin == Coin.Heads && agent.connectedAgent.connectedAgent.myCoin == Coin.Tails) {
                    agent.connectedAgent.isActive = false;
                }
                
                boolean succActive = agent.connectedAgent.isActive;
                //build a tree of children and look for a cycle
                while (succActive == false) {
                    if (agent.children.contains(agent.connectedAgent)) {
                        break;
                    }
                    agent.children.add(agent.connectedAgent);       //add this connected agent to the agent's children list
                    agent.connectedAgent = agent.connectedAgent.connectedAgent;     //move to the next agent in the tree
                    
                    //perform another coin flip to get the active status of the new connected agent
                    succflip = random.nextInt(2);
                    if (succflip == 0) {
                        agent.connectedAgent.connectedAgent.myCoin = Coin.Heads;
                    } else if (succflip == 1) {
                        agent.connectedAgent.connectedAgent.myCoin = Coin.Tails;
                    }
                    if (agent.connectedAgent.myCoin == Coin.Heads && agent.connectedAgent.connectedAgent.myCoin == Coin.Tails) {
                        agent.connectedAgent.isActive = false;
                    }
                    
                    succActive = agent.connectedAgent.isActive;                    
                }

                //if the tree has come back around to the agent, a cycle has been found
                if (agent.connectedAgent == agent) {
                    agent.isActive = false;
                }
            }
        }
        
//        System.out.println("Agent and children");
//        System.out.println(agent.currentHouse);
//        int count = 0;
//        for (Agent child : agent.children) {
//            count++;
//            System.out.println("child " + count + ": " + child.currentHouse);
//        }

        //NOTIFY STEP
        if (agent.connectedAgent == agent) {
            //send "cycle" to children
            agent.inCycle = true;
            for (Agent child : agent.children) {
                MESSAGE_SENDER.firePropertyChange(NOTIFY_CHILDREN_OF_CYCLE, null, child);
            }

        }

    }

    private void removeAgentWithFirstPreference(Agent agent) {
        //If the agent has been matched with his first preference on creation, remove it from available houses;
        if (agent.firstPreference) {
            MESSAGE_SENDER.firePropertyChange(AVAILABLE_HOUSE_REMOVE, null, agent.currentHouse);
            availableHouses.remove(agent.currentHouse);
            agentList.remove(agent);

            agent.isActive = false;
        }
    }

    public enum Coin {
        Heads, Tails
    };

    public class Agent implements Runnable, PropertyChangeListener {

        private Agent connectedAgent;
        private ArrayList<Agent> children = new ArrayList<>();

        private final List<Integer> preferenceList;
        private int currentHouse;

        private boolean firstPreference = false;
        private boolean isActive = true;
        private boolean inCycle = false;
        Coin myCoin;

        private Agent(int currentHouse) {
            this.currentHouse = currentHouse;

            preferenceList = new ArrayList<>();

            for (Integer i : availableHouses) {
                preferenceList.add(i);
            }
            Collections.shuffle(preferenceList);

            firstPreference = preferenceList.get(0) == currentHouse;

            //lasVegasTTC();
        }

        @Override
        public void run() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(AVAILABLE_HOUSE_REMOVE)) {
                int houseToRemove = (int) evt.getNewValue();
//                System.out.println("Removing " + houseToRemove + " from " + currentHouse);
                preferenceList.remove(houseToRemove);
            } else if (evt.getPropertyName().equals(SEND_STATUS)) {
                if (isActive) {
                    System.out.println(status());
                }
            } else if (evt.getPropertyName().equals(NOTIFY_CHILDREN_OF_CYCLE)) {
                Agent agentToNotify = (Agent) evt.getNewValue();
                if (agentToNotify.equals(this)) {
                    System.out.println("Agent " + currentHouse + " is in a cycle!");
                    inCycle = true;
                    for (Agent child : agentToNotify.children) {
                        MESSAGE_SENDER.firePropertyChange(NOTIFY_CHILDREN_OF_CYCLE, null, child);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "Agent{" + "preferenceList=" + preferenceList + ", currentHouse=" + currentHouse + ", firstPreference=" + firstPreference + '}';
        }

        public String status() {
            return "Agent: " + currentHouse + "\n"
                    + "\tPrefered House: " + preferenceList.get(0) + "\n"
                    + "\tpreferenceListSize: " + preferenceList.size() + "\n"
                    + "\tconnectedAgent: " + connectedAgent.currentHouse;
        }

    }

}
