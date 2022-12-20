/**
 * 
 */
package net.meanings.model;

import static org.neo4j.driver.Values.parameters;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.meanings.app.DependencyInjection;
import net.meanings.model.graph.Pattern;
import net.meanings.model.graph.PatternBuilder;
import net.meanings.model.graph.Relation;
import net.meanings.params.ParamValues;
import net.meanings.types.NodeLabel;
import net.meanings.types.RelationshipType;

/**
 * @author seliv
 *
 */
public class Brain implements IBrain {
	//поощрение
	private final double encouragementValue = 0.002;
	//порицание
	private final double penaltyValue = 0.001;
	//Глобальные зависимости
	protected DependencyInjection di;
	//Путь вверх
	protected List<PathElement> upPath = null;
	//Путь вниз
	protected List<PathElement> downPath = null;
	//Хранилище распознанных паттернов
	protected List<Pattern> recognizedPatterns = null;
	//Распознанные паттерны, подготовленные для записи в БД
	protected List<Pattern> savedRecognized = new ArrayList<Pattern>();
	//Наивысший найденный полный паттерн
	protected Pattern fullPatternUp = null;
	//Позиция в последовательности полного паттерна
	protected int fullPatternPosition = -1;
	//Может ли мозг учиться (записывать в БД новые паттерны)
	protected boolean canLearn = true;
	
	private static final Logger logger = LoggerFactory.getLogger(
			Brain.class);
	
	public Brain(DependencyInjection di, boolean canLearn) {
		
		//Может учиться? Новые паттерны и веса.
		this.canLearn = canLearn;
		
		//Qhelper, sLevels, PatternBuilder теперь в DependencyInjection
		this.di = di;

	}

	public String perceive(String message) {
		
		//Инициализируем переменные
		//Пути вверх и вниз
		upPath = null;
		downPath = null;
		//Массив распознанных наиболее высокоуровневых паттернов (сюда попадают при откате)
		recognizedPatterns = null;
		savedRecognized = new ArrayList<Pattern>();
		//Жутко
		fullPatternUp = null;
		fullPatternPosition = -1;

		//всегда всё в верхнем регистре
		message = message.toUpperCase();
		String pred = "";

		//Цикл по строке, берём буквы
		for (int i = 0; i < message.length(); i++) {

			//Находим IO паттерн, соответствующий текущей букве
			Pattern curPattern = getIOPattern(String.valueOf(message.charAt(i)));
			
			logger.info("[perceive] ----"+message.charAt(i)+"----");

			//Если путь вниз не пуст
			if(!isDownPathEmpty())
				//Если текущий паттерн (буква) является последним элементом в пути вниз
				if(curPattern.equals(getLastDownPathElement().getPattern())) {
					//То мы угадали
					predictionIsTrue(curPattern);
					//Продолжаем цикл
					continue;
				} else {
					//Иначе предсказание ложно - мы не угадали
					predictionIsFalse(curPattern);
				}
			
			if(isUpPathEmpty())
				//приходим сюда если ещё ничего не предсказывали
				if(buildUpPath(curPattern,null,null)) {
					//Выводим в лог путь наверх
					logPath(upPath,"upPath");

					//Берём последний элемент из пути наверх
					PathElement lastPE = upPath.get(upPath.size()-1);
					if(buildDownPath(lastPE.getPattern(), lastPE.getWay().getNum(), null, null)) {
						logPath(downPath,"downPath");
					} else {
						savedRecognized.add(curPattern);
						upPath = null;
					}
				}
			
			if(isDownPathEmpty()) {
				upPath = null;
				downPath = null;
				savedRecognized.add(curPattern);
			}
			
		}
		
		if(!isDownPathEmpty()) {
			logPath(upPath,"upPath");
			pred = resolve(getLastDownPathElement().getPattern());
		} else {
			upPath = null;
			pred = "[no_prediction]";
		}
		
		saveRecognizedPatterns();
		rememberSavedRecognized();
				
		return pred;
	}
	
	private void rememberSavedRecognized() {
		
		if (!canLearn) {
			return;
		}
		
		if(savedRecognized.size()>0) {
			List<Pattern> organizedStructure = organizeHierarchy(savedRecognized);
			di.QHelper().attachToTimeline(organizedStructure);
		}
		
	}

	private List<Pattern> organizeHierarchy(List<Pattern> seq) {
				
		List<Pattern> organizedStructure = new ArrayList<Pattern>();
		for (Pattern lp : seq) {
			
			organizedStructure.add(lp);
			//logger.info("[organizeHierarchy] ----"+resolve(node)+"----"+lp.getLevel().getName());
		}
		
		if (organizedStructure.size()<2) {
			return organizedStructure;
		}
		
		int orgLvl = 0;
		
		while(orgLvl < di.SLevels().size()) {
			
			//organizedStructure = detectCollocations(organizedStructure);
			
			if (organizedStructure.size()<2) {
				return organizedStructure;
			}
			
			List<Pattern> toJoin = new ArrayList<Pattern>();
			
			boolean lvlFullyOrganized = false;
			
			boolean firstOccur = true;
			int pos = -1;
			int len = 0;
			for (int i=0; i<organizedStructure.size(); i++) {
				if(i==organizedStructure.size()-1) {
					lvlFullyOrganized = true;
				}
				Pattern cur = organizedStructure.get(i);
				/*if((cur.getLevelNum()>orgLvl) && toJoin.size()<0) {
					continue;
				}*/
				if((cur.getLevelNum()>orgLvl) && toJoin.size()<=1) {
					toJoin.clear();
					firstOccur = true;
					pos = -1;
					len = 0;
					continue;
				}
				if((cur.getLevelNum()>orgLvl) && toJoin.size()>1) {
					break;
				}
				toJoin.add(cur);
				if (firstOccur) {
					pos = i;
					firstOccur = false;
				}
				len ++;
			}
			
			if(toJoin.size()>1) {
				for (int i=0; i<len; i++) {
					organizedStructure.remove(pos);
				}
				
				Pattern lpj = null;
				
				toJoin = detectCollocations(toJoin);
				
				if(toJoin.size()>1) {
					
					//найдём синонимы (семантически близкие паттерны или паттерны-подстановщики)!
					//перенесли поиск семантически близких паттернов в режим сна
					//findSimilarPatterns(toJoin);
					
					lpj = di.QHelper().createNewSequence(toJoin);
				} else {
					lpj = toJoin.get(0);
				}
				organizedStructure.add(pos, lpj);
			}
			
			if(lvlFullyOrganized) {
				orgLvl ++;
			}
		}
		
		
		return organizedStructure;
	}
	
	private List<Pattern> detectCollocations(List<Pattern> seq){
		
		String tf = "[detectCollocations] ";
		for (Pattern r : seq) {
			tf = tf + "<"+resolve(r)+">";
		}
		logger.info(tf);
		
		List<Pattern> buf = new ArrayList<Pattern>();
		Queue<Pattern> inputPatterns = new LinkedList<Pattern>();
		inputPatterns.addAll(seq);
		
		int detectedCount = 0;
		List<Pattern> toFind = new ArrayList<Pattern>();
		while(true) {
			
			if(inputPatterns.size()==0) {
				buf.addAll(toFind);
				if(detectedCount != 0) {
					buf = detectCollocations(buf);
				}
				break;
			}
			
			boolean toFindFormed = false;
			while(!toFindFormed) {
				
				if(inputPatterns.size()<1) {
					break;
				}
				
				//form toFind patterns
				
				if(toFind.size()==0) {
					toFind.add(inputPatterns.poll()); //init toFind patterns
				} else if(toFind.size()==1) {
					toFind.add(inputPatterns.poll()); //read second pattern toFind
					
					boolean firstDelim = toFind.get(0).isDelimitter();
					boolean secondDelim = toFind.get(1).isDelimitter();
					
					if(firstDelim==secondDelim) {
						toFindFormed = true; //idetical patterns - toFind formed
						break;
					}
					if(firstDelim && !secondDelim) { //dont want to connect delimitter with non-delimitter in pair
						buf.add(toFind.get(0)); //backup delimitter to buf
						toFind.remove(0); //remove it from toFind and go to next iteration
						continue;
					}
					if(!firstDelim && secondDelim) {
						continue; //go to triple
					}
										
				} else if(toFind.size()==2) {
					toFind.add(inputPatterns.poll());
					
					if(toFind.get(2).isDelimitter()) {
						buf.add(toFind.get(0)); //backup non-delimitter to buf
						toFind.remove(0);  //remove it from toFind - here stay only 2 delimitters now
					}
					toFindFormed = true;
					break;
				}
				
				
			}
			
			if(!toFindFormed) {
				buf.addAll(toFind);
				toFind.clear();
				continue;
			}
			
			Pattern replacer = findAndReplace(toFind);
			
			if(replacer==null) {
				buf.add(toFind.get(0));
				toFind.remove(0);
				if(toFind.size()>1) {
					buf.add(toFind.get(0));
					toFind.remove(0);
				}
			} else {
				detectedCount = detectedCount + 1;
				buf.add(replacer);
				toFind.clear();
			}

		}
		
		if(buf.size()>1) {
			//try to find whole sequence
			Pattern replacer = findAndReplace(buf);
			if(replacer!=null) {
				buf.clear();
				buf.add(replacer);
			}
		}
		
		return buf;
	}

	private Pattern findAndReplace(List<Pattern> toFind) {
		// TODO Auto-generated method stub
		String tf = "[findAndReplace] ";
		for (Pattern r : toFind) {
			tf = tf + "<"+resolve(r)+">";
		}
		logger.info(tf);
		
		if(toFind.size()==0) {
			return null;
		}
		
		Disambiguater da = new Disambiguater(di);
		
		Pattern replacer = da.tryToReplace(toFind);
		
		return replacer;
	}
	
	private List<Pattern> findSimilarPatterns(List<Pattern> seq){
		
		SimilarityProcessor sp = new SimilarityProcessor(di,2);
		
		seq =  sp.findSimilarPatterns(seq);
		
		return seq;
	}

	private void saveRecognizedPatterns() {
		recognizedPatterns = new ArrayList<Pattern>();
					
		
		if(!isDownPathEmpty()) {
			findOtherDownPath(null, true);
		}
		if(!isUpPathEmpty()) {
			findOtherUpPath(null, true);
		}
		
		saveRecognizedInternal();

		for (Pattern r : savedRecognized) {
			logger.info("[finalSavedRecognized] "+resolve(r));
			/*List<String> rl = di.QHelper().resolveInList(r);
			for (String str : rl) {
				logger.info("[LIST] "+str);
			}*/
		}
		
	}
	
	protected void saveRecognizedInternal() {
		
		if(recognizedPatterns!=null) {
			
			//logger.info("[saveRecognizedPatterns fullPatternUp] "+resolve(fullPatternUp));
			//logger.info("[saveRecognizedPatterns fullPatternPosition] "+fullPatternPosition);
			
			for(int m = 0; m<recognizedPatterns.size(); m++) {
				logger.info("[DEBUG 1] "+resolve(recognizedPatterns.get(m)));
			}
			
			if(fullPatternUp!=null && fullPatternPosition!=-1) {
				for(int i = 0; i<=fullPatternPosition; i++) {
					if (recognizedPatterns.size()>0) {
						recognizedPatterns.remove(recognizedPatterns.size()-1);
					}
				}
				for(int m = 0; m<recognizedPatterns.size(); m++) {
					logger.info("[DEBUG 2] "+resolve(recognizedPatterns.get(m)));
				}
				recognizedPatterns.add(fullPatternUp);
			}
			for(int m = 0; m<recognizedPatterns.size(); m++) {
				logger.info("[DEBUG 3] "+resolve(recognizedPatterns.get(m)));
			}
			
			List<Pattern> revRec = reversedList(recognizedPatterns);
			
			savedRecognized.addAll(revRec);
			
			recognizedPatterns=null;
			fullPatternUp = null;
			fullPatternPosition = -1;
		}
	}

	protected void logPath(List<PathElement> lpe, String label) {
		if(lpe==null) {
			logger.info("["+label+"] NULL");
			return;
		}
		int j = 0;
		for (PathElement pe : lpe) {
			Pattern pattern = pe.getPattern();
			Relation way = pe.getWay();
			String patternName = "NULL";
			String wayNum = "NULL";
			if(pattern != null) {
				patternName = resolve(pe.getPattern());
			}
			if(way != null) {
				Integer v = way.getNum();
				if(v != null){
					wayNum = String.valueOf(v);
				}
			}
			logger.info("["+label+" "+j+"] "+patternName+" "+wayNum);
			j++;
		}
		
	}
	
	protected void predictionIsFalse(Pattern incomingPattern) {
		logger.info("[predictionIsFalse]");
		recognizedPatterns = new ArrayList<Pattern>();
		if (findOtherDownPath(incomingPattern, false)) {
			predictionIsTrue(incomingPattern);
		} else {
			downPath = null;
			
			//logPath(upPath,"upPath");
			
			//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			//This is before
			//TODO: if too many recognizedPaterns then need matching projections
			if(findOtherUpPath(incomingPattern, false)) {
				predictionIsTrue(incomingPattern);
			} else {
				//TODO: save recognized patterns
				saveRecognizedInternal();
				upPath=null;
			}
						
			logPath(upPath,"upPath");
		}
						
	}
	
	protected boolean findOtherUpPath(Pattern incomingPattern, boolean extractRecognizedOnly) {
		logger.info("[findOtherUpPath]");
		
		fullPatternUp = null;
		fullPatternPosition = -1;
		
		boolean found = false;
		
		int originUpPathSize = upPath.size()-1;
		for(int i=originUpPathSize; i>=0; i--) {
			PathElement curPe = upPath.get(i);
			if(!extractRecognizedOnly)
				penalty(curPe);
			Relation way = curPe.getWay();
			upPath.remove(i);
			
			
			
			if(upPath.size()==0)
				break;
				
			PathElement dwPe = upPath.get(i-1);
			dwPe.addExcludeWay(way);
			
			int afterNum = -1;
			if(dwPe.wayHasNum()) {
				afterNum = dwPe.getWay().getNum();
			}
			
			if(i==1) {
				afterNum = afterNum -1;
			}
			
			Pattern dwPattern = dwPe.getPattern();
			int beforeNum = di.QHelper().lengthOfSequence(dwPattern);
			
			///
			if ((afterNum == 0) || (i==1)) {
				if (fullPatternUp==null) {
					fullPatternUp = dwPe.getPattern();
				}
				
				
				/*if(i==originUpPathSize || afterNum == 0) {
					fullPatternPosition = fullPatternPosition + (beforeNum-1);
				} else {
					fullPatternPosition = fullPatternPosition + (beforeNum-1);
				}*/
				fullPatternPosition = fullPatternPosition + (beforeNum-1);
				if(i==1) {
					fullPatternPosition = fullPatternPosition + 1;
				}
				
				
				//fullPatternPosition = fullPatternPosition + (beforeNum);
				
				logger.info("[findOtherUpPath fullPatternUp] "+resolve(fullPatternUp));
				logger.info("[findOtherUpPath fullPatternPosition] "+fullPatternPosition);
				
				/*for(int m = 0; m<recognizedPatterns.size(); m++) {
					logger.info("[findOtherUpPath rcog] "+resolve(recognizedPatterns.get(m)));
				}*/
			} else {
				fullPatternPosition = -1;
				fullPatternUp = null;
			}
			///
			
			if(!extractRecognizedOnly) {
				//List<PathElement> backupPath = copyOfPath(upPath);
				int upPathBackupSize = upPath.size();
				
				List<Pattern> includePatterns = reversedList(recognizedPatterns);
				while(buildUpPath(dwPattern, dwPe.getExcludeWays(), includePatterns)) {
					logger.info("[findOtherUpPath] found other up path");
					
					for (Pattern r : recognizedPatterns) {
						logger.info("[recognizedPatterns] "+resolve(r));
					}
					
					logPath(upPath, "upPath");
					
					int an = getLastUpPathElement().getWay().getNum();
					if(getLastUpPathElement().hasDownPathShift()) {
						an = getLastUpPathElement().getDownPathShift();
						logger.info("[findOtherUpPath] path has shift");
					}
					if(buildDownPath(getLastUpPathElement().getPattern(), an, null, includePatterns)){
						logger.info("[findOtherUpPath] found other down path");
						logPath(downPath, "downPath");
						logger.info("[findOtherUpPath] matching "+resolve(getLastDownPathElement().getPattern())+"="+resolve(incomingPattern));
						if(getLastDownPathElement().getPattern().equals(incomingPattern)) {
							logger.info("[findOtherUpPath] UP and DOWN MATCHED!");
							return true;
						}
						logger.info("[findOtherUpPath] up and down not matched");
					}
					dwPe.addExcludeWay(upPath.get(i).getWay());
					downPath = null;
					
					//rollback upPath
					while (upPath.size()>upPathBackupSize) {
						/*if(getLastUpPathElement().hasDownPathShift()) {
							if(upPath.size()>1) {
								upPath.get(upPath.size()-2).setDownPathShift(getLastUpPathElement().getDownPathShift());
								logger.info("[rollback upPath] save shift");
							}
						}*/
						upPath.remove(upPath.size()-1);
					}
					includePatterns = reversedList(recognizedPatterns);
				}
			}
			
			logger.info("[getRecognizedPatterns] "+resolve(dwPattern)+" after="+afterNum+" before="+beforeNum);
			recognizedPatterns.addAll(getRecognizedPatterns(dwPattern, beforeNum, afterNum));
			
		}
		
		return found;
	}
	

	protected boolean findOtherDownPath(Pattern incomingPattern, boolean extractRecognizedOnly) {
		logger.info("[findOtherDownPath]");
		
		boolean found = false;
		
		int originDownPathSize = downPath.size()-1;
		for(int i=originDownPathSize; i>=0; i--) {
			
			PathElement curPe = downPath.get(i);
			//TODO: not penalty if extractRecognizedOnly
			if(!extractRecognizedOnly)
				penalty(curPe);
			Relation way = curPe.getWay();
			downPath.remove(i);
			
			/*if(i == 0) {
				break;
			}*/
			
			PathElement upPe = getLastUpPathElement();
			int afterNum = upPe.getWay().getNum();
			if(i != 0) {
				upPe = downPath.get(i-1);
				afterNum = -1;
			}
			Pattern upPattern = upPe.getPattern();
			
			
			//reverse recognizedPatterns
			//reversedList(recognizedPatterns);
			
			if((upPattern.hasLabel(NodeLabel.Meaning())||upPattern.hasLabel(NodeLabel.Similarity())) && !extractRecognizedOnly) {
				
				int debug = 0;
				while(!found) {

					debug++;
					logger.info("REFRESH "+debug+" "+resolve(upPattern)+" wayId="+way.id());
					
					
					upPe.addExcludeWay(way);
					
					/*for (Node r : recognizedPatterns) {
						logger.info("[recognizedPatterns] "+resolve(r));
					}*/
					
					if(buildDownPath(upPattern, -1, upPe.getExcludeWays(), reversedList(recognizedPatterns))) {
						logger.info("incomingPattern= "+resolve(incomingPattern));
						logger.info("LastDownPathElement= "+resolve(getLastDownPathElement().getPattern()));
						if(incomingPattern.equals(getLastDownPathElement().getPattern())) {
							found = true;
							break;
						}
					}
					
					if(di.QHelper().countOfMeanings(upPattern) == upPe.getExcludeWays().size()) {
						break;
					}
					
					way = downPath.get(i).getWay();
					
					//TODO: Maybe need recursion here to find other path in other path
					while(downPath.size() != i){
						if(!extractRecognizedOnly)
							penalty(downPath.get(i));
						downPath.remove(i);
					}
					
				}
				if(found)
					break;
			}
			if(upPattern.hasLabel(NodeLabel.Sequence())) {
				logger.info("[findOtherDownPath getRecognizedPatterns] "+resolve(upPattern)+" after="+afterNum+" before="+way.getNum());
				recognizedPatterns.addAll(getRecognizedPatterns(upPattern, way.getNum(), afterNum));
			}
			
			
		}
		
		return found;
	}
	
	protected List<Pattern> reversedList(List<Pattern> input) {
		/*List<?> shallowCopy = input.subList(0, input.size());
		Collections.reverse(shallowCopy);
		return shallowCopy;*/
		List<Pattern> res = new ArrayList<Pattern>();
		for(int i = input.size()-1; i>=0; i--) {
			res.add(input.get(i));
		}
		return res;
	}
	
	protected List<Pattern> getRecognizedPatterns(Pattern pattern, int beforeNum, int afterNum) {
		List<Pattern> res = new ArrayList<Pattern>();
		
		if (pattern.hasLabel(NodeLabel.IO())) {
			res.add(pattern);
			return res;
		}
		
		if (pattern.hasLabel(NodeLabel.Sequence())) {
			for(int i=beforeNum-1; i>afterNum; i--) {
				Pattern p = di.QHelper().getElementOfSequence(pattern, i);
				if(p!=null) {
					res.add(p);
				}
			}
		}
		
		return res;
	}
	
	
	
	protected void predictionIsTrue(Pattern curPattern) {
		logger.info("[predictionIsTrue]");
		recognizedPatterns = null;
		logger.info("[predictionIsTrue] recognizedPatterns flushed!");
		if(!findNextDownPath()) {
			
			logPath(upPath, "upPath");
			
			if(buildUpPath(getLastUpPathElement().getPattern(),null,null)) {
				logger.info("[predictionIsTrue] buildUpPath builded");
				downPath = null;
				buildDownPath(getLastUpPathElement().getPattern(), getLastUpPathElement().getWay().getNum(), null, null);
			} else {
				logger.info("[predictionIsTrue] buildUpPath NOT builded");
				logPath(upPath, "upPath");
				logPath(downPath, "downPath");
				saveRecognizedPatterns();
				//savedRecognized.add(curPattern);
				upPath = null;
				downPath = null;
			}
		}
		
		if(!isDownPathEmpty())
			logPath(downPath,"downPath");
		
	}
	
	protected boolean findNextDownPath() {
		
		logger.info("[findNextDownPath]");
		
		boolean rebuildSome = false;
		
		int originDownPathSize = downPath.size()-1;
		
		for(int i=originDownPathSize; i>=0; i--) {
			encouragement(downPath.get(i));
			Relation way = downPath.get(i).getWay();
			downPath.remove(i);
			
			Pattern upPattern = getLastUpPathElement().getPattern();
			if(i != 0) {
				upPattern = downPath.get(i-1).getPattern();
			}
			
			Integer upPatternLength = di.QHelper().lengthOfSequence(upPattern);
			if(upPatternLength!=null) {
				upPatternLength--;
				if(upPatternLength > way.getNum()) {
					if(buildDownPath(upPattern, way.getNum(), null, null)) {
						rebuildSome = true;
						break;
					}
				}
			}
		}
		
		logger.info("[findNextDownPath] = "+rebuildSome);
		return rebuildSome;
		
	}
	
	protected void encouragement(PathElement pe) {
		encouragement(pe.getWay());
		pe.getPattern().activate();
	}
	
	protected void penalty(PathElement pe) {
		if(pe.getWay()!=null)
			penalty(pe.getWay());
		pe.getPattern().deactivate();
	}
	
	protected void encouragement(Relation way) {
		
		if (!canLearn) {
			return;
		}
		
		if(way==null)
			return;
		
		way.plusWeight(encouragementValue);
		
	}
	
	protected void penalty(Relation way) {
		if(way==null)
			return;
		
		if (!canLearn) {
			return;
		}
		
		way.minusWeight(penaltyValue);
	}
	

	
	protected boolean isUpPathEmpty() {
		if(upPath!=null) {
			if(upPath.size()>0) {
				return false;
			}
		}
		return true;
	}
	
	protected boolean isDownPathEmpty() {
		if(downPath!=null) {
			if(downPath.size()>0) {
				return false;
			}
		}
		return true;
	}
	
	protected PathElement getLastUpPathElement() {
		if(!isUpPathEmpty()) {
			return upPath.get(upPath.size()-1);
		}
		return null;
	}
	
	protected PathElement getLastDownPathElement() {
		if(!isDownPathEmpty()) {
			return downPath.get(downPath.size()-1);
		}
		return null;
	}
	
	public void terminate()
    {
		logger.info("Terminate command. Wait 2 sec to complete transactions...");
		wait(2000);
		logger.info("Brain ends working");
        di.terminate();
    }
	
	public boolean buildUpPath(Pattern currentPattern, List<Relation> excludeWays, List<Pattern> includePatterns) {
		
		if (upPath==null) {
			upPath = new ArrayList<PathElement>();
			//это первый самый низкоуровневый паттерн
			PathElement first = new PathElement(currentPattern, null);
			upPath.add(first);
		}
		
		//int currPathSize = upPath.size();
		boolean builded = false;
		
		
		Pattern cp = currentPattern;
		
		while(true) {
			//пытаемся найти ступеньку вверх
			PathElement pe = di.QHelper().getWayUpStrict(cp, excludeWays);
			
			if(pe != null) {
				Pattern tmpPattern = pe.getPattern();
				int wayNum = pe.getWay().getNum();
				int lastNum = di.QHelper().lengthOfSequence(tmpPattern)-1;
				///
				if(includePatterns != null) {
					if(tmpPattern.hasLabel(NodeLabel.Sequence())) {
						if(wayNum != lastNum) {
							//check if first included pattern matches next pattern in sequence
							//if matches to the end, remove used includePatterns, append to upPath and continue
							//if matches less than end, remove used includePatterns and set downPathShift
							//if not matches - skip this section
							Integer shift = matchingIncludePatterns(tmpPattern, wayNum+1, lastNum, includePatterns);
							if(shift != null) {
								if(shift == lastNum) {
									upPath.add(pe);
									builded = true;
									continue;
								} else {
									pe.setDownPathShift(shift);
								}
							}
						}
					}
				}
				///
				upPath.add(pe);
				builded = true;
				if(tmpPattern.hasLabel(NodeLabel.Sequence())) {
					if(wayNum < lastNum) {
						break;
					}
				}
			} else {
				break;
			}
			cp = pe.getPattern();
		}
		
		if(builded) {
			return true;
		}
		
		if(excludeWays == null) {
			//upPath = null;
		}
		return false;
		
	}
	
	protected Integer matchingIncludePatterns(Pattern pattern, int fromNum, int toNum, List<Pattern> includePatterns) {
		Integer shift = null;
		
		for(int i = fromNum; i<=toNum; i++) {
			if(includePatterns.size()==0) {
				return shift;
			}
			Pattern cp = di.QHelper().getElementOfSequence(pattern, i);
			if(cp.equals(includePatterns.get(0))) {
				shift = i;
				includePatterns.remove(0);
			} else {
				return shift;
			}
		}
		
		return shift;
	}

	public boolean buildDownPath(Pattern currentPattern, int afterNum, List<Relation> excludeWays, List<Pattern> includePatterns) {
		
		if (downPath==null) {
			downPath = new ArrayList<PathElement>();
		}
		
		//int currPathSize = downPath.size();
		boolean builded = false;
		
		Pattern cp = currentPattern;
		int an = afterNum;
		
		while(true) {
			PathElement pe = di.QHelper().getWayDown(cp, an, true, excludeWays);
			if(pe != null) {
				downPath.add(pe);
				builded = true;
				if(cp.hasLabel(NodeLabel.Sequence())) {
					if(includePatterns != null) {
						if(includePatterns.size()>0) {
							if(includePatterns.get(0).id() == pe.getPattern().id()) {
								an = an + 1;
								includePatterns.remove(0);
								downPath.remove(downPath.size()-1);
								continue;
							}
						}
					}
				}
				if(pe.getPattern().hasLabel(NodeLabel.IO())) {
					break;
				}
			} else {
				break;
			}
			cp = pe.getPattern();
			an = -1;
			//TODO: excludeWays = null;
		}
		
		/*if(downPath.size()>currPathSize) {
			return true;
		}*/
		if(includePatterns != null) {
			if(includePatterns.size()>0) {
				return false;
				//builded = false;
			}
		}
		if(!builded) {
			downPath = null;
		}
		return builded;
		
		/*downPath = null;
		return false;*/
		
	}
	
	
	public String resolve(Pattern upPattern) {
		return di.QHelper().resolve(upPattern);
	}
	
		

    public Pattern getIOPattern(String name) {
		//Пробуем найти уже существующий
		Pattern res = di.QHelper().findIONodeByName(name);
		
		if (res == null) {
			return di.QHelper().createIOPattern(name);
		}
		return res;
	}

	
    public static void wait(int ms)
	{
	    try
	    {
	        Thread.sleep(ms);
	    }
	    catch(InterruptedException ex)
	    {
	        Thread.currentThread().interrupt();
	    }
	}

	public void reset() {
		di.QHelper().resetDB();
	}
	
	public void sleep() {

		List<PathElement> unprocessed = di.QHelper().getUnprocessedHighLevelSequence();
		sleepSimilarityProcess(unprocessed);
		di.QHelper().setProcessed(unprocessed);

		di.QHelper().decreaseRelationsWeight(0.001);
		di.QHelper().flushPatternsActivity();
	}

	public void sleepSimilarityProcess(List<PathElement> unprocessed) {
		List<Pattern> seq = new ArrayList<Pattern>();
		for (PathElement pe:unprocessed) {
			Pattern p = pe.getPattern();
			sleepSimilarityProcess(p.getChilds());
			seq.add(p);
		}
		findSimilarPatterns(seq);
	}



}
