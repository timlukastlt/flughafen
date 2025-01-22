import java.util.*;
import java.util.concurrent.Semaphore;

class Flugzeug implements Runnable {

    int flughafenNummer, id, prioität, kapzität;
    Semaphore startGate, zielGate, startLande, zielLande, luft, passagiere;

    int boardingZeit, deboardingZeit;
    int passenger_per_time;
    int flugdauer;
    int bereitstellungsDauer;

    public Flugzeug(int bereitstellungsDauer, int flughafenNummer, int id, int prioität, int kapzität, Semaphore luft, Semaphore startGate, Semaphore zielGate, Semaphore startLande, Semaphore zielLande, Semaphore passagiere, int boardingZeit, int deboardingZeit, int flugdauer) {
        this.bereitstellungsDauer = bereitstellungsDauer;
        this.flughafenNummer  = flughafenNummer;
            this.id = id;
            this.prioität = prioität;
            this.kapzität = kapzität;
            this.startGate = startGate;
            this.zielGate = zielGate;
            this.startLande = startLande;
            this.zielLande = zielLande;
            this.luft = luft;
            this.passagiere = passagiere;
            this.boardingZeit = boardingZeit;
            this.deboardingZeit = deboardingZeit;
            this.passenger_per_time = 10;
            this.flugdauer = flugdauer;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(bereitstellungsDauer);
            int passagiere_im_flugzeug = 0;
            startGate.acquire();
            int left_till_wait_boarding = passenger_per_time;
            for(int i = kapzität; i > 0; i--){
                // Boarding
                if(left_till_wait_boarding == 0){
                    Thread.sleep(boardingZeit);
                    left_till_wait_boarding = passenger_per_time;
                }
                if(!passagiere.tryAcquire()){
                    break;
                }
                passagiere_im_flugzeug++;
                left_till_wait_boarding--;
            }
            if(passagiere_im_flugzeug == 0){
                startGate.release();
                return;
            }
            luft.acquire();
            startLande.acquire();
            // Fahren zur Landebahn
            startGate.release();
            startLande.release();
            Thread.sleep(flugdauer * 100L);
            zielGate.acquire();
            zielLande.acquire();
            // Landne
            luft.release();
            zielLande.release();
            int left_till_wait_deboarding = passenger_per_time;
            for(int i = passagiere_im_flugzeug; i > 0; i--){
                // Deboarding
                if(left_till_wait_deboarding == 0){
                    Thread.sleep(deboardingZeit);
                    left_till_wait_deboarding = passenger_per_time;
                }
                left_till_wait_deboarding--;
                passagiere_im_flugzeug--;
            }
            zielGate.release();
            System.out.println("Flugzeug meldet sich ab: " + id);
            // Flugzeug verschwindet dann ohne wieder wegzufliegen :D

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Flugzeug [" + id + "] -Thread wurde unterbrochen.");
        }
    }
}



class Simulation {

    Random r;
    int boardingZeitPro10Passagiere, deboardingZeitPro10Passagiere;
    int bereitstellungsDauer, maxFlugzeugeInLuft, flugdauerZwischenFlughäfen, zeitschritteProFlugzeugGenerierung;

    Semaphore passagiereFlughafen1, passagiereFlughafen2;

    Semaphore idSema;
    int nextId;

    int zeitschritt = 0;

    List<Thread> flugzeugThreads;
    Semaphore gatesFlughafen1, gatesFlughafen2;
    Semaphore landebahnenFlughafen1, landebahnenFlughafen2;
    Semaphore luft;

    int anzahlLandebahnenFlughafen1;
    int anzahlLandebahnenFlughafen2;

    int anzahlGatesFlughafen1;
    int anzahlGatesFlughafen2;

    public Simulation(int anzahlLandebahnenFlughafen1, int anzahlLandebahnenFlughafen2, int anzahlGatesFlughafen1, int anzahlGatesFlughafen2,
                      int boardingZeitPro10Passagiere, int deboardingZeitPro10Passagiere, int bereitstellungsDauer,
                      int maxFlugzeugeInLuft, int flugdauerZwischenFlughäfen, int zeitschritteProFlugzeugGenerierung, long randomSeed) {
        this.gatesFlughafen1 = new Semaphore(anzahlGatesFlughafen1);
        this.anzahlGatesFlughafen1 = anzahlGatesFlughafen1;
        this.anzahlGatesFlughafen2 = anzahlGatesFlughafen2;
        this.gatesFlughafen2 = new Semaphore(anzahlGatesFlughafen2);
        this.landebahnenFlughafen1 = new Semaphore(anzahlLandebahnenFlughafen1);
        this.anzahlLandebahnenFlughafen1 = anzahlLandebahnenFlughafen1;
        this.anzahlLandebahnenFlughafen2 = anzahlLandebahnenFlughafen2;
        this.landebahnenFlughafen2 = new Semaphore(anzahlLandebahnenFlughafen2);
        this.boardingZeitPro10Passagiere = boardingZeitPro10Passagiere;
        this.deboardingZeitPro10Passagiere = deboardingZeitPro10Passagiere;
        this.bereitstellungsDauer = bereitstellungsDauer;
        this.maxFlugzeugeInLuft = maxFlugzeugeInLuft;
        this.flugdauerZwischenFlughäfen = flugdauerZwischenFlughäfen;
        this.zeitschritteProFlugzeugGenerierung = zeitschritteProFlugzeugGenerierung;

        this.luft = new Semaphore(3);
        this.flugzeugThreads = new ArrayList<>();
        this.r = new Random(randomSeed);

        this.nextId = 0;
        idSema = new Semaphore(1);

        this.passagiereFlughafen1 = new Semaphore(r.nextInt(1001) + 50);
        this.passagiereFlughafen2 = new Semaphore(r.nextInt(1001) + 50);
    }

    public int generateId() {
        try {
            idSema.acquire();
            this.nextId++;
            int myid = nextId;
            idSema.release();
            return myid;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void startSimulation() throws InterruptedException {
        while(passagiereFlughafen1.availablePermits() != 0 || passagiereFlughafen2.availablePermits() != 0 || gatesFlughafen1.availablePermits() < anzahlGatesFlughafen1 || gatesFlughafen2.availablePermits() < anzahlGatesFlughafen2 || luft.availablePermits() < 3 || landebahnenFlughafen2.availablePermits() < anzahlLandebahnenFlughafen2 || landebahnenFlughafen1.availablePermits() < anzahlGatesFlughafen1){

            if(passagiereFlughafen1.availablePermits() > 0 && gatesFlughafen1.availablePermits() > 0){
                int id = generateId();
                generiereFlugzeug(1, this.flugzeugThreads, id, passagiereFlughafen1);
            }
            if(passagiereFlughafen2.availablePermits() > 0 && gatesFlughafen2.availablePermits() > 0){
                int id = generateId();
                generiereFlugzeug(2, this.flugzeugThreads, id, passagiereFlughafen2);
            }
            for(int i = zeitschritteProFlugzeugGenerierung; i > 0; i--){
                Thread.sleep(100);
                zeitschritt++;
                debugInfo(zeitschritt);
            }
        }

        for (Thread t : flugzeugThreads) {
            t.join();
        }

    }

    private void generiereFlugzeug(int flughafenNummer, List<Thread> flugzeugThreads, int flugzeugid, Semaphore passagiere) {
        int prio = r.nextInt(10) + 1;
        int kapazitaet = r.nextInt(141) + 80;


            Flugzeug newFlugzeug = null;
            if(flughafenNummer == 1){
                newFlugzeug = new Flugzeug(bereitstellungsDauer, flughafenNummer, flugzeugid, prio, kapazitaet, luft, gatesFlughafen1, gatesFlughafen2, landebahnenFlughafen1, landebahnenFlughafen2, passagiere, boardingZeitPro10Passagiere, deboardingZeitPro10Passagiere, flugdauerZwischenFlughäfen);
            }else{
                newFlugzeug = new Flugzeug(bereitstellungsDauer, flughafenNummer, flugzeugid, prio, kapazitaet, luft, gatesFlughafen2, gatesFlughafen1, landebahnenFlughafen2, landebahnenFlughafen1, passagiere, boardingZeitPro10Passagiere, deboardingZeitPro10Passagiere, flugdauerZwischenFlughäfen);
            }
            Thread t = new Thread(newFlugzeug);
            // keine ahnung ob das mit der Prio so geht:
            t.setPriority(prio);
            flugzeugThreads.add(t);
            t.start();

    }

    private void debugInfo(int zeitschritt) {
        System.out.printf("Zeitschritt: %d | Passagiere Flughafen 1: %d | Passagiere Flughafen 2: %d\n",
                zeitschritt, passagiereFlughafen1.availablePermits(), passagiereFlughafen2.availablePermits());
        System.out.printf("Landebahnen Flughafen 1: %d/%d frei | Landebahnen Flughafen 2: %d/%d frei\n",
                landebahnenFlughafen1.availablePermits(), anzahlLandebahnenFlughafen1,
                landebahnenFlughafen2.availablePermits(), anzahlLandebahnenFlughafen2);
        System.out.printf("Gates Flughafen 1: %d/%d frei | Gates Flughafen 2: %d/%d frei\n",
                gatesFlughafen1.availablePermits(), anzahlGatesFlughafen1,
                gatesFlughafen2.availablePermits(), anzahlGatesFlughafen2);
        System.out.printf("Flugzeuge in der Luft: %d/%d\n",
                maxFlugzeugeInLuft - luft.availablePermits(), maxFlugzeugeInLuft);
        System.out.println();
    }

    public static void main(String[] args) throws InterruptedException {
        Simulation simulation = new Simulation(
                3, // Landebahnen Flughafen 1
                2, // Landebahnen Flughafen 2
                2, // Gates Flughafen 1
                2, // Gates Flughafen 2
                100, // Boardingzeit pro 10 Passagiere (ms)
                100, // Deboardingzeit pro 10 Passagiere (ms)
                500, // Bereitstellungsdauer (ms)
                3, // Maximale Flugzeuge in der Luft
                6, // Flugdauer zwischen den Flughäfen (Zeitschritte)
                4, // Zeitschritte pro Flugzeuggenerierung
                42 // Random-Seed
        );

        simulation.startSimulation();
    }
}
