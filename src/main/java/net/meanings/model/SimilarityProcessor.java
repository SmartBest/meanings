/**
 * 
 */
package net.meanings.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.meanings.app.DependencyInjection;
import net.meanings.model.graph.Pattern;

/**
 * @author seliv
 *
 */
public class SimilarityProcessor {

	private DependencyInjection di;
	private List<Pattern> rawSeq;
	private List<Pattern> leftContext;
	private Pattern toAnalyze;
	private List<Pattern> rightContext;
	private int currentPosition;
	private int contextLength;
	private int calculatedSize;
	private int createSimilarityIfFoundCount = 1;
	private List<SimilarityStructure> simList;
	
	private static final Logger logger = LoggerFactory.getLogger(
			SimilarityProcessor.class);
	
	public SimilarityProcessor(DependencyInjection di, int contextLength) {
		this.di = di;
		this.currentPosition = 0;
		this.contextLength = contextLength;
	}
	
	public List<Pattern> findSimilarPatterns(List<Pattern> seq){
		
		this.calculatedSize = calcSize(seq);
		if(this.calculatedSize<2) {
			return seq;
		}
		//Need to limit by contextLength somehow!
		if(this.calculatedSize<(contextLength+1)) {
			return seq;
		}
		
		this.rawSeq = seq;
		
		while (currentPosition < calculatedSize) {
			
			getContext();
			
			String logLeft = "";
			for (Pattern pattern : leftContext) {
				logLeft = logLeft + "<"+pattern.getResolved()+">";
			}
			logger.info("[findSimilar] Left="+logLeft);
			logger.info("[findSimilar] ToAnalyze="+"<"+toAnalyze.getResolved()+">");
			String logRight = "";
			for (Pattern pattern : rightContext) {
				logRight = logRight + "<"+pattern.getResolved()+">";
			}
			logger.info("[findSimilar] Right="+logRight);
			
			searchSimilarityInternal();
		}
		
		return seq;
	}
	
	private void searchSimilarityInternal() {
		simList = di.QHelper().getSimilarities(leftContext, toAnalyze, rightContext);
		for (SimilarityStructure sims : simList) {
 		   if(sims.size()>=createSimilarityIfFoundCount) {
 			   //Create similarity pattern and relations in db
 			   di.QHelper().createNewSimilarity(sims);
 		   }
 	   }
	}

	private void getContext() {
		
		int realPos = 0;
		boolean foundPosition = false;
		boolean rightContextActivated = false;
		leftContext = new ArrayList<Pattern>();
		toAnalyze = null;
		rightContext = new ArrayList<Pattern>();
		
		for (Pattern pattern : this.rawSeq) {
			
			if(foundPosition) {
				rightContextActivated = true;
			}
			
			if(!pattern.isDelimitter() && !foundPosition) {
				realPos = realPos+1;
				
				if(realPos>currentPosition) {
					foundPosition = true;
					currentPosition = realPos;
					toAnalyze = pattern;
				}
			}
			
			if(!foundPosition) {
				leftContext.add(pattern);
			}
			if(rightContextActivated) {
				rightContext.add(pattern);
			}
			
		}
		
		trimToContextLength();
	}

	private void trimToContextLength() {
		if(this.contextLength==-1) {
			return;
		}
		
		while (calcSize(this.leftContext)>contextLength) {
			this.leftContext.remove(0);
		}
		
		while (calcSize(this.rightContext)>contextLength) {
			this.rightContext.remove(this.rightContext.size()-1);
		}
		
		trimDelimitters();
	}

	private void trimDelimitters() {
		if(this.leftContext.size()>0) {
			while (this.leftContext.get(0).isDelimitter()) {
				this.leftContext.remove(0);
			}
		}
		if(this.rightContext.size()>0) {
			while (this.rightContext.get(this.rightContext.size()-1).isDelimitter()) {
				this.rightContext.remove(this.rightContext.size()-1);
			}
		}
	}

	protected int calcSize(List<Pattern> seq) {
		int res = 0;
		
		for (Pattern pattern : seq) {
			if(!pattern.isDelimitter()) {
				res = res+1;
			}
		}
		
		return res;
	}
	
}
