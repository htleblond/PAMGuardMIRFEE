package mirfeeFeatureExtractor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import mirfee.MIRFEEJarExtractor;

/**
 * Creates an instance of a Python interpreter, sends clips to the
 * Python script for processing, and manages communication between
 * Java and Python.
 * @author Holly LeBlond
 */
public class FEPythonThreadManager {
	
	private FEControl feControl;
	private boolean active;
	private boolean printThreadsActive;
	private String pathname;
	private BufferedWriter bw = null;
	private BufferedReader br = null;
	private BufferedReader ebr = null;
	private InputPrintThread ipt = null;
	private ErrorPrintThread ept = null;
	private RunnerThread rt = null;
	//private ArrayList<String> activePythonThreads;
	private Process pr;
	public PythonInterpreterThread pit = null;
	protected volatile boolean pamHasStopped;
	private volatile long rdbctTimestamp;
	
	// The following should be synchronized with the following hierarchy:
	protected volatile ArrayList<ContourClip> waitList;
	protected volatile ArrayList<String> idList;
	protected volatile ArrayList<ArrayList<ContourClip>> ccList;
	private volatile ArrayList<ArrayList<String[]>> pythonOutpList;
	protected volatile boolean[] activeThreads;
	private volatile ArrayList<String> commandList;
	
	public volatile ArrayList<String> remainingUIDs; // TODO This is only for testing.
	
	public FEPythonThreadManager(FEControl feControl) {
		this.feControl = feControl;
		//this.activePythonThreads = new ArrayList<String>();
		this.printThreadsActive = true;
		this.commandList = new ArrayList<String>();
		//this.rdbctSignal = false;
		//this.rdbctSignalList = new ArrayList<Integer>();
		//this.lastClusterPushed = 0;
		this.rdbctTimestamp = -1;
		this.remainingUIDs = new ArrayList<String>();
		this.activeThreads = new boolean[feControl.getParams().expMaxThreads];
		this.pamHasStopped = true;
		
		String defpathname = feControl.getParams().tempFolder;
		this.pathname = "";
		for (int i = 0; i < defpathname.length(); i++) {
			if (!defpathname.substring(i, i+1).equals("\\")) {
				this.pathname += defpathname.substring(i, i+1);
			} else {
				this.pathname += "/";
			}
		}
		
		if (feControl.getParams().tempFolder.length() > 0) {
			setActive();
		} else {
			setInactive();
		}
		
		this.pit = new PythonInterpreterThread();
		pit.start();
	}
	
	/**
	 * Checks if threads are still running and restarts them if necessary.
	 */
	public void checkThreads() {
		if (!pit.isAlive() || active == false) {
			pit = new PythonInterpreterThread();
			pit.start();
			active = true;
		}
		if (!printThreadsActive) {
			printThreadsActive = true;
			rt = new RunnerThread();
			rt.start();
			ipt = new InputPrintThread();
			ipt.start();
			ept = new ErrorPrintThread();
			ept.start();
		}
	}
	
	/**
	 * Clears waitList, idList, ccList and pythonOutpList, and calls resetActiveThread().
	 */
	public void resetWaitlists() {
		waitList = new ArrayList<ContourClip>();
		idList = new ArrayList<String>();
		ccList = new ArrayList<ArrayList<ContourClip>>();
		resetActiveThreads();
		pythonOutpList = new ArrayList<ArrayList<String[]>>();
	}
	
	/**
	 * Sets activeThread to -1. This means that addVectorToDataBlock() won't
	 * skip over any slots in pythonOutpList. Don't call this unless FEProcess
	 * has finished with its current cluster.
	 */
	public void resetActiveThreads() {
		if (activeThreads == null) {
			activeThreads = new boolean[feControl.getParams().expMaxThreads];
		} else {
			synchronized(activeThreads) {
				activeThreads = new boolean[feControl.getParams().expMaxThreads];
			}
		}
	}
	
	/**
	 * Thread that initializes and sends commands to the Python interpreter.
	 */
	protected class PythonInterpreterThread extends Thread {
		protected PythonInterpreterThread() {}
		@Override
		public void run() {
			if (active) {
				try {
					resetWaitlists();
					
					// Kudos to this: https://stackoverflow.com/questions/25041529/how-to-run-the-python-interpreter-and-get-its-output-using-java
					
					ProcessBuilder pb = new ProcessBuilder("python", "-i");
					pr = pb.start();
			        br = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			        ebr = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
		            bw = new BufferedWriter(new OutputStreamWriter(pr.getOutputStream()));
		            pythonCommand("import os");
		            pythonCommand("os.chdir(r\""+pathname+"\")");
		            pythonCommand("os.getcwd()");
		            pythonCommand("import numpy as np");
		            //pythonCommand("np.set_printoptions(legacy='1.25')");
		            String pyParams = feControl.getParams().outputPythonParamsToText();
		            if (pyParams.length() > 0) {
		            	pythonCommand("txtParams = "+pyParams);
		            } else {
		            	pythonCommand("txtParams = []");
		            }
		            pythonCommand("import librosa");
		            pythonCommand("import librosa");
		            pythonCommand("import sys");
		            pythonCommand("import gc");
		            pythonCommand("import FEPythonThread");
		            
					String outpstr = null;
					if (br.ready()) {
						while ((outpstr = br.readLine()) != null) {
							if (feControl.getParams().miscPrintJavaChecked)
								System.out.println(outpstr);
						}
					}
					if (ebr.ready()) {
						while ((outpstr = ebr.readLine()) != null) {
							if (feControl.getParams().miscPrintJavaChecked)
								System.out.println(outpstr);
						}
					}
					
					System.out.println("Python Interpreter Thread initialization successful.");
				} catch (Exception e) {
					System.out.println("Python Interpreter Thread initialization failed.");
					e.printStackTrace(System.out);
				}
			}
			startPrintThreads();
			while (active || commandList.size() > 0) {
				if (commandList.size() > 0) {
					String command;
					synchronized(commandList) {
						command = commandList.remove(0);
					}
					pythonCommand(command);
				}
				if (commandList.size() == 0) {
					try {
						TimeUnit.MILLISECONDS.sleep(50); // consider speeding this up
					} catch (Exception e) {
						System.out.println("Sleep exception.");
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * A simple data structure for storing info to be passed to the Python script.
	 */
	protected class ContourClip {
		
		public String clusterID;
		public String uid;
		public String nrName;
		public String clipName;
		public String[] headerData;
		
		protected ContourClip(String clusterID, String uid, String nrName, String clipName, String[] headerData) {
			this.clusterID = clusterID;
			this.uid = uid;
			this.nrName = nrName;
			this.clipName = clipName;
			this.headerData = headerData;
		}
	}
	
	/**
	 * Adds a Python command to the queue.
	 */
	public void addCommand(String inp) {
		synchronized(commandList) {
			commandList.add(inp);
		}
	}
	
	/**
	 * Adds a contour clip to the waitList.
	 * "headerData" should include contour header data (see use in FEProcess for details).
	 */
	public void sendContourClipToThread(String clusterID, String uid, String nrName, String clipName, String[] headerData) {
		synchronized(waitList) {
			remainingUIDs.add(uid);
			ContourClip cc = new ContourClip(clusterID, uid, nrName, clipName, headerData);
			waitList.add(cc);
		}
	}
	
	/**
	 * @return How many Python commands are still in the queue.
	 */
	public int commandsLeft() {
		return commandList.size();
	}
	
	/**
	 * @return How many clips there are that haven't been sent to the Python script yet.
	 */
	public int getWaitlistSize() {
		return waitList.size();
	}
	
	/**
	 * @return How many clips there are that are currently being processed.
	 */
	public int clipsLeft() {
		ArrayList<ArrayList<ContourClip>> ccClone = new ArrayList<ArrayList<ContourClip>>(ccList);
		int outp = 0;
		for (int i = 0; i < ccClone.size(); i++) {
			outp += ccClone.get(i).size();
		}
		return outp;
	}
	
	/**
	 * @return How many instances of Python output there are that haven't been added to the data block yet.
	 */
	public int vectorsLeft() {
		ArrayList<ArrayList<String[]>> pythonOutpClone = new ArrayList<ArrayList<String[]>>(pythonOutpList);
		int outp = 0;
		for (int i = 0; i < pythonOutpClone.size(); i++) {
			outp += pythonOutpClone.get(i).size();
		}
		return outp;
	}
	
	/**
	 * Shuts down the Python interpreter.
	 * Probably useless and would likely cause a tonne of exceptions.
	 */
	@Deprecated
	protected void shutDown() {
		if (bw != null) {
			try {
				bw.write("quit()");
				bw.newLine();
				bw.flush();
				bw.close();
			} catch (IOException e) {
				System.out.println("IOException in shutDown().");
			}
		}
	}
	
	/**
	 * Directly sends a Python command to the interpreter.
	 */
	private void pythonCommand(String command) {
		if (bw != null) {
			try {
				if (feControl.getParams().miscPrintInputChecked)
					System.out.println("FE COMMAND: "+command);
				if (command != null) {
					bw.write(command);
					bw.newLine();
					bw.flush();
				}
			} catch (IOException e) {
				System.out.println("IOException in pythonCommand().");
			}
		}
	}
	
	/**
	 * Signals to the Python thread manager how far ahead the raw data block has reached in the current file.
	 */
	public void setRDBCTTimestamp(long inp) {
		rdbctTimestamp = inp;
	}
	
	/**
	 * Thread that passes ContourClip objects from the waitList to the ccList and
	 * sends Python commands in the queue to the interpreter.
	 */
	protected class RunnerThread extends Thread {
		protected RunnerThread() {}
		@Override
		public void run() {
			while(printThreadsActive) {
				if (waitList.size() > 0 && clipsLeft() < feControl.getParams().expMaxClipsAtOnce) {
					synchronized(waitList) {
						ContourClip cc = waitList.get(0);
						if (cc == null) { // TODO FIGURE OUT WHAT CAUSES THIS
							waitList.remove(0);
							feControl.subtractOneFromPendingCounter();
							feControl.addOneToCounter(FEPanel.FAILURE, "???");
						} else if (idList.contains(cc.clusterID)) {
							synchronized(idList) { synchronized(ccList) {
								ccList.get(idList.indexOf(cc.clusterID)).add(cc);
								String command = "thread"+String.format("%02d", idList.indexOf(cc.clusterID))+".addClip(r\""+cc.clipName+"\"";
								for (int i = 0; i < cc.headerData.length; i++) {
									command += ","+cc.headerData[i];
								}
								command += ")";
								synchronized(commandList) {
									commandList.add(command);
								}
								waitList.remove(0);
							} }
						} else {
							int index = -1;
							synchronized(idList) { synchronized(ccList) { synchronized(pythonOutpList) {
								if (idList.size() < feControl.getParams().expMaxThreads) {
									idList.add(cc.clusterID);
									ccList.add(new ArrayList<ContourClip>());
									pythonOutpList.add(new ArrayList<String[]>());
									index = idList.size()-1;
								} else {
									for (int i = 0; i < ccList.size(); i++) {
										if (ccList.get(i).size() == 0 && pythonOutpList.get(i).size() == 0) {
											index = i;
											idList.set(index, cc.clusterID);
											pythonOutpList.set(index, new ArrayList<String[]>());
											break;
										}
									}
								}
							} } }
							if (index != -1) {
								synchronized(commandList) {
									if (feControl.getParams().audioNRChecked) {
										commandList.add("nr"+String.format("%02d", index)+
												" = FEPythonThread.loadAudio(fn=r\""+cc.nrName+"\", sr="+String.valueOf(feControl.getParams().sr)+")");
										commandList.add("thread"+String.format("%02d", idList.indexOf(cc.clusterID))+
												" = FEPythonThread.FEThread(r\""+cc.nrName+"\", nr"+String.format("%02d", idList.indexOf(cc.clusterID))+", txtParams)");
									} else {
										commandList.add("thread"+String.format("%02d", idList.indexOf(cc.clusterID))+
												" = FEPythonThread.FEThread(\"\", [], txtParams)");
									}
								}
								synchronized(activeThreads) {
									activeThreads[index] = true;
								}
							}
							try {
								TimeUnit.MILLISECONDS.sleep(10);
							} catch (Exception e) {
								System.out.println("Sleep exception.");
								e.printStackTrace();
							}
						}
					}
				} else {
					try {
						TimeUnit.MILLISECONDS.sleep(100);
					} catch (Exception e) {
						System.out.println("Sleep exception.");
						e.printStackTrace();
					}
				}
				if (vectorsLeft() > 0) {
					pushVectorsToDataBlock();
				}
			}
		}
	}
	
	/**
	 * Processes output from the InputPrintThread.
	 */
	protected boolean processPythonOutput(String inp) {
		if (inp.startsWith("outp:")) {
			//System.out.println(inp);
			FEParameters params = feControl.getParams();
			synchronized(idList) { synchronized (ccList) { synchronized(pythonOutpList) {
				String[] tokens = inp.substring(7,inp.length()-1).split(", ");
				boolean breakLoop = false;
				for (int i = 0; i < ccList.size(); i++) {
					for (int j = 0; j < ccList.get(i).size(); j++) {
						if (ccList.get(i).get(j).uid.equals(tokens[1])) {
							ccList.get(i).remove(j);
							breakLoop = true;
							break;
						}
					}
					if (breakLoop) {
						break;
					}
				}
				String foundID = tokens[0].substring(1,tokens[0].length()-1);
				int slot = idList.indexOf(foundID);
				if (slot < 0) {
					if (idList.size() < feControl.getParams().expMaxThreads) {
						//System.out.println("Condition A");
						slot = idList.size();
						idList.add(tokens[0].substring(1,tokens[0].length()-1));
						ccList.add(new ArrayList<ContourClip>());
						pythonOutpList.add(new ArrayList<String[]>());
					} else {
						//System.out.println("Condition B");
						for (int i = 0; i < pythonOutpList.size(); i++) {
							if (pythonOutpList.get(i).size() == 0 && ccList.get(i).size() == 0) {
								slot = i;
								idList.set(i, foundID);
								break;
							}
						}
					}
					if (slot < 0) {
						remainingUIDs.remove(tokens[1]); // TODO
						feControl.subtractOneFromPendingCounter();
						feControl.addOneToCounter(FEPanel.FAILURE, tokens[1]);
						return false;
					}
				}
				pythonOutpList.get(slot).add(tokens);
				// The blankfile/matchesfeatures checks are pretty much already taken care of in FESettingsDialog now.
				if (params.outputDataOption > 0) {
					try {
						File f = new File(params.outputDataName);
						f.setWritable(true, false);
						String outp = "";
						outp += tokens[0].substring(1, tokens[0].length()-1); // 0 - cluster
						outp += ","+tokens[1]; // 1 - uid
						int index = 2;
						if (params.outputDataOption == params.OUTPUT_MTSF) {
							String location = tokens[index++]; // 2 (.mtsf) - location
							outp += ","+location.substring(1, location.length()-1);
						}
						outp += ","+FEControl.convertDateLongToString(Long.valueOf(tokens[index++])); // 2 (.mfe), 3 (.mtsf) - datetime (as date string)
						outp += ","+tokens[index++]; // 3 (.mfe), 4 (.mtsf) - duration
						outp += ","+tokens[index++]; // 4 (.mfe), 5 (.mtsf) - lf
						outp += ","+tokens[index++]; // 5 (.mfe), 6 (.mtsf) - hf
						if (params.outputDataOption == params.OUTPUT_MTSF) {
							String label = tokens[index++]; // 7 (.mtsf) - label
							outp += ","+label.substring(1, label.length()-1);
						}
						for (int i = index; i < tokens.length; i++) {
							if (tokens[i].length() == 0) {
								System.out.println("ERROR: "+tokens[1]+" -> "+params.featureList[i-index][1]+" has no value.");
								return false;
							} else if (tokens[i].equals("nan")) {
								System.out.println("ERROR: "+tokens[1]+" -> "+params.featureList[i-index][1]+" = NaN.");
								return false;
							}
							String[] num_tokens = tokens[i].split("e");
							if (num_tokens.length == 1) outp += ","+tokens[i];
							else if (num_tokens.length == 2)
								outp += ","+String.valueOf(Double.valueOf(num_tokens[0])*Math.pow(10, Integer.valueOf(num_tokens[1])));
							else {
								System.out.println("ERROR: "+tokens[1]+" -> "+params.featureList[i-3][1]+" has a non-sequitur value.");
								return false;
							}
						}
						outp += "\n";
						try {
							PrintWriter pw = new PrintWriter(new FileOutputStream(f, true));
							StringBuilder sb = new StringBuilder();
							sb.append(outp);
							pw.write(sb.toString());
							pw.flush();
							pw.close();
							// There was an attempt to re-format the files into binary like .pgdf files, but I've abandoned that for now.
						/*	DataOutputStream dos = new DataOutputStream(new FileOutputStream(f, true));
							String[] outptokens = outp.split(",");
							dos.writeUTF(outptokens[0]); // 0 - cluster
							dos.writeLong(Long.valueOf(outptokens[1])); // 1 - uid
							index = 2;
							if (params.outputDataOption == params.OUTPUT_MTSF)
								dos.writeUTF(outptokens[index++]); // 2 (.mtsf) - location
							dos.writeLong(Long.valueOf(outptokens[index++])); // 2 (.mfe), 3 (.mtsf) - datetime (as long)
							dos.writeInt(Double.valueOf(outptokens[index++]).intValue()); // 3 (.mfe), 4 (.mtsf) - duration
							dos.writeInt(Double.valueOf(outptokens[index++]).intValue()); // 4 (.mfe), 5 (.mtsf) - lf
							dos.writeInt(Double.valueOf(outptokens[index++]).intValue()); // 5 (.mfe), 6 (.mtsf) - hf
							if (params.outputDataOption == params.OUTPUT_MTSF)
								dos.writeUTF(outptokens[index++]); // 7 (.mtsf) - label
							for (index = index; index < outptokens.length; index++)
								dos.writeDouble(Double.valueOf(outptokens[index])); // features written as doubles
							dos.close(); */
						} catch (Exception e2) {
							e2.printStackTrace();
							System.out.println("ERROR: "+tokens[1]+" -> Could not write row to output file.");
							return false;
						}
					} catch (Exception e3) {
						System.out.println("ERROR: "+tokens[1]+" -> Python output not formatted correctly.");
						return false;
					}
				}
			} } } // synchronized
		}
		return true;
	}
	
	/**
	 * Parses through pythonOutpList and sends any feature vector data that's ready to go to the data block.
	 */
	private void pushVectorsToDataBlock() {
		synchronized(ccList) { synchronized(pythonOutpList) {
			for (int i = 0; i < pythonOutpList.size(); i++) {
				if (feControl.feProcess.getVectorDataBlock().isFinished() && getWaitlistSize() == 0 && clipsLeft() == 0)
					resetActiveThreads();
				ArrayList<String[]> currList = new ArrayList<String[]>(pythonOutpList.get(i));
				if (ccList.get(i).size() > 0 || currList.size() == 0) {
					continue;
				}
				
				long startTime = -1;
				long endTime = -1;
				for (int j = 0; j < currList.size(); j++) {
					long datetime;
					long duration;
					if (feControl.getParams().inputFromWMATorMTSF && feControl.getParams().inputFilesAreMTSF()) {
						datetime = Long.valueOf(currList.get(j)[3]);
						duration = (long) Double.valueOf(currList.get(j)[4]).doubleValue();
					} else {
						datetime = Long.valueOf(currList.get(j)[2]);
						duration = (long) Double.valueOf(currList.get(j)[3]).doubleValue();
					}
					if (startTime == -1 || startTime > datetime)
						startTime = datetime;
					if (endTime < datetime + duration)
						endTime = datetime + duration;
				}
				if (feControl.getParams().miscClusterChecked)
					endTime += feControl.getParams().miscJoinDistance;
				/*
				 * Drops the corresponding active thread flag if:
				 * - RDBCT has passed the end of the active thread plus the join distance and buffer
				 * - Date/time of following file occurs before file the cluster came from (Sound Acquisition sorts by file creation date, not file name)
				 * - pamStop has been called (and pamStart hasn't been called since)
				 */
				synchronized(activeThreads) {
					if (activeThreads[i] && 
							(endTime + feControl.getParams().expBlockPushTriggerBuffer < rdbctTimestamp || startTime > rdbctTimestamp || pamHasStopped))
						activeThreads[i] = false;
					if (activeThreads[i])
						continue;
				}
				// (Keeping this for troubleshooting.)
			/*	System.out.println("\nCluster "+currList.get(0)[0]);
				//System.out.println("activeThread: "+String.valueOf(activeThread));
				for (int j = 0; j < pythonOutpList.size(); j++) {
					String outp = String.valueOf(j)+": ";
					if (j == i) outp += "[>] ";
					else if (activeThreads[j]) outp += "[*] ";
					else outp += "[ ] ";
					outp += String.valueOf(pythonOutpList.get(j).size());
					if (pythonOutpList.get(j).size() > 0)
						outp += " ("+pythonOutpList.get(j).get(0)[0]+")";
					System.out.println(outp);
				}
				System.out.println(); */
				
				if (currList.get(0).length == 0) {
					pythonOutpList.get(i).clear();
					continue;
				}
				FECallCluster cc = new FECallCluster(currList.size(), feControl.getParams().featureList.length);
				try {
					cc.clusterID = currList.get(0)[0];
					for (int j = 0; j < currList.size(); j++) {
						cc.uids[j] = Long.valueOf(currList.get(j)[1]);
						int index = 2;
						if (feControl.getParams().inputFromWMATorMTSF && feControl.getParams().inputFilesAreMTSF()) {
							String location = currList.get(j)[index++];
							cc.locations[j] = location.substring(1, location.length()-1);
						}
						cc.datetimes[j] = Long.valueOf(currList.get(j)[index++]);
						cc.durations[j] = (int) Double.valueOf(currList.get(j)[index++]).doubleValue();
						cc.lfs[j] = (int) Double.valueOf(currList.get(j)[index++]).doubleValue();
						cc.hfs[j] = (int) Double.valueOf(currList.get(j)[index++]).doubleValue();
						if (feControl.getParams().inputFilesAreMTSF()) {
							String label = currList.get(j)[index++];
							cc.labels[j] = label.substring(1, label.length()-1);
						}
						for (int k = index; k < currList.get(j).length; k++) {
							cc.featureVector[j][k-index] = Double.valueOf(currList.get(j)[k]);
						}
					}
					if (cc.uids.length > 0) {
						FEDataUnit du = new FEDataUnit(feControl, cc);
						feControl.feProcess.addVectorData(du);
						for (int j = 0; j < cc.uids.length; j++) {
							remainingUIDs.remove(String.valueOf(cc.uids[j])); // TODO
							feControl.subtractOneFromPendingCounter();
							feControl.addOneToCounter(FEPanel.SUCCESS, String.valueOf(cc.uids[j]));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					for (int j = 0; j < currList.size(); j++) {
						remainingUIDs.remove(currList.get(j)[1]); // TODO
						feControl.subtractOneFromPendingCounter();
						feControl.addOneToCounter(FEPanel.FAILURE, currList.get(j)[1]);
					}
				}
				deleteFilesAfterProcessing(cc);
				pythonOutpList.get(i).clear();
			}
		} } // synchronized
	}
	
	public void signalPAMHasStarted() {
		pamHasStopped = false;
	}
	
	public void signalPAMHasStopped() {
		pamHasStopped = true;
	}
	
	protected void deleteFilesAfterProcessing(FECallCluster cc) {
		new File(feControl.getParams().tempFolder+"NR_"+cc.clusterID.replace("-", "_")+".wav").delete();
		String toDelete = feControl.getParams().tempFolder+"FE_"+cc.clusterID.replace("-", "_")+"_";
		for (int i = 0; i < cc.getSize(); i++) {
			try { // In case UIDs were not parsed correctly for whatever reason.
				new File(toDelete+cc.uids[i]+".wav").delete();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Re-initializes the "txtParams" variable, generally under the assumption that the
	 * Feature Extractor's settings have changed.
	 */
	protected void resetTxtParams() {
		try {
			String pyParams = feControl.getParams().outputPythonParamsToText();
	        if (pyParams.length() > 0) {
	        	pythonCommand("txtParams = "+pyParams);
	        } else {
	        	pythonCommand("txtParams = []");
	        }
		} catch (Exception e) {
			System.out.println("Exception in resetTxtParams().");
		}
	}
	
	/**
	 * Forces the print threads and runner thread to (re)start.
	 */
	public void startPrintThreads() {
		printThreadsActive = true;
		ipt = new InputPrintThread();
		ept = new ErrorPrintThread();
		rt = new RunnerThread();
		ipt.start();
		ept.start();
		rt.start();
	}
	
	/**
	 * Doesn't work. Needs to be fixed at some point.
	 */
	@Deprecated
	public void stopPrintThreads() {
		printThreadsActive = false;
		try {
			ipt.join();
			ept.join();
			rt.join();
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	protected void signalPythonError(String outpstr) {
		String uid = outpstr.substring(outpstr.indexOf("Error: Could not process ")+25);
		boolean breakLoop = false;
		synchronized(ccList) {
			for (int i = 0; i < ccList.size(); i++) {
				for (int j = 0; j < ccList.get(i).size(); j++) {
					if (ccList.get(i).get(j).uid.equals(uid)) {
						ccList.get(i).remove(j);
						breakLoop = true;
						break;
					}
				}
				if (breakLoop) {
					break;
				}
			}
		}
		if (breakLoop) {
			remainingUIDs.remove(uid); // TODO
			boolean subd = feControl.subtractOneFromPendingCounter();
			feControl.addOneToCounter(FEPanel.FAILURE, uid);
		}
	}
	
	protected void signalImportError(String missingLibrary) {
		feControl.simpleErrorDialog("Warning: The Feature Extractor's Python script was unable to import "+missingLibrary+", "
				+ "so it may not work properly. To fix this, try running the .bat file that comes with the MIRFEE plugin, or manually "
				+ "installing that library via pip.", 300);
	}
	
	/**
	 * Passes non-error Python output back to Java.
	 */
	protected class InputPrintThread extends Thread {
		protected InputPrintThread() {}
		@Override
		public void run() {
			while(printThreadsActive) {
				try {
					String outpstr = "";
					if (br.ready()) {
						while ((outpstr = br.readLine()) != null) {
							if (feControl.getParams().miscPrintOutputChecked)
								System.out.println("FE IBR: "+outpstr);
							boolean boo = processPythonOutput(outpstr);
							if (outpstr.contains("Error: Could not process ")) {
								signalPythonError(outpstr);
							}
						}
					}
				} catch (IOException e) {
					// TODO
				}
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (Exception e) {
					System.out.println("Sleep exception.");
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Passes Python error output back to Java.
	 */
	protected class ErrorPrintThread extends Thread {
		protected ErrorPrintThread() {}
		@Override
		public void run() {
			while(printThreadsActive) {
				try {
					String outpstr = "";
					if (ebr.ready()) {
						while (ebr.ready() && (outpstr = ebr.readLine()) != null) {
							System.out.println("FE EBR: "+outpstr);
							if (outpstr.contains("NameError: name ")) {
								printThreadsActive = false;
								active = false;
								break;
							}
							String[] tokens = outpstr.split(" ");
						/*	if (tokens.length > 0) {
								if (tokens[0].equals(">>>")) {
									break;
								}
							} */
							if (outpstr.contains("Error: Could not process ")) {
								signalPythonError(outpstr);
							}
							if (outpstr.startsWith("ModuleNotFoundError")) {
								signalImportError(outpstr.substring(36));
							}
						}
					}
				} catch (IOException e) {
					// TODO
				}
				try {
					TimeUnit.MILLISECONDS.sleep(100);
				} catch (Exception e) {
					System.out.println("Sleep exception.");
					e.printStackTrace();
				}
			}
		}
	}
	
	public FEControl getControl() {
		return feControl;
	}
	
	/**
	 * Checks if the thread manager is running or not. Generally should return true.
	 */
	public boolean isActive() {
		return active;
	}
	
	/**
	 * Attempts to extract the Python script from the .jar file and sets the thread manager to active if it worked.
	 * @return Whether or not the JarExtractor succeeded.
	 */
	public boolean setActive() {
		active = new MIRFEEJarExtractor().extract("src/mirfeeFeatureExtractor/FEPythonThread.py",
				feControl.getParams().tempFolder, "FEPythonThread.py", true);
		if (feControl.getParams().miscPrintJavaChecked)
			System.out.println("JarExtractor completed.");
		return active;
	}
	
	/**
	 * Deactivates the threads. Exercise caution.
	 */
	public void setInactive() {
		active = false;
	}
}