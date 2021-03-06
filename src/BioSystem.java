import java.util.ArrayList;
import java.util.Random;

public class BioSystem {

    //no. of habitats, "carrying capacity", no. of nutrients present initially in each habitat
    private int L, K, s, s_max;

    private double c, timeElapsed;

    private boolean populationDead = false;

    private Microhabitat[] microhabitats;

    Random rand = new Random();

    public BioSystem(int L, int K, int S, double c){

        this.L = L;
        this.K = K;
        this.s = S;
        this.s_max = S;
        this.c = c;

        this.microhabitats = new Microhabitat[L];
        this.timeElapsed = 0.;

        for(int i = 0; i < L; i++){
            microhabitats[i] = new Microhabitat(K, c, S);
        }
        microhabitats[0].fillWithWildType();
    }

    public double getL(){return L;}
    public double getTimeElapsed(){return timeElapsed;}
    public boolean getPopulationDead(){return populationDead;}

    public int getCurrentPopulation(){
        int runningTotal = 0;

        for(int i = 0; i < L; i++){
            runningTotal += microhabitats[i].getN();
        }
        return runningTotal;
    }


    public Microhabitat getMicrohabitat(int i){
        return microhabitats[i];
    }

    public Bacteria getBacteria(int l, int k){
        return microhabitats[l].getBacteria(k);
    }

    public void migrate(int currentL, int bacteriumIndex){

        if(currentL < (L-1)){

            ArrayList<Bacteria> source = microhabitats[currentL].getPopulation();
            ArrayList<Bacteria> destination = microhabitats[currentL + 1].getPopulation();

            destination.add(source.remove(bacteriumIndex));
        }
    }

    public void die(int currentL, int bacteriumIndex){

        microhabitats[currentL].removeABacterium(bacteriumIndex);
        if(getCurrentPopulation() == 0) populationDead = true;
    }


    public void replicate(int currentL, int bacteriumIndex){
        //a nutrient unit is consumed for every replication
        microhabitats[currentL].consumeNutrients();
        //the bacterium which is going to be replicated and its associated properties
        Bacteria parentBac = microhabitats[currentL].getBacteria(bacteriumIndex);
        int m = parentBac.getM();

        Bacteria childBac = new Bacteria(m);

        microhabitats[currentL].addABacterium(childBac);
    }


    public void performAction(){

        //selects a random bacteria from the total population
        if(!populationDead) {

            int randomIndex = rand.nextInt(getCurrentPopulation());
            int indexCounter = 0;
            int microHabIndex = 0;
            int bacteriaIndex = 0;

            forloop:
            for(int i = 0; i < getL(); i++) {

                if((indexCounter + microhabitats[i].getN()) <= randomIndex) {

                    indexCounter += microhabitats[i].getN();
                    continue forloop;
                } else {
                    microHabIndex = i;
                    bacteriaIndex = randomIndex - indexCounter;
                    break forloop;
                }
            }

            Microhabitat randMicroHab = microhabitats[microHabIndex];

            int s = randMicroHab.getS(), s_max = randMicroHab.getS_max();
            double K_prime = randMicroHab.getK_prime(), c = randMicroHab.getC();
            Bacteria randBac = randMicroHab.getBacteria(bacteriaIndex);

            double migRate = randBac.getB();
            double deaRate = randBac.getD();
            double repliRate = randBac.replicationRate(c, s, s_max, K_prime);
            double R_max = 1.2;
            double rando = rand.nextDouble()*R_max;

            if(rando < migRate) migrate(microHabIndex, bacteriaIndex);
            else if(rando >= migRate && rando < (migRate + deaRate)) die(microHabIndex, bacteriaIndex);
            else if(rando >= (migRate + deaRate) && rando < (migRate + deaRate + repliRate))
                replicate(microHabIndex, bacteriaIndex);

            timeElapsed += 1./((double)getCurrentPopulation()*R_max);
            //move this to the death() method

        }
    }



    public static void antibioticVsNutrients(){

        int nPoints = 10, nReps = 5;
        int L = 500, K = 100;
        double duration = 500.;
        String filename = "fastGrowers_nutrients_vs_antibiotic";

        ArrayList<Double> sVals = new ArrayList<Double>();
        ArrayList<Double> cVals = new ArrayList<Double>();
        ArrayList<Double> popVals = new ArrayList<Double>();
        ArrayList<Double> maxPopVals = new ArrayList<Double>();

        int initS = 10, finalS = 1000;
        int sIncrement = (int)((finalS - initS)/(double)nPoints);

        double initC = 0., finalC = 10, zerothC = 0.;
        double cIncrement = (finalC - initC)/(double)nPoints;

        for(int s = initS; s<=finalS; s+=sIncrement){

            double avgMaxPopulation = 0.;

            for(int r = 0; r < nReps; r++){
                BioSystem bs = new BioSystem(L, K, s, zerothC);

                while(bs.getTimeElapsed() <= duration && !bs.getPopulationDead()) bs.performAction();
                avgMaxPopulation += bs.getCurrentPopulation();
            }
            maxPopVals.add(avgMaxPopulation/(double)nReps);
        }

        int indexCounter = 0;
        for(int s = initS; s <= finalS; s += sIncrement){
            sVals.add((double)s);

            for(double c = initC; c <= finalC; c += cIncrement){
                cVals.add(c);

                double avgMaxPopulation = 0.;

                for(int r = 0; r < nReps; r++){
                    BioSystem bs = new BioSystem(L, K, s, c);

                    while(bs.getTimeElapsed() <= duration && !bs.getPopulationDead()){
                        bs.performAction();
                    }

                    avgMaxPopulation+=bs.getCurrentPopulation();
                    System.out.println(bs.getCurrentPopulation());
                    System.out.println("sVal: "+s+"\t cVal: "+ c+"\t rep: "+r);
                }

                popVals.add(avgMaxPopulation/((double)nReps*maxPopVals.get(indexCounter)));
            }
            indexCounter++;
        }
        Toolbox.writeContoursToFile(sVals, cVals, popVals, filename);
    }
}