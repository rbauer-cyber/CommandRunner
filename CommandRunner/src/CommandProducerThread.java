import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

class CommandProducerThread implements Runnable {
    private SharedSerialPort resource;
    private boolean commandDone = false;

    private Thread worker;
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(false);

	// Define pattern for detecting motion control commands, single letter commands.
	private Pattern commandPattern = Pattern.compile("[+-hfm]");

    public void interrupt() {
        running.set(false);
        worker.interrupt();
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public CommandProducerThread(SharedSerialPort resource) {
        this.resource = resource;
    }

	private void runCommand(String command) {	
        Matcher matcher = commandPattern.matcher(command);
        Stack<String> lineStack = null;
        String terminator = null;
        String commandResult = null;
        //String line = null;
        
        int timeOut = 4000;        

    	if ( command.contains("u") || command.contains("o") ) {
   			terminator = "command";
   			timeOut = 4000;
    	}
    	else if ( matcher.find() ) { 
    		terminator = "MotionMgr: motor";
   			timeOut = 60000;
    	}
    	else
    	{
    		System.out.println("failed to find command keyword");
    	}
    	
		resource.produceData(command, terminator, timeOut);
		lineStack = resource.getLineStack();
				
        while ( commandResult == null ) {
        	for (String line : lineStack) {
            	//line = lineStack.pop();
	        	
	            if ( line.contains("Motor: position") ) {
	            	commandResult = line;		                	
	            }
	            else if (line.contains("MotionMgr: motor position")) {
	            	commandResult = line;		                			                	
	            }
	            else if (line.contains("MotionMgr: motor error")) {
	            	// Motor position is last field in string, find start of field.
	            	int firstIndex = line.indexOf("position");
	            	String substring = line.substring(firstIndex);
	            	commandResult = "MotionMgr: motor "+substring;		                			                	
	            }
            }
        }
		        
        resource.setCommandResult(commandResult);
    	System.out.println("CommandProducer command done");
    	commandDone = true;
	}
	
	private void runFromCommand(String command) {
    	System.out.println("CommandProducer running command "+command);
    	commandDone = false;
		runCommand(command);		
	}
	
	public String getCommandResult() {
		return resource.commandResult;		
	}
	
	public boolean getCommandDone() {
		return commandDone;		
	}

    @Override
    public void run() {
    	// Command producer waits for other thread to request command and then executes command.
    	System.out.println("CommandProducer running");
        running.set(true);
        stopped.set(false);
 
    	try {
    		while ( running.get() ) { 
	    		if ( resource.command != null ) {
	            	runFromCommand(resource.command);  
	            	commandDone = true;
	    		}
		        Thread.sleep(1000);
    		}
    	}
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("CommandProducer interrupted");
        }
    	
    	//resource.closePort();
    	stopped.set(true);    	
        System.out.println("CommandProducer stopped");
    }
}
