# LCPythonScript.py (for MIRFEE Classifier plugin for PAMGuard)
# By Holly LeBlond

import numpy as np
import os
import sys
from datetime import datetime
from datetime import timedelta
import pytz
import traceback
import warnings
import random

import pandas as pd
from sklearn.feature_selection import SelectKBest, f_classif
from sklearn.ensemble import RandomForestClassifier
from sklearn.ensemble import ExtraTreesClassifier
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.discriminant_analysis import LinearDiscriminantAnalysis

class LCModel():
    def __init__(self, trainFN: str, txtParams: list, excludeIDs: list, excludeFolds: list, doBestFeatures: bool):
        try:
            #np.set_printoptions(legacy='1.25')
            self.trainFN = trainFN
            self.excludeIDs = excludeIDs
            self.excludeFolds = excludeFolds;
            #self.validation = txtParams[0] #str
            self.kNum = txtParams[0] #int
            self.selectKBest = txtParams[1] #boolean
            self.kBest = txtParams[2] #int
            self.samplingLimits = txtParams[3] #str
            self.maxSamples = txtParams[4] #int
            self.limitClusterSize = txtParams[5] #boolean
            self.maxClusterSize = txtParams[6] #int
            self.labels = txtParams[7] #list
            self.sortedLabels = np.sort(self.labels).tolist()
            self.classifierName = txtParams[8] #str
            self.nEstimators = txtParams[9] #int
            self.criterion = txtParams[10] #str
            self.hasMaxDepth = txtParams[11] #boolean
            self.maxDepth = txtParams[12] #int
            self.maxFeaturesMode = txtParams[13] #str
            self.maxFeatures = txtParams[14] #int
            self.bootstrap = txtParams[15] #boolean
            self.classWeightMode = txtParams[16] #str
            self.classWeights = txtParams[17] #list
            self.learningRate = txtParams[18] #float
            self.maxIterations = txtParams[19] #int
            self.minClusterSize = txtParams[20] #int
            self.displayIgnored = txtParams[21] #boolean
            self.featureList = txtParams[22] #list
            
            self.containsClusters = {}
            
            self.modelList = []
            self.featureIndices = []
            X, y = self.createXy()
            if self.selectKBest:
                skb = SelectKBest(f_classif, k=self.kBest).fit(X,y)
                currIndices = skb.get_support(indices=True).tolist()
                X = [[row[num] for num in currIndices] for row in X]
                self.featureIndices.append(currIndices)
            else:
                self.featureIndices.append(np.arange(len(X[0])).tolist())
            self.modelList.append(self.createFittedModel(X,y))
            if doBestFeatures:
                self.printBestFeatureOrder()
            print("Initialization succeeded", flush=True)
        except:
            print("Initialization failed", flush=True, file=sys.stderr)
            traceback.print_exc()
            
    def createXy(self):
        skipLine = -1
        with open(self.trainFN) as f:
            lines = [line for line in f]
            for i in np.arange(len(lines)):
                if "cluster,uid,location,date,duration,lf,hf,label," in lines[i]:
                    skipLine = i
                    break
        if skipLine == -1:
            raise Exception("Input training/testing set does not contain an appropriate header.")
        trainList = pd.read_csv(self.trainFN, skiprows=np.arange(i)).values.tolist()
        clusterList = []
        clusterSizes = {}
        for i in np.arange(len(trainList)):
            if trainList[i][0] not in clusterList:
                clusterList.append(trainList[i][0])
                clusterSizes.update({trainList[i][0]: 1})
            else:
                clusterSizes.update({trainList[i][0]: clusterSizes.get(trainList[i][0]) + 1})
        # NOTE that this if-statement can cause an exception by removing all instances of a class from all or all but one subset.
        # (This is scenario is very unlikely though.)
        if self.limitClusterSize and self.maxClusterSize > 0:
            random.shuffle(trainList)
            num = 0
            while num < len(trainList):
                if clusterSizes.get(trainList[num][0]) > self.maxClusterSize:
                    clusterSizes.update({trainList[num][0]: clusterSizes.get(trainList[num][0]) - 1})
                    trainList.pop(num)
                else:
                    num += 1
        #clusterList.sort()
        X = []
        y = []
        for i in np.arange(len(trainList)):
            row = trainList[i]
            testNum = self.kNum*clusterList.index(row[0])/len(clusterList)
            #print(str(self.kNum))
            #print(str(i)+": "+str(testNum)+" -> "+str(np.floor(testNum)))
            if row[0][:2] not in self.excludeIDs and row[0][:1] not in self.excludeIDs \
            and int(np.floor(self.kNum*clusterList.index(row[0])/len(clusterList))) not in self.excludeFolds:
                X.append(row[8:])
                y.append(row[7])
                if row[0] not in self.containsClusters:
                    self.containsClusters[row[0]] = [row[3]]
                else:
                    dateList = self.containsClusters.get(row[0])
                    dateList.append(row[3])
                    self.containsClusters.update({row[0]: dateList})
        #print("containsClusters: "+str(len(self.containsClusters)))
        if self.samplingLimits == "duplicate":
            X, y = self.growSet(X, y)
        elif self.samplingLimits != "none":
            X, y = self.shrinkSet(X, y)
        return X, y
    
    def shrinkSet(self, X, y):
        allocX = [[] for val in self.labels]
        for i in np.arange(len(y)):
            allocX[self.labels.index(y[i])].append(X[i])
        limit = np.min([len(arr) for arr in allocX])
        if limit == 0:
            # Throw error
            pass
        if self.samplingLimits == "setmax":
            limit = self.maxSamples
        X = []
        y = []
        for i in np.arange(len(allocX)):
            while len(allocX[i]) > limit:
                allocX[i].pop(np.random.randint(len(allocX[i])))
            for row in allocX[i]:
                X.append(row)
                y.append(self.labels[i])
        return X, y
    
    def growSet(self, X, y):
        allocX = [[] for val in self.labels]
        for i in np.arange(len(y)):
            allocX[self.labels.index(y[i])].append(X[i])
        limit = np.max([len(arr) for arr in allocX])
        if limit == 0:
            # Throw error
            pass
        for i in np.arange(len(allocX)):
            np.random.shuffle(allocX[i])
            count = len(allocX[i])
            while count < limit:
                X.append(allocX[i][np.mod(count,len(allocX[i]))])
                y.append(self.labels[i])
                count += 1
        return X, y
        
    def createFittedModel(self, X, y):
        model = None
        tempMaxDepth = None
        if self.hasMaxDepth:
            tempMaxDepth = self.maxDepth
        if self.classifierName == "HistGradientBoostingClassifier":
            model = HistGradientBoostingClassifier(learning_rate=self.learningRate, max_iter=self.maxIterations, max_depth=tempMaxDepth)
        elif self.classifierName == "LinearDiscriminantAnalysis":
            model = LinearDiscriminantAnalysis()
        else:
            tempCriterion = "gini"
            if self.criterion == "Log. loss":
                tempCriterion = "log_loss"
            elif self.criterion == "Entropy":
                tempCriterion = "entropy"
            tempMaxFeatures = self.maxFeatures
            if self.maxFeaturesMode == "Square root":
                tempMaxFeatures = "sqrt"
            elif self.maxFeaturesMode == "Log2":
                tempMaxFeatures = "log2"
            tempClassWeight = None
            if self.classWeightMode == "Balanced":
                tempClassWeight = "balanced"
            elif self.classWeightMode == "Balanced subsample":
                tempClassWeight = "balanced_subsample"
            elif self.classWeightMode == "Custom":
                try:
                    tempClassWeight = {self.labels[i]:float(self.classWeights[i]) for i in np.arange(len(self.labels))}
                except:
                    tempClassWeight = None
                    # Print something?
            if self.classifierName == "RandomForestClassifier":
                model = RandomForestClassifier(n_estimators=self.nEstimators, criterion=tempCriterion, max_depth=tempMaxDepth, max_features=tempMaxFeatures, \
                                               bootstrap=self.bootstrap, class_weight=tempClassWeight)
            else:
                model = ExtraTreesClassifier(n_estimators=self.nEstimators, criterion=tempCriterion, max_depth=tempMaxDepth, max_features=tempMaxFeatures, \
                                               bootstrap=self.bootstrap, class_weight=tempClassWeight)
        model.fit(X,y)
        return model
        
    def predictCluster(self, testList):
        with warnings.catch_warnings():
            warnings.filterwarnings("ignore")
            if not (len(testList) < self.minClusterSize and not self.displayIgnored):
                try:
                    currModel = self.modelList[0]
                    accMatrix = [[0 for a in np.arange(5)] for b in np.arange(len(self.labels))]
                    probaList = []
                    outp = []
                    for entry in testList:
                        #testEntry = entry[7:][0]
                        testEntry = entry[8]
                        ua = currModel.predict_proba([[testEntry[num] for num in self.featureIndices[0]]])[0]
                        predictionArr = [ua[self.sortedLabels.index(label)] for label in self.labels]
                        subOutp = entry[:8]
                        subOutp.append(predictionArr)
                        outp.append(subOutp)
                    #print("outp: "+str(outp))
                    #outp = np.transpose(outp)
                    outp = [[outp[j][i] for j in np.arange(len(outp))] for i in np.arange(len(outp[0]))] # No idea why np.transpose doesn't work.
                    outpStr = str(outp[0])
                    for row in outp[1:]:
                        outpStr += "|" + str(row)
                    outpStr = outpStr.replace("\n", "")
                    print("RESULT: "+outpStr, flush=True)
                except:
                    print("Error encountered while attempting to process cluster "+str(testList[0][0])+".", flush=True, file=sys.stderr)
                    traceback.print_exc()
            else:
                print("Cluster ignored due to settings: "+str(testList[0][0]), flush=True)

    def checkIfInTrainingSet(self, testEntry):
        if testEntry[0] in self.containsClusters:
            dateList = self.containsClusters.get(testEntry[0])
            #print("ciits: "+testEntry[0]+", "+str(dateList))
            if testEntry[3] in dateList:
                return True
        return False

    def printBestFeatureOrder(self):
        X, y = self.createXy()
        skb = SelectKBest(f_classif, k=len(self.featureList)).fit(X,y)
        scores = skb.scores_
        p_values = skb.pvalues_
        currIndices = skb.get_support(indices=True).tolist()
        X_T = np.transpose(X)
        corr_scores = {}
        for i in np.arange(len(currIndices)):
            score_arr = []
            for j in np.arange(len(currIndices)):
                if i == j:
                    score_arr.append(0.0)
                else:
                    score_arr.append(np.abs(np.corrcoef(X_T[currIndices[i]], X_T[currIndices[j]])[0][1]))
            corr_scores[self.featureList[currIndices[i]]] = score_arr
        outp = "BESTFEATUREORDER: "
        for i in np.arange(len(currIndices)):
            feature_name = self.featureList[currIndices[i]]
            score_arr = corr_scores[feature_name]
            corr_feat = self.featureList[currIndices[np.argmax(score_arr)]]
            max_corr = np.max(score_arr)
            outp += "("+self.featureList[currIndices[i]]+", "+str(scores[i])+", "+str(p_values[i])+", "+corr_feat+", "+str(max_corr)+")"
            if (i < len(currIndices)-1):
                outp += ", "
        print(outp, flush=True)

class ModelManager():
    def __init__(self):
        #np.set_printoptions(legacy='1.25')
        self.modelList = []
        
    def addModel(self, newModel: LCModel):
        self.modelList.append(newModel)
    
    def clearModelList(self):
        self.modelList = []
    
    def predictCluster(self, testList):
        if len(self.modelList) == 0:
            print("ERROR - ModelManager is empty.", flush=True, file=sys.stderr)
            return
        for i in np.arange(len(self.modelList)):
            model = self.modelList[i]
            modelIsInvalid = False
            for testEntry in testList:
                #print("checkIfInTrainingSet: "+str(model.checkIfInTrainingSet(testEntry)))
                if model.checkIfInTrainingSet(testEntry):
                    modelIsInvalid = True
                    break
            if modelIsInvalid:
                continue
            #print("model i: "+str(i))
            model.predictCluster(testList)
            return
        print("ERROR - Input test entry found in all training sets: "+testList[0][0]+", "+testList[0][2]+", "+str(len(testList)), flush=True, file=sys.stderr)
        
    def printBestFeatureOrder(self):
        if len(self.modelList) == 0:
            print("ERROR - ModelManager is empty.", flush=True, file=sys.stderr)
            return
        self.modelList[0].printBestFeatureOrder()
        
    def runLast(self):
        print("RUNLAST", flush=True)
