package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat{
    static BoatGrader bg;

    private static boolean isBoatInOahu, childLandedInMolokai;
    private static int oahuAdults, oahuChildren, molokaiAdults, molokaiChildren, childrenReadyToGo; 
    private static Lock lockOahu, lockMolokai;
    private static Condition2 childPilotsBackToOahu;
    private static Condition2 childrenDonatingTheirPlace;
    private static Condition2 childWaitingForPartnerInMolokai;
    private static Condition2 adultsWaitingInOahu;

    public static void selfTest(int a, int c){
		BoatGrader b = new BoatGrader();
		
	//	System.out.println("\n ***Testing Boats with only 2 children***");
	//	begin(0, 2, b);

	//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
	//  	begin(1, 2, b);

		System.out.println("\n ***Testing Boats with " + c + " children and" + a +" adults***");
  		begin(a, c, b);
    }

    private static void displaySituation(){
    	System.out.println("\n Children still in Oahu: " + oahuChildren + " --- Children in Molokai: " + molokaiChildren);
		System.out.println(" Adults still in Oahu: " + oahuAdults + "  ---  Adults in Molokai: " + molokaiAdults+"\n");
    }

    public static void begin(int adults, int children, BoatGrader b ){
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
		bg = b;

	// Instantiate global variables here
    	isBoatInOahu = true;
    	childLandedInMolokai = false;
    	oahuAdults = adults;
    	oahuChildren = children;
    	molokaiChildren = molokaiAdults = 0;
    	lockOahu = new Lock();
    	lockMolokai = new Lock();
		childPilotsBackToOahu = new Condition2(lockMolokai);
		childrenDonatingTheirPlace = new Condition2(lockOahu);
    	childWaitingForPartnerInMolokai = new Condition2(lockOahu);
    	adultsWaitingInOahu = new Condition2(lockOahu);

    	displaySituation();

	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.
    	
    	for (int numChild = 1; numChild <= children; numChild++)
    		new KThread(new Runnable(){
    			public void run(){ 
    				ChildItinerary(); 
    			}
    			}).setName("Child #"+numChild).fork();

    	for (int numAdults = 1; numAdults <= adults; numAdults++)
    		new KThread(new Runnable(){
    			public void run(){ 
    				AdultItinerary(); 
    			}
    			}).setName("Adult #"+numAdults).fork();


    	KThread.waitUntilSeconds(4);
    	displaySituation();



		/*
		Runnable r = new Runnable() {
		    public void run() {
	                SampleItinerary();
	            }
	        };
	        KThread t = new KThread(r);
	        t.setName("Sample Boat Thread");
	        t.fork();

		*/
    }

    static void AdultItinerary(){
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
	 	lockOahu.acquire();
		while (!isBoatInOahu || oahuChildren >= 2) {
		    adultsWaitingInOahu.sleep();
		}
		oahuAdults--;
		isBoatInOahu = false;
		lockOahu.release();
		bg.AdultRowToMolokai();
		lockMolokai.acquire();
		molokaiAdults++;
		childPilotsBackToOahu.wake();
		lockMolokai.release();
    }

    static void ChildItinerary(){
    	while(oahuChildren + oahuAdults >= 2){
    		lockOahu.acquire();
    		if (oahuChildren == 1)
    			adultsWaitingInOahu.wake();
    		
    		while(!isBoatInOahu || childrenReadyToGo >= 2)
    			childrenDonatingTheirPlace.sleep();

    		if (childrenReadyToGo == 0){
    			childrenReadyToGo++;
    			childrenDonatingTheirPlace.wake();
    			childWaitingForPartnerInMolokai.sleep();
    			bg.ChildRideToMolokai();
    			childWaitingForPartnerInMolokai.wake();
    		}
    		else{
    			childrenReadyToGo++;
    			childWaitingForPartnerInMolokai.wake();
    			bg.ChildRowToMolokai();
    			childWaitingForPartnerInMolokai.sleep();
    		}
    		childrenReadyToGo--;
    		oahuChildren--;
    		isBoatInOahu = false;
    		lockOahu.release();
    		lockMolokai.acquire();
    		molokaiChildren++;
    		if (!childLandedInMolokai){
				childLandedInMolokai = true;
    			childPilotsBackToOahu.sleep();
    		}
    		childLandedInMolokai = false;
    		molokaiChildren--;
    		lockMolokai.release();
    		bg.ChildRowToOahu();
    		lockOahu.acquire();
    		isBoatInOahu = true;
    		oahuChildren++;
    		lockOahu.release();
    	}

    	lockOahu.acquire();
		oahuChildren--;
		lockOahu.release();
		bg.ChildRowToMolokai();
		lockMolokai.acquire();
		molokaiChildren++;
		lockMolokai.release();
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
