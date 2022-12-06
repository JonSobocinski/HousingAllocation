package utexas.cockrell.distributedsystems.housingallocation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author jsobocinski
 * @author tstubbs
 */
public class Agency {

    private static final PropertyChangeSupport MESSAGE_SENDER = new PropertyChangeSupport(Agency.class.getName());

    private static final String SEND_STATUS = "SEND_STATUS";
    private static final String AVAILABLE_HOUSE_REMOVE = "AVAILABLE_HOUSE_REMOVE";
    private static final String NOTIFY_CHILDREN_OF_CYCLE = "NOTIFY_CHILDREN_OF_CYCLE";
    private static final String COPY_GRAPH = "COPY_GRAPH";

    private static final String OK = "OK";
    private static final String NEXT_STAGE = "NEXT_STAGE";
    private static final String REMOVE = "REMOVE";

    private static boolean init = false;
    private static Agency agency;
    private static Random random = new Random();

    private static AtomicInteger coloringCount = new AtomicInteger(1);

    CopyOnWriteArrayList<Agent> agentList = new CopyOnWriteArrayList<>();
    CopyOnWriteArrayList<Integer> availableHouses = new CopyOnWriteArrayList<>();

    CopyOnWriteArrayList<Thread> listOfTasks = new CopyOnWriteArrayList<>();

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

        //Now we need to create the graph by setting each Agent's connectedAgent, to the one that matches
        for (Agent agent : agentList) {
            for (Agent searchAgent : agentList) {
                if (agent.preferenceList.get(0) == searchAgent.currentHouse) {
                    agent.connectedAgent = searchAgent;
                    continue;
                }
            }
        }

        //After we've created the graph, send a reference to the full agent list to each agent just for bookkeeping purposes 
        //(makes certain parts of future algo implementations easier)
        MESSAGE_SENDER.firePropertyChange(COPY_GRAPH, null, agentList);

        for (Agent ag : agentList) {
            distibutedTTC(ag, Main.DEBUG);
        }

    }

    public void sendStatus() throws InterruptedException {
        for (Thread t : listOfTasks) {
            t.join();
        }
        for (Agent a : agentList) {
            System.out.println(a.toString());
        }
    }

    private void distibutedTTC(Agent p, boolean debug) {

        if (Main.USE_LAS_VEGAS) {
            lasVegasTTC(p, Main.DEBUG);
        } else {
            coloringTTC(p, Main.DEBUG);
        }

        if (p.inCycle) {
            System.out.println("Updating current house from: " + p.currentHouse + " to " + p.connectedAgent.currentHouse);
            p.currentHouse = p.connectedAgent.currentHouse;
            p.assigned = true;
            MESSAGE_SENDER.firePropertyChange(REMOVE, null, p.currentHouse);

            if (p.children.isEmpty()) {
                for (Agent parentAgent : p.parents) {
                    Thread t = new Thread(parentAgent.getOkRunnable());
                    listOfTasks.add(t);
                    t.start();
                }
            }

        }
    }

    public static Agency getInstance() {
        if (!init) {
            agency = new Agency();
            init = true;
        }
        return agency;
    }

    /**
     * Algorithm Par-VertexColoring: A Parallel Algorithm for Vertex Coloring in
     * a Ring 1 input: n nodes arranged in a cycle such that next[i] gives the
     * next node for node i 2 A coloring c of vertices such that for all i: c[i]
     * di↵ers form c[next[i]] 3 output: Another valid coloring d with fewer
     * colors 4 forall nodes i in parallel do 5 let k be the least significant
     * bit such that c[i] and c[next[i]] di↵er 6 d[i] := 2 ⇤ k+ the kth least
     * significant bit in c[i
     *
     * @param agent
     * @param debug
     */
    public void coloringTTC(Agent agent, boolean debug) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                agent.color = 2 * 1 + coloringCount.getAndIncrement();
            }
        });
        t.start();
    }

    public void lasVegasTTC(Agent agent, boolean debug) {
        while (agent.isActive) {

            agent.flipMyCoin();
            agent.connectedAgent.flipMyCoin();

            if (agent.myCoin == Coin.Heads && agent.connectedAgent.myCoin == Coin.Tails) {
                agent.isActive = false;
            }

            //EXPLORE STEP
            if (agent.isActive) {
                System.out.println("Agent " + agent.currentHouse + " is active");

                agent.connectedAgent.connectedAgent.flipMyCoin();

                if (agent.connectedAgent.myCoin == Coin.Heads && agent.connectedAgent.connectedAgent.myCoin == Coin.Tails) {
                    agent.connectedAgent.isActive = false;
                }

                boolean succActive = agent.connectedAgent.isActive;
                //build a tree of children and look for a cycle
                while (succActive == false && agent.connectedAgent.currentHouse != agent.connectedAgent.connectedAgent.currentHouse) {
                    System.out.println("Connected Agent " + agent.connectedAgent.currentHouse + " is active");

                    if (!agent.children.contains(agent.connectedAgent)) {
                        agent.children.add(agent.connectedAgent);//add this connected agent to the agent's children list
                        agent.connectedAgent.parents.add(agent);
                    }
                    agent.connectedAgent = agent.connectedAgent.connectedAgent;//move to the next agent in the tree

                    agent.connectedAgent.flipMyCoin();
                    agent.connectedAgent.connectedAgent.flipMyCoin();

                    if (agent.connectedAgent.myCoin == Coin.Heads && agent.connectedAgent.connectedAgent.myCoin == Coin.Tails) {
                        agent.connectedAgent.isActive = false;
                    } else {
                        agent.connectedAgent.isActive = true;
                    }

                    succActive = agent.connectedAgent.isActive;
                }
                //if the tree has come back around to the agent, a cycle has been found
                if (agent.connectedAgent == agent) {
                    agent.isActive = false;
                }
            }
        }

        if (debug) {
            System.out.println("Agent and children");
            System.out.println(agent.currentHouse);
            int count = 0;
            for (Agent child : agent.children) {
                count++;
                System.out.println("child " + count + ": " + child.currentHouse);
            }
        }

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

    public class Agent implements PropertyChangeListener {

        private Agent connectedAgent;
        private CopyOnWriteArrayList<Agent> parents = new CopyOnWriteArrayList<>();
        private CopyOnWriteArrayList<Agent> children = new CopyOnWriteArrayList<>();
        private CopyOnWriteArrayList<Agent> agentList;

        private final LinkedList<Integer> preferenceList;
        private int currentHouse;

        private boolean firstPreference = false;
        private boolean isActive = true;
        private boolean inCycle = false;

        private boolean assigned = false;
        private Agent succesor;

        Coin myCoin;
        int color;

        private Agent(int currentHouse) {
            this.currentHouse = currentHouse;

            preferenceList = new LinkedList<>();

            for (Integer i : availableHouses) {
                preferenceList.add(i);
            }
            Collections.shuffle(preferenceList);

            firstPreference = preferenceList.get(0) == currentHouse;

            flipMyCoin();

        }

        public void connectedAgentInfo() {
            System.out.println("Agent: " + this.currentHouse);
            System.out.println("Agent connected agent: " + this.connectedAgent.currentHouse + "\n");
        }

        private void flipMyCoin() {
            myCoin = random.nextBoolean() ? Coin.Heads : Coin.Tails;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(AVAILABLE_HOUSE_REMOVE)) {
                int houseToRemove = (int) evt.getNewValue();
//                System.out.println("Removing " + houseToRemove + " from " + currentHouse);
                preferenceList.remove(houseToRemove);
            } else if (evt.getPropertyName().equals(SEND_STATUS)) {
                this.toString();
            } else if (evt.getPropertyName().equals(NOTIFY_CHILDREN_OF_CYCLE)) {
                Agent agentToNotify = (Agent) evt.getNewValue();
                if (agentToNotify.equals(this)) {
                    System.out.println("Agent " + currentHouse + " is in a cycle!");
                    inCycle = true;
                    for (Agent child : agentToNotify.children) {
                        System.out.println("\tNotifying child: " + child.currentHouse);
                        MESSAGE_SENDER.firePropertyChange(NOTIFY_CHILDREN_OF_CYCLE, null, child);
                    }
                }
            } else if (evt.getPropertyName().equals(COPY_GRAPH)) {
                agentList = (CopyOnWriteArrayList<Agent>) evt.getNewValue();
            } else if (evt.getPropertyName().equals(OK)) {
                Thread t = new Thread(getOkRunnable());
                listOfTasks.add(t);
                t.start();
            } else if (evt.getPropertyName().equals(NEXT_STAGE)) {
                Thread t = new Thread(getNextStageRunnable());
                listOfTasks.add(t);
                t.start();
            } else if (evt.getPropertyName().equals(REMOVE)) {
                Thread t = new Thread(getRemoveRunnable((int) evt.getNewValue()));
                listOfTasks.add(t);
                t.start();
            }
        }

        @Override
        public String toString() {
            return "Agent{" + "preferenceList=" + preferenceList + ", currentHouse=" + currentHouse + ", firstPreference=" + firstPreference + ", myCoin=" + myCoin + '}';
        }

        public String status() {
            return "Agent: " + currentHouse + "\n"
                    + "\tPrefered House: " + preferenceList.get(0) + "\n"
                    + "\tpreferenceListSize: " + preferenceList.size() + "\n"
                    + "\tconnectedAgent: " + connectedAgent.currentHouse;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Agent other = (Agent) obj;
            if (this.currentHouse != other.currentHouse) {
                return false;
            }
            if (this.firstPreference != other.firstPreference) {
                return false;
            }
            if (this.isActive != other.isActive) {
                return false;
            }
            if (this.inCycle != other.inCycle) {
                return false;
            }
            if (this.assigned != other.assigned) {
                return false;
            }

            if (this.color != other.color) {
                return false;
            }
            if (!Objects.equals(this.connectedAgent, other.connectedAgent)) {
                return false;
            }
            if (!Objects.equals(this.parents, other.parents)) {
                return false;
            }
            if (!Objects.equals(this.children, other.children)) {
                return false;
            }
            if (!Objects.equals(this.agentList, other.agentList)) {
                return false;
            }
            if (!Objects.equals(this.preferenceList, other.preferenceList)) {
                return false;
            }
            if (!Objects.equals(this.succesor, other.succesor)) {
                return false;
            }
            if (this.myCoin != other.myCoin) {
                return false;
            }
            return true;
        }

        private Runnable getOkRunnable() {
            Agent agent = this;
            return new Runnable() {
                @Override
                public void run() {
                    if (agent.parents.isEmpty()) {//This agent is a root node
                        MESSAGE_SENDER.firePropertyChange(NEXT_STAGE, null, null);
                    } else {
                        for (Agent parentAgent : agent.parents) {
                            Thread t = new Thread(parentAgent.getOkRunnable());
                            listOfTasks.add(t);
                            t.start();
                        }
                    }
                }
            };
        }

        private Runnable getNextStageRunnable() {
            Agent agent = this;
            return new Runnable() {
                @Override
                public void run() {
                    if (!agent.assigned) {
                        agent.isActive = true;
                        int nextChoice = agent.preferenceList.pop();
                        for (Agent agentToSearchThrough : agentList) {
                            if (agentToSearchThrough.currentHouse == nextChoice) {
                                agent.connectedAgent = agentToSearchThrough;
                                break;
                            }
                        }
                        distibutedTTC(agent, Main.DEBUG);
                    }
                }
            };
        }

        private Runnable getRemoveRunnable(int removal) {
            Agent agent = this;
            return new Runnable() {
                @Override
                public void run() {
                    int index = -1;
                    for (int i = 0; i < agent.preferenceList.size(); i++) {
                        index = agent.preferenceList.get(i) == removal ? i : -1;
                    }

                    if (index != -1) {
                        agent.preferenceList.remove(index);
                    }
                }
            };
        }

    }

}
