import java.util.*;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;

class Request implements Comparable<Request>{
    String direction;                          //Direction for request (i.e. down or up) 
    double time;                               //person's time or request
    int floor;                                 //which floor the elevator needs to go
    Person person;
    public Request(String dir, double time_, int floor_, Person person_){
	direction = dir;
	time      = time_;                                              //Time in Seconds
	floor     = floor_;
	person    = person_;
    }
    //overide compareTo
    public int compareTo(Request r){
	return (int)(this.time - r.time);
    }
}
//Floor sorted in ascending order
class floorComparatorAsc implements Comparator<Request>{
    public int compare(Request r1, Request r2){
	return r1.floor - r2.floor;
    }
}

//Floor sorted in descending order
class floorComparatorDesc implements Comparator<Request>{
    public int compare(Request r1, Request r2){
	return r2.floor - r1.floor;
    }
}

class Stats{
    public double res_time_up   = 0.0;
    public double res_time_down = 0.0;
    public int count            = 0;
}

class Person{
    public double arrival;
    public double departure;
    public int floor;
    public double work_till;
    public int id;
    public double res_time_up;                //response time going up
    public double res_time_down;              //response time going down
   
    
    public Person(double arr, int fl, double wrk, int id_){
	arrival       = arr;
	departure     = 0.0;
	floor         = fl;
	work_till     = wrk;
	id            = id_;
	res_time_up   = 0.0;
	res_time_down = 0.0;
    }
    
}
public class Elevator{

    private static final Random R1        = new Random(12345);
    private static final Random R2        = new Random(54321);
    private static final Random R3        = new Random(123456789);
    private static final double MAX_VALUE = 9999999.0;
    private static final int MAX_QUEUE    = 9999999;
    private static final int NUM_ARR      = 15000;
   


    
    public static void main(String [] args){
	double arrival_lambda   = 0.1;
	double service_lambda   = 1/60.0;
	int num_floor           = 7;
	int elevator_floor      = 0;
	int next_event          = 0;
	int id                  = 0;
	int index               = 0;
	int STOP                = 30;


	double current_time     = 0.0;
	double arrival          = 0.0;
	double departure        = MAX_VALUE;

	//Debugs
	boolean debug           = false;           //debug for elevator 
	boolean debug1          = false;            //debug for general queue
	boolean debug2          = true;          // For generating stats
	boolean debug3          = false;         // Stats for floor
       
	    
	//ELEVATOR STATE
	boolean IDLE            = true;

	//Stats
	Stats stats = new Stats();
	
	//Set elevator's queue
	ArrayList<Request> queue = new ArrayList<Request>();
		  

	ArrayList<ArrayList<Request>> genReq  = new ArrayList<ArrayList<Request>>(num_floor);
	//Initialize the general queue
	for(int i = 0; i < num_floor; i++){
	    genReq.add(i,new ArrayList<Request>());
	}
	
	genReq = gen_arrival(arrival_lambda,service_lambda,genReq,num_floor);
	
	
	current_time   = genReq.get(0).get(0).person.arrival;

	if(debug1)
	    System.out.println("Current time: " + current_time);

	next_event = 0;
	
	if(debug1)
	    printBuilding(genReq);

	//Scanner inp = new Scanner(System.in);
	String schedule         = "FCFS";
	String idle_behavior    = "Stay";

	//System.out.print("Choose what Algorithm to use (i.e. FCFS or Linear): ");
	schedule = args[0];
	//System.out.print("Choose what idle behavior for the elevator (i.e. Stay or Bottom): ");
	idle_behavior = args[1];

	//inp.close();

	if(schedule.compareToIgnoreCase("Linear") == 0){
	    while(index < NUM_ARR){
		if(IDLE){
		    //Advance the time to the next imminent event + floor the elevator is at
		    if(index < NUM_ARR - 1){
			if(idle_behavior.compareToIgnoreCase("Stay") == 0){
			    //Time to go up to the next event
			    current_time = ele_time_traveled(current_time,Math.abs(elevator_floor - next_event));
			    if(current_time < ele_time_traveled(genReq.get(next_event).get(0).time,Math.abs(elevator_floor - next_event)))
				current_time = ele_time_traveled(genReq.get(next_event).get(0).time,Math.abs(elevator_floor - next_event));
			}
			//Bottom
			else{
			    //Time to go down
			    current_time = ele_time_traveled(current_time,elevator_floor);
			    //Time to go up to the next event
			    current_time = ele_time_traveled(current_time,next_event);
			    if(current_time < ele_time_traveled(genReq.get(next_event).get(0).time,Math.abs(elevator_floor - next_event))){
				current_time = genReq.get(next_event).get(0).time;
				current_time = ele_time_traveled(current_time,next_event);
			    }
			    
			}
		    }
		    else{
			break;
		    }
		    IDLE = false;
		}
		else{
		    
		    //if arrival, update the person info for stats
		    if(next_event == 0){
			
			
			//Add all people on the general queue based on the current time
			queue.addAll(getElementsQueue(genReq,0,current_time));
			Collections.sort(queue,new floorComparatorAsc());
			
			if(debug)
			    printElevator(queue);
			
			
			//Update the general queue
			genReq = updateQueue(genReq,0,current_time);
			
			
			int ele_size = queue.size(); 
			
			//Loop until finish servicing all arrivals
			for(int i = 0; i < ele_size; i++){
			    
			    Request dep_req = queue.remove(0);
			    //Set response time
			    if(i > 0){
				dep_req.person.res_time_up = ele_time_traveled(current_time,Math.abs(elevator_floor - dep_req.floor));
			    }
			    else{
				dep_req.person.res_time_up = ele_time_traveled(current_time,dep_req.floor);
			    }
			    
			    if(debug1)
				System.out.println("Arrival Response Time: " + dep_req.person.res_time_up + " Floor number: " +dep_req.person.floor);
			    
			    
			    //Assign departure 
			    dep_req.direction = "down";
			    dep_req.time = dep_req.person.res_time_up + dep_req.person.work_till;
			    dep_req.floor = 0;
			    dep_req.person = dep_req.person;
			    
			    ArrayList<Request> dep = genReq.get(dep_req.person.floor);
			    dep.add(dep_req);
			    Collections.sort(dep);
			    genReq.set(dep_req.person.floor,dep);
			    
			    //update current time
			    current_time = dep_req.person.res_time_up;
			    
			    //update elevator floor
			    elevator_floor = dep_req.person.floor;
			    
			    if(debug1)
				System.out.println("Current Time: "+current_time+" Elevator current floor " + elevator_floor);
			}
			
			IDLE = true;
			
			if(debug1)
			    printBuilding(genReq);
			
		    }
		    //else departure, update the person info for stats
		    else{
			//Add all people on the floor on that genral queue based on the current time
			queue.addAll(getElementsQueue(genReq,next_event,current_time));
			
			if(debug1)
			    System.out.println("Current Time: " + current_time);
			
			
			//Remove all queued up person on that floor
			genReq = updateQueue(genReq,next_event,current_time);
			
			int which_floor = queue.get(0).person.floor;
			
			//This for loop is for checking each floor along the way
			for(int i = which_floor - 1; i > 0; i--){
			    current_time = current_time + 10;
			    queue.addAll(0,getElementsQueue(genReq,i,current_time));
			    genReq = updateQueue(genReq,i,current_time);
			}
			
			if(debug)
			    printElevator(queue);
			//update current_time when it goes to floor 0
			current_time = current_time + 10;
			
			int ele_size = queue.size();
			
			for(int i = 0; i < ele_size; i++){
			    Request dep_req = queue.remove(0);
			    
			    //Assigned reponse time for down
			    dep_req.person.res_time_down = dep_req.time;
			    
			    //Assigned departure time
			    dep_req.person.departure = current_time;

			    if(debug1){
				System.out.println("Departure Time: " + dep_req.person.departure + " Was in Floor number " + dep_req.person.floor);
			    }
			    
			    index++;
			    //Put prints for stats
			    
			    if(debug2){
				double timeUp    = 0.0;
				double timeDown  = 0.0;
				
				timeUp           = dep_req.person.res_time_up - dep_req.person.arrival;
				timeDown         = dep_req.person.departure - dep_req.person.res_time_down;

				stats.res_time_up   += timeUp;
				stats.res_time_down += timeDown;
				
				//System.out.println(timeUp + " " + timeDown + " ID " + dep_req.person.id);
			    }
			}
			
			IDLE = true;
			elevator_floor = 0;
			
			if(debug1)
			    printBuilding(genReq);
			
			
		    }
		    next_event = next_event(genReq,num_floor);
		} 
	    }
	     if(debug2){
		System.out.println("Average up   "+ idle_behavior +" response time " + stats.res_time_up / index);
		System.out.println("Average down "+ idle_behavior +" response time " + stats.res_time_down / index);
		}
	}
	else if(schedule.compareToIgnoreCase("FCFS") == 0){
	    while(index < NUM_ARR){
		if(IDLE){
		    //Advance the time to the next imminent event + floor the elevator is at
		    if(index < NUM_ARR - 1)
			if(idle_behavior.compareToIgnoreCase("Stay") == 0){
			    //Time to go up to the next event
			    current_time = ele_time_traveled(current_time,Math.abs(elevator_floor - next_event));
			    if(current_time < ele_time_traveled(genReq.get(next_event).get(0).time,Math.abs(elevator_floor - next_event)))
				current_time = ele_time_traveled(genReq.get(next_event).get(0).time,Math.abs(elevator_floor - next_event));
			}
			//Bottom
			else{
			    //Time to go down
			    current_time = ele_time_traveled(current_time,elevator_floor);
			    //Time to go up to the next event
			    current_time = ele_time_traveled(current_time,next_event);
			    if(current_time < ele_time_traveled(genReq.get(next_event).get(0).time,Math.abs(elevator_floor - next_event))){
				current_time = genReq.get(next_event).get(0).time;
				current_time = ele_time_traveled(current_time,next_event);
			    }
			    
			}
		    else
			break;
		    IDLE = false;
		}
		else{
		    //Add all people on the elevator queue with the imminent requests
		    queue.addAll(getElementsQueue(genReq,next_event,current_time));

		    if(debug)
			printElevator(queue);

		    boolean firstEle = true;
		    
		    //Update the general queue(Removing people based on the current time)
		    genReq = updateQueue(genReq,next_event,current_time);

		    
		    while(!queue.isEmpty()){
			//Check if general request is much immenent than the first element on the elevator queue
			int check = next_event(genReq,num_floor);
			if(genReq.get(check).get(0).time < queue.get(0).time){
			    

			    //update current time to pick up that request
			    current_time   = ele_time_traveled(current_time,Math.abs(elevator_floor - check));
			    elevator_floor = genReq.get(check).get(0).person.floor;
			    //queue all request in that floor based on current time
			    queue.addAll(getElementsQueue(genReq,check,current_time));

			    //update queue
			    genReq = updateQueue(genReq,check,current_time);

			    //sort the elevator queue
			    Collections.sort(queue);


			}

			Request req   = queue.remove(0);
			
			//arrival
			if(req.direction == "up"){
			    //removing all request in the queue with the same floor
			    int ele_size = queue.size();
			    ArrayList<Request> arrivals  = new ArrayList<Request>();

			    
			    //add the first request
			    arrivals.add(req);

			    //add request for arrivals
			    for(int i = 0; i < ele_size; i++){
				if(queue.get(i).floor == req.floor){
				    arrivals.add(queue.get(i));
				}
			    }
			    
			   
			    int arrivals_size = arrivals.size();
			    for(int j = 0; j < arrivals_size; j++){
				Request request = arrivals.remove(0);

				//assign response time up
				if(firstEle == false){
				    request.person.res_time_up = ele_time_traveled(current_time,Math.abs(elevator_floor - request.floor));
				}
				else{
				    request.person.res_time_up = ele_time_traveled(current_time,request.floor);
				    firstEle = false;
				}
				if(debug1)
				    System.out.println("Arrival Response Time: " + request.person.res_time_up + " Floor number: " + request.floor);
				
				//assign departure time
				ArrayList<Request> dep = genReq.get(request.person.floor);
				request.direction = "down";
				request.time      = request.person.res_time_up + request.person.work_till;
				request.floor     = 0;
				request.person    = request.person;
				
				//add the departure on the designated floor
				dep.add(request);
				Collections.sort(dep);
				genReq.set(request.person.floor,dep);
				
				//update current time
				current_time = request.person.res_time_up;
				
				//update elevator floor
				elevator_floor = request.person.floor;
				
				if(debug1)
				    System.out.println("Current Time: " + current_time + " Elevator current floor " + elevator_floor);
				
			    }
			    
			    //remove all request with the same floor
			    queue = dequeue(queue,req.floor);

			    if(debug1)
				printBuilding(genReq);
			}
			//departure
			else{
			    //removing all departure request
			    int ele_size = queue.size();
			    ArrayList<Request> departures = new ArrayList<Request>();

			    //add first departure
			    departures.add(req);

			    //add all departure request
			    for(int i = 0; i < ele_size; i++){
				if(queue.get(i).direction == "down"){
				    departures.add(queue.get(i));
				}
			    }

			    //printElevator(departures);
			    //printElevator(queue);

			    //update current time
			    current_time = ele_time_traveled(current_time, req.person.floor);
			    
			    if(debug1)
				System.out.println("Current Time: " + current_time);

			    int departures_size = departures.size();
			    for(int j = 0; j < departures_size; j++){
				Request dep_req = departures.remove(0);

				//assign departure response time
				dep_req.person.res_time_down = dep_req.time;

				//assign departure time
				dep_req.person.departure = current_time;

				if(debug1){
				    System.out.println("Departure Time: " + dep_req.person.departure + " Was in Floor number " + dep_req.person.floor);
				}
				
				index++;

				if(debug2){
				    double timeUp      = 0.0;
				    double timeDown    = 0.0;

				    timeUp             = dep_req.person.res_time_up - dep_req.person.arrival;
				    timeDown           = dep_req.person.departure - dep_req.person.res_time_down;

				    if(timeDown > 0){
					//System.out.println(timeUp + " " + timeDown + " ID " + dep_req.person.id);
					stats.res_time_up   += timeUp;
					stats.res_time_down += timeDown;

					stats.count++;
				    }
				   
				    
				}
			    }

			    //remove departures from the queue
			    for(int k = 0; k < ele_size; k++){
				try{
				    if(queue.get(k).direction == "down"){
					queue.remove(queue.get(k));
				    }
				}
				catch(Exception e){
				    break;
				}
			    }

			    
			    elevator_floor = 0;
			    
			    if(debug1)
				printBuilding(genReq);
			}
		    }
		    IDLE = true;
		    next_event = next_event(genReq,num_floor);
		} 
	    }
	    
	    if(debug2){
		System.out.println("Average up   "+ idle_behavior +" response time " + stats.res_time_up / stats.count);
		System.out.println("Average down "+ idle_behavior +" response time " + stats.res_time_down / stats.count);
		}	    
	}
	else{
	    System.out.println("Error: No such Algorithm Exist");
	}
    }
    
    /*==========================================THIS ARE HELPER FUNCTIONS============================================================*/

    private static ArrayList<Request> dequeue_depart(ArrayList<Request> ele_queue){
	for(int i = 0; i < ele_queue.size(); i++){
	    if(ele_queue.get(i).direction == "down"){
		ele_queue.remove(i);
	    }
	}
	return ele_queue;
    }
    //Remove people with the same floor
    private static ArrayList<Request> dequeue(ArrayList<Request> ele_queue, int floor){
	for(int i = 0; i < ele_queue.size();i++){
	    if(ele_queue.get(i).floor == floor){
		ele_queue.remove(i);
	    }
	}
	return ele_queue;
    }
    
    //Elevator time traveled
    private static double ele_time_traveled(double time,int floor){
	return ((time) + (10 * floor));
    }
    
    //Get Elements on the queue lower than the current time
    private static ArrayList<Request> getElementsQueue(ArrayList<ArrayList<Request>> queue,int floor, double current_time){

	//get the size of the floor queue
	int queue_size = queue.get(floor).size();
	ArrayList<Request> ele_queue = new ArrayList<Request>();
	
	for(int i = 0; i < queue_size; i++){
	    if(queue.get(floor).get(0).time <= current_time){
		ele_queue.add(queue.get(floor).remove(0));
	    }
	    else
		break;
	}
	return ele_queue;
    }

    //not working
    //Update the general queue 
    private static ArrayList<ArrayList<Request>> updateQueue(ArrayList<ArrayList<Request>> queue,int floor, double current_time){

	//get the size of the floor queue
	int queue_size = queue.get(floor).size();

	//Remove the elements that are lower than the current_time
	for(int i = 0; i < queue_size; i++){
	    if(!queue.get(floor).isEmpty()){
		try{
		    if(queue.get(floor).get(i).time <= current_time){
			System.out.println("Removing "+queue.get(floor).get(i).time);
			queue.get(floor).remove(i);
			
		    }
		    else
			break;
		}
		catch(Exception e){
		    break;
		}
	    }
	}
	return queue;
    }
    

    //Returns the index of the next imminent event
    private static int next_event(ArrayList<ArrayList<Request>> queue, int num_floor){
	double temp;
	int index = 0;
	temp = MAX_VALUE;
	
	for(int i = 0; i < num_floor; i++){
	    if(!queue.get(i).isEmpty()){
		if(temp > queue.get(i).get(0).time){
		    temp = queue.get(i).get(0).time;
		    index = i;
		}
	    }
	}
	
	return index;
    }
    private static ArrayList<ArrayList<Request>> gen_arrival(double lambda,double service_lambda, ArrayList<ArrayList<Request>> genReq,int num_floor){
	
	double arr = 0.0;
	for(int i = 0; i < NUM_ARR; i++){
	    arr += (-(1/lambda) * Math.log(1 - R1.nextDouble())) * 60;
	    Person person  = new Person(arr,Equilikely(1,num_floor-1), gen_serv(service_lambda)*60,i);
	    
	    Request request = new Request("up",person.arrival,person.floor,person);
	    
	    //Add the first request to the elevator queue
	    ArrayList<Request> req  = genReq.get(0);
	    req.add(request);
	    //Sort the queue so that the imminent element is always on index 0
	    Collections.sort(req);
	    genReq.set(0,req);
	    
	    
	    
	}
	return genReq;	
    }
    
    private static double gen_serv(double lambda){
	return -(1/lambda) * Math.log(1 - R2.nextDouble());
    }
    private static int Equilikely(int low, int high){
	int answer;
	
	answer = low + (int)(R3.nextDouble()*(high - low + 1));
	
	return( answer );
    }
    
    //See the state of the elevator queue
    private static void printElevator(ArrayList<Request> queue){
	System.out.println("\n========================================================");
	for(int j = 0; j < queue.size(); j++){
	    if(queue.get(j).direction == "up"){
		System.out.print("Arrival Time ");
	    }
	    else{
		System.out.print("Departure Time ");
	    }
	    System.out.println(queue.get(j).time + " ID: " + queue.get(j).person.id +" Going to floor " + queue.get(j).floor);
	}
	System.out.println("========================================================\n");
    }
    
    //See the state of the queue of the system
    private static void printBuilding(ArrayList<ArrayList<Request>> queue){
	int floors = queue.size();
	//Print floor queue
	System.out.println("\n========================================================");
	for(int i = 0; i < floors; i++){
	    int queue_size = queue.get(i).size();
	    System.out.println();
	    for(int j = 0; j < queue_size; j++){
		if(i == 0){
		    System.out.print("Ground Floor Arrival Time ");
		}
		else{
		    System.out.print(i+" Floor Departure Time ");
		}
		System.out.println(queue.get(i).get(j).time + " ID: " + queue.get(i).get(j).person.id);
	    }
	}
	System.out.println("========================================================\n");
	
    }
}
