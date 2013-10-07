package org.sikuli.slides.api.actions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Timer;
import java.util.TimerTask;

import org.sikuli.slides.api.Context;
import com.google.common.base.Objects;

// execute all children actions in parallel
// execution returns when all the children have finished execution
// 
public class RetryActionNode extends ActionNode {
	
	public RetryActionNode(Action action, long timeout, long interval){
		setAction(checkNotNull(action));
		this.timeout = timeout;
		this.interval = interval;
	}
	
	long timeout;
	long interval;
	volatile boolean timesupFlag;
	volatile boolean stopFlag;
	
	/**
	 * Execute and wait for execution to finish
	 */
	public void execute(Context context) throws ActionExecutionException{
		Action action = checkNotNull(getAction());
		
		// start a timer that will set the timesup flag to true
		Timer timer = new Timer();
		TimerTask task = new TimerTask(){
			@Override
			public void run() {
				timesupFlag = true;
			}
		};
		timer.schedule(task, timeout);				
		
		ActionExecutionException exception = null;
		
		timesupFlag = false;
		while(!timesupFlag && !stopFlag){
			try{
				action.execute(context);
				return;
			}catch(ActionExecutionException e){				
				exception = e;
			}
			synchronized (this){
				try {
					this.wait(interval);
				} catch (InterruptedException e) {
				}
			}
		}

		if (timesupFlag){
			// execution does not succeed before timeout
			// rethrow the exception
			if (exception != null)
				throw exception;
		}
			
			
	}
	
	public void stop(){
		stopFlag = true;
		synchronized(this){
			this.notify();
		}
	}

	public String toString(){
		return Objects.toStringHelper(this)
				.add("actions", getChildren())
				.toString();
	}

}