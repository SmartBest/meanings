/**
 * 
 */
package net.meanings.app;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.meanings.model.Brain;
import net.meanings.model.IBrain;
import net.meanings.sensor.TextFileReader;

/**
 * @author seliv
 *
 */
public class App {
	
	private static final Logger logger = LoggerFactory.getLogger(
			App.class);

	/**
	 * @param args
	 * Точка входа
	 */
	public static void main(String[] args) {
		logger.info("--- Meanings-net app start ---");
        
		DependencyInjection di = new DependencyInjection("bolt://localhost:7687", "neo4j", "123456");
		
		IBrain brain = new Brain(di, true);
		/*String message = "meme";
		String answer = brain.perceive(message);
		
		logger.info("Message: {}", message);
		logger.info("Answer: {}", answer);*/
		
		//logger.info("Nodes: {}", di.QHelper().getAllNodeCount());
		//logger.info("Relations: {}", di.QHelper().getAllRelationCount());
		
		//brain.reset();
		
		//readFile("res/test_text3.txt", brain);
		
		brain.sleep();
		
		/*String message = "маша каша наша.";
		String answer = brain.perceive(message);
		
		logger.info("Message: {}", message);
		logger.info("Answer: {}", answer);
		
		message = "не ваша, а наша.";
		answer = brain.perceive(message);
		
		logger.info("Message: {}", message);
		logger.info("Answer: {}", answer);*/
		
		/*message = "new long sentense. and new one!";
		answer = brain.perceive(message);
		
		logger.info("Message: {}", message);
		logger.info("Answer: {}", answer);
		
		message = "and next two! many sentenses.";
		answer = brain.perceive(message);
		
		logger.info("Message: {}", message);
		logger.info("Answer: {}", answer);
		
		message = "new long sentense. and two!";
		answer = brain.perceive(message);
		
		logger.info("Message: {}", message);
		logger.info("Answer: {}", answer);*/
		
		brain.terminate();
		
        logger.info("--- Meanings-net app end ---");
	}
	
	public static void readFile(String name, IBrain brain) {
		TextFileReader fr = new TextFileReader(name);
		
		try {
		
			String line = fr.getNextLine();
			
			while(!line.equals("<end>")) {
				
				String answer = brain.perceive(line);
				
				logger.info("Message: {}", line);
				logger.info("Answer: {}", answer);
				
				line = fr.getNextLine();
			}
		
		} catch (IOException e) {
			logger.error("ReadFile error", e);
		}
	}
	

}
