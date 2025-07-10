import java.util.Scanner;

public class CommandRunner implements Runnable {
    private Scanner scanner; 
    static SharedSerialPort resource;
	private Thread producerThread;
    
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		boolean runFromPrompt = true;
		resource = new SharedSerialPort(runFromPrompt);
		CommandProducerThread commandProducer = new CommandProducerThread(resource);
		Thread producerthread = new Thread(commandProducer);
		producerthread.start();	
		
		CommandRunner commandRunner = new CommandRunner(producerthread);
		Thread commandThread = new Thread(commandRunner);
		commandThread.start();	

		System.out.println("CommandRunner started");
	}

    public CommandRunner(Thread producerThread) {
        this.producerThread = producerThread;
    }

    @Override
    public void run() {
        String name = "CommandRunner";
    	System.out.println("CommandRunner running");			
        scanner = new Scanner(System.in);         

        while ( true ) {
	        try {
	    		System.out.printf("Enter command: ");
	    		String lineIn = scanner.nextLine()+'\r';
	    		if ( lineIn.contains("q") ) {
	    			break;
	    		}
	
	    		System.out.printf("%s:writing command\n", name);
	    		resource.setCommand(lineIn);
	    		
	    		while (!resource.getDataReady()) {
		    		//System.out.printf("%s:waiting for resource dataReady\n", name);
	    			if ( System.in.available() > 0 ) {
			    		System.out.printf("%s:scanner has a line\n", name);
	    	    		lineIn = scanner.nextLine()+'\r';
	    	    		if ( lineIn.contains("s") ) {
				    		System.out.printf("%s:sending stop command\n", name);
	    	    			//resource.setStopCommand(true);
	    	    			resource.stopCommand();
	    	    		}
	    			}	    				
			        Thread.sleep(100);
	    		}
	    		
	    		//resource.consumeData(lineIn);
	    		//resource.consumeData();
	    		System.out.printf("%s:consume data done\n", name);
		        Thread.sleep(500);
	        }
		    catch (Exception e) {
		        // This block will catch any exception that is a subclass of Exception
		        System.out.println("An exception occurred: " + e.getMessage());
		        // Optionally, log the full stack trace for debugging
		        e.printStackTrace();
		    }
        }
        
    	// Stop the command producer thread.
        resource.closePort();
        producerThread.interrupt();

    	// Wait for the command producer thread to finish.
        try {
	        while ( producerThread.isAlive() )
	        	Thread.sleep(200);
        }
	    catch (Exception e) {
	        // This block will catch any exception that is a subclass of Exception
	        System.out.println("An exception occurred: " + e.getMessage());
	        // Optionally, log the full stack trace for debugging
	        e.printStackTrace();
	    }
        
    	System.out.println("CommandRunner: App done");
    	scanner.close();        
    }	
}
