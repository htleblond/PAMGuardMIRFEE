# FEPythonThread.py (for MIRFEE Classifier plugin for PAMGuard)
# By Holly LeBlond

import numpy as np
import librosa
from librosa import display
from librosa import core
import parselmouth
#import soundfile as sf
#import matplotlib.pyplot as plt
import scipy
import scipy.signal as signal
from scipy import fft
import os
import sys
#import IPython.display as ipd

from datetime import datetime
from datetime import timedelta
import pytz
import traceback

#import matplotlib.markers

import logging
import threading
#import multiprocessing
import time
import warnings
from builtins import FileNotFoundError

# Basically, an instance of this should be created in the Python interpreter
# for each cluster, and sound clips should be fed into it via addClip.
class FEThread():
    def __init__(self, y_nr_name: str, y_nr: np.ndarray, txtParams: list):
        #print("In thread __init__.")
        #np.set_printoptions(legacy='1.25')
        self.active = True
        self.clipList = []
        self.y_nr = y_nr
        self.y_nr_name = y_nr_name
        #self.txtParams = txtParams
        self.inputType = txtParams[0] # "detector", "wmat", or "mtsf"
        self.sr = int(txtParams[1])
        self.outputDecimalLimit = txtParams[2]
        self.audioAutoClipLength = txtParams[3]
        self.audioClipLength = txtParams[4]
        self.audioSTFTLength = txtParams[5]
        self.audioHopSize = txtParams[6]
        self.audioWindowFunction = txtParams[7]
        self.audioNormalizeChecked = txtParams[8]
        self.audioHPFChecked = txtParams[9]
        self.audioHPFThreshold = txtParams[10]
        self.audioHPFMagnitude = txtParams[11]
        self.audioLPFChecked = txtParams[12]
        self.audioLPFThreshold = txtParams[13]
        self.audioLPFMagnitude = txtParams[14]
        self.audioNRChecked = txtParams[15]
        self.audioNRStart = txtParams[16]
        self.audioNRLength = txtParams[17]
        self.audioNRScalar = txtParams[18]
        self.features = txtParams[19]
        self.featureProcessList, self.featureProcessIndexes = self.featuresToProcess(self.features)
        if self.audioNRChecked and len(y_nr) > 0:
            self.y_nr_stft = librosa.stft(y_nr, n_fft=self.audioSTFTLength, hop_length=self.audioHopSize, \
                                          win_length=self.audioSTFTLength, window='rectangular')
            self.y_nr_avg = [np.mean(x)*self.audioNRScalar for x in np.abs(self.y_nr_stft)]
        #print("About to start thread.")
        #thread = threading.Thread(target=self.threadFunc(), args=(1,))
        #thread.start()
    
    # Loads a clip for processing.
    def addClip(self, fn:str, uid, datelong, amplitude, duration, freqhd_min, freqhd_max, slice_data, \
                pe_cluster_id="", pe_location="", pe_label="", pe_header_features={}): # "pe" stands for "pre-existing", implying it came from a pre-existing .mtsf file
        #print("Reached addClip().")
        self.clipList.append(fn)
        headerData = HeaderData(uid, datelong, amplitude, duration, freqhd_min, freqhd_max, slice_data, \
                                pe_cluster_id, pe_location, pe_label, pe_header_features)
        thread = threading.Thread(target=self.threadFunc, args=(fn,headerData,))
        #thread = multiprocessing.Process(target=self.threadFunc, args=(fn,headerData,))
        thread.start()
        #print("Thread "+str(uid)+" has finished.", flush=True)
    
    # The processing thread's function.
    def threadFunc(self, fn: str, headerData):
        #print("Reached threadFunc().")
        if self.audioNRChecked and len(self.y_nr) == 0:
            print("Error: Could not process "+str(headerData.uid), flush=True, file=sys.stderr)
            #if os.path.exists(fn):
            #    os.remove(fn)
            return
        try:
            outp = self.extractFeatures(fn, headerData)
            print("outp: "+str(outp), flush=True)
            self.clipList.pop(0);
        except Exception:
            #print("Error: Could not process "+str(extras[0]), file=sys.stderr)
            print("Error: Could not process "+str(headerData.uid), flush=True, file=sys.stderr)
            print(traceback.format_exc(), flush=True, file=sys.stderr)
       # if os.path.exists(fn):
       #     os.remove(fn)
    
    # Loads and manipulates the audio before sending it to the functions
    # that perform the feature extraction.
    def extractFeatures(self, fn: str, headerData):
        self.sr = librosa.core.get_samplerate(fn)
        y = loadAudio(fn, self.sr)
        y_orig = y
        y_stft = librosa.stft(y, n_fft=self.audioSTFTLength, hop_length=self.audioHopSize, win_length=self.audioSTFTLength, \
                              window=self.getWindowName(self.audioWindowFunction))
        maxval = np.max(np.abs(y_stft))
        if self.audioNRChecked:
            for i in np.arange(len(y_stft)):
                for j in np.arange(len(y_stft[i])):
                    if self.y_nr_avg[i] > np.abs(y_stft[i][j]):
                        y_stft[i][j] *= 0
                    elif self.y_nr_avg[i] > 0 and np.abs(y_stft[i][j]) > 0:
                        y_stft[i][j] *= (np.abs(y_stft[i][j]) - self.y_nr_avg[i])/np.abs(y_stft[i][j])
        if self.audioNormalizeChecked and np.max(np.abs(y_stft)) > 0:
            y_stft *= 1/np.max(np.abs(y_stft))
        y = librosa.istft(y_stft, hop_length=self.audioHopSize, win_length=self.audioSTFTLength, \
                          window=self.getWindowName(self.audioWindowFunction))
        if self.audioHPFChecked:
            hpf = signal.butter(self.audioHPFMagnitude, self.audioHPFThreshold, 'highpass', fs=self.sr, output='sos')
            y = signal.sosfilt(hpf, y)
        if self.audioLPFChecked:
            lpf = signal.butter(self.audioLPFMagnitude, self.audioLPFThreshold, 'lowpass', fs=self.sr, output='sos')
            y = signal.sosfilt(lpf, y)
        if self.audioHPFChecked or self.audioLPFChecked:
            y_stft = librosa.stft(y, n_fft=self.audioSTFTLength, hop_length=self.audioHopSize, win_length=self.audioSTFTLength, \
                                  window=self.getWindowName(self.audioWindowFunction))
            if self.audioNormalizeChecked and np.max(np.abs(y_stft)) > 0:
                y_stft *= 1/np.max(np.abs(y_stft))
        
        #TEST CODE - DELETE AFTERWARDS
        #newFN = str(headerData.uid)
        #sf.write("C:/Users/wtleb/Desktop/MIRFEE export (delete)/cliptest/"+newFN+"_NR.wav", self.y_nr, self.sr, 'PCM_24')
        #sf.write("C:/Users/wtleb/Desktop/MIRFEE export (delete)/cliptest/"+newFN+"_Orig.wav", y_orig, self.sr, 'PCM_24')
        #sf.write("C:/Users/wtleb/Desktop/MIRFEE export (delete)/cliptest/"+newFN+"_New.wav", y, self.sr, 'PCM_24')
        
        #y_mag = librosa.amplitude_to_db(np.abs(y_stft), ref=np.max)
        preCalcFeatures = []
        for feature in self.featureProcessList:
            preCalcFeatures.append(self.preCalculateFeature(feature, y, y_stft))
        outp = []
        if self.inputType == "mtsf":
            outp = [headerData.pe_cluster_id, headerData.uid, headerData.pe_location, headerData.datelong, headerData.duration, \
                    headerData.freqhd_min, headerData.freqhd_max, headerData.pe_label]
        else: # self.inputType == "detector"
            outp = [headerData.pe_cluster_id, headerData.uid, headerData.datelong, headerData.duration, headerData.freqhd_min, headerData.freqhd_max]
        for i in np.arange(len(self.features)):
            outp_val = 0.0
            if self.featureProcessIndexes[i] == -1:
                outp_val = self.extractIndividualFeature(self.features[i], [], headerData)
            else:
                outp_val = self.extractIndividualFeature(self.features[i], preCalcFeatures[self.featureProcessIndexes[i]], headerData)
            if self.outputDecimalLimit:
                outp_val = self.limitTo3Decimals(outp_val)
            outp.append(outp_val)
        return outp
    
    def limitTo3Decimals(self, outp_val):
        if outp_val == 0:
            return 0
        split = str(outp_val).split(".")
        if outp_val >= 1 and len(split) == 2 and len(split[1]) > 3:
            return float("{:.3f}".format(outp_val))
        elif outp_val < 1:
            first_nonzero = 0
            while split[1][first_nonzero] == "0":
                first_nonzero += 1
            format_str = "{:."+str(first_nonzero+3)+"f}"
            return float(format_str.format(outp_val))
        return outp_val
    
    # Parses through input settings and creates a list of what to process, while making sure it doesn't process the same thing if used by the
    # same feature with marginally different settings.
    def featuresToProcess(self, features: list):
        outp = []
        outpIndexes = []
        for i in np.arange(len(features)):
            curr = ""
            tokens = features[i].split("_")
            if tokens[0] in ["amplitude","duration","freqhd","frange","fslopehd", \
                             "freqsd","freqsdd1","freqsdd2","freqsdelbow","freqsdslope", \
                             "wcfreqs","wcslopes","wccurves"]:
                # These are just calculated from input header or slice data.
                pass
            elif tokens[0] in ["rms","centroid","flux","zcr"]:
                curr = tokens[0]
            elif tokens[0] in ["mfcc","poly","flatness","rolloff"]:
                curr = tokens[0]+"_"+tokens[1]
            elif tokens[0] in ["bandwidth","specmag","praat","hcentroid"]:
                curr = tokens[0]+"_"+tokens[1]+"_"+tokens[2]
            elif tokens[0] in ["contrast"]:
                curr = tokens[0]+"_"+tokens[1]+"_"+tokens[2]+"_"+tokens[3]+"_"+tokens[4]
            elif tokens[0] in ["formantcount"]:
                if tokens[4] == "fullclip":
                    curr = "formantsfc_"+tokens[1]+"_"+tokens[2]+"_"+tokens[3]
                else:
                    curr = "formants_"+tokens[1]+"_"+tokens[2]+"_"+tokens[3]
            elif tokens[0] in ["formantfreq","formantdiff","formantratio"]:
                if tokens[5] == "fullclip":
                    curr = "formantsfc_"+tokens[1]+"_"+tokens[2]+"_"+tokens[3]
                else:
                    curr = "formants_"+tokens[1]+"_"+tokens[2]+"_"+tokens[3]
            elif tokens[0] in ["thd","hbr","behc","hfr"]:
                curr = "harms_"+tokens[1]+"_"+tokens[2]+"_"+tokens[3]
                if tokens[0] == "hfr":
                    for prev in outp:
                        ptokens = prev.split("_")
                        if len(ptokens) >= 4 and ptokens[0] == "harms" and ptokens[2] == tokens[2] \
                        and ptokens[3] == tokens[3] and ptokens[1] >= tokens[1]:
                            curr = prev
                            break
            if len(curr) == 0:
                outpIndexes.append(-1)
            elif curr not in outp:
                outp.append(curr)
                outpIndexes.append(len(outp)-1)
            else:
                outpIndexes.append(outp.index(curr))
        return outp, outpIndexes
    
    # Performs the actual feature extraction. Designed such that instances where
    # the actual processing is the same will use the same output without having
    # to calculate it more than once.
    def preCalculateFeature(self, feature: str, y, y_stft):
        tokens = feature.split("_")
        #print(str(tokens), flush=True)
        if tokens[0] in ["formants","formantsfc"]:
            # Kudos: https://www.mathworks.com/help/signal/ug/formant-estimation-with-lpc-coefficients.html
            # and also: https://support.ircam.fr/docs/AudioSculpt/3.0/co/LPC_1.html
            order = int(np.ceil(self.sr/(int(tokens[1])*0.25)))
            y_frames = []
            curr_hop = 0
            outp = []
            if tokens[0] == "formantsfc":
                y_frames = [y]
            else:
                while curr_hop + self.audioSTFTLength < len(y):
                    y_frames.append(y[curr_hop:curr_hop+self.audioSTFTLength])
                    curr_hop += self.audioHopSize
            for y_frame in y_frames:
                windowed = y_frame * librosa.filters.get_window('hamming', len(y_frame))
                filtered = scipy.signal.lfilter([1], [1, 0.63], windowed)
                lpccs = [x for x in librosa.lpc(filtered, order=order)]
                skip = False
                for x in lpccs:
                    if np.isnan(x) or np.isinf(x):
                        skip = True # Frame will be skipped
                        break
                if skip:
                    continue
                roots = [x for x in np.roots(lpccs) if np.imag(x) >= 0]
                freqs = [x*(self.sr/(2*np.pi)) for x in np.arctan2(np.imag(roots),np.real(roots))]
                stuff = [[freqs[i],roots[i]] for i in np.arange(len(freqs))]
                stuff.sort()
                #bandwidths = [-1/2*(self.sr/(2*np.pi))*np.log(np.abs(x[1])) for x in stuff]
                bandwidths = []
                for x in stuff:
                    if np.abs(x[1]) > 0:
                        bandwidths.append(-0.5*(self.sr/(2*np.pi))*np.log(np.abs(x[1])))
                    else:
                        bandwidths.append(float(tokens[3])) # discounts it in case of log 0
                outp.append([stuff[i][0] for i in np.arange(len(stuff)) if stuff[i][0] > float(tokens[2]) and bandwidths[i] < float(tokens[3])])
            return outp
        elif tokens[0] == "mfcc":
            #return librosa.feature.mfcc(y=y, sr=sr, n_mfcc=int(tokens[1]), n_fft=audioSTFTLength)
            return librosa.feature.mfcc(y=y, sr=self.sr, n_mfcc=int(tokens[1]))
        elif tokens[0] == "poly":
            return librosa.feature.poly_features(y=y, sr=self.sr, order=int(tokens[1]), n_fft=self.audioSTFTLength, hop_length=self.audioHopSize)
        elif tokens[0] == "rms":
            return librosa.feature.rms(y=y)
        elif tokens[0] == "bandwidth":
            if tokens[2] == "ny":
                return librosa.feature.spectral_bandwidth(y=y, sr=self.sr, p=float(tokens[1]), norm=True, n_fft=self.audioSTFTLength, \
                                                          hop_length=self.audioHopSize)
            else:
                return librosa.feature.spectral_bandwidth(y=y, sr=self.sr, p=float(tokens[1]), norm=False, n_fft=self.audioSTFTLength, \
                                                          hop_length=self.audioHopSize)
        elif tokens[0] == "centroid":
            return librosa.feature.spectral_centroid(y=y, sr=self.sr, n_fft=self.audioSTFTLength, hop_length=self.audioHopSize, \
                                                     window=self.getWindowName(self.audioWindowFunction))
        elif tokens[0] == "contrast":
            if tokens[4] == "lin":
                return librosa.feature.spectral_contrast(y=y, sr=self.sr, fmin=float(tokens[1]), n_bands=int(tokens[2]), linear=True, \
                                                     n_fft=self.audioSTFTLength, hop_length=self.audioHopSize, \
                                                     window=self.getWindowName(self.audioWindowFunction))
            else:
                return librosa.feature.spectral_contrast(y=y, sr=self.sr, fmin=float(tokens[1]), n_bands=int(tokens[2]), linear=False, \
                                                     n_fft=self.audioSTFTLength, hop_length=self.audioHopSize, \
                                                     window=self.getWindowName(self.audioWindowFunction))
        elif tokens[0] == "flatness":
            return librosa.feature.spectral_flatness(y=y, power=float(tokens[1]), \
                                                     n_fft=self.audioSTFTLength, hop_length=self.audioHopSize, \
                                                     window=self.getWindowName(self.audioWindowFunction))
        elif tokens[0] == "flux":
            return librosa.onset.onset_strength(y=y, sr=self.sr)
        elif tokens[0] == "specmag":
            return np.abs(y_stft)[int(self.audioSTFTLength*(int(tokens[1])/self.sr)):int(self.audioSTFTLength*(int(tokens[2])/self.sr))]
        elif tokens[0] == "rolloff":
            return librosa.feature.spectral_rolloff(y=y, sr=self.sr, roll_percent=float(tokens[1])/100,\
                                                     n_fft=self.audioSTFTLength, hop_length=self.audioHopSize, \
                                                     window=self.getWindowName(self.audioWindowFunction))
        elif tokens[0] == "praat":
            #yin = librosa.yin(y, fmin=int(tokens[1]), fmax=int(tokens[2]), sr=self.sr, \
            #                   frame_length=2*self.audioSTFTLength, hop_length=self.audioHopSize, win_length=self.audioSTFTLength)
            snd = parselmouth.Sound(y, sampling_frequency=self.sr)
            praat = snd.to_pitch(time_step=self.audioHopSize/self.sr, pitch_floor=int(tokens[1]), pitch_ceiling=int(tokens[2]))
            outp = [x[0] for x in praat.to_array()[0] if x[0] > 0.0]
            if len(outp) == 0:
                return [0.0]
            return outp
        elif tokens[0] == "harms":
            #yin_orig = librosa.yin(y, fmin=int(tokens[2]), fmax=int(tokens[3]), sr=self.sr, \
            #                   frame_length=2*self.audioSTFTLength, hop_length=self.audioHopSize, win_length=self.audioSTFTLength)
            snd = parselmouth.Sound(y, sampling_frequency=self.sr)
            time_step = self.audioHopSize/self.sr
            praat = snd.to_pitch(time_step=time_step, pitch_floor=int(tokens[2]), pitch_ceiling=int(tokens[3]))
            praat = [x[0] for x in praat.to_array()[0]]
            nh = int(tokens[1])
            y_fft_arr = []
            j = 0
            # Not sure what the window size is, so it assumes it's the same as the time step.
            while (j+1)*int(self.sr*time_step) <= len(y):
                y_fft_arr.append(fft.fft(y[j*int(self.sr*time_step):(j+1)*int(self.sr*time_step)], n=self.sr)[:int(self.sr/2)])
                j += 1
            freqs_mags = []
            fft_mag_outp = []
            for i in np.arange(len(praat)):
                if praat[i] == 0.0:
                    continue
                frame = []
                for j in np.arange(nh)+1:
                    if int(np.round(praat[i]*j)) < self.sr/2:
                        frame.append([praat[i]*j, np.abs(y_fft_arr[i][int(np.round(praat[i]*j))])])
                    else:
                        frame.append([praat[i]*j, 0.0])
                freqs_mags.append(frame)
                fft_mag_outp.append(np.abs(y_fft_arr[i]))
            return [freqs_mags, fft_mag_outp]
        elif tokens[0] == "hcentroid":
            snd = parselmouth.Sound(y, sampling_frequency=self.sr)
            time_step = self.audioHopSize/self.sr
            praat = snd.to_pitch(time_step=time_step, pitch_floor=int(tokens[1]), pitch_ceiling=int(tokens[2]))
            praat = [x[0] for x in praat.to_array()[0]]
            centroid = librosa.feature.spectral_centroid(y=y, sr=self.sr, n_fft=self.audioSTFTLength, hop_length=self.audioHopSize, \
                                                     window=self.getWindowName(self.audioWindowFunction))[0]
            while len(praat) < len(centroid)-1:
                praat.insert(0, 0.0) # This is to align Praat with the centroids - graphing them out seems to imply that this is the best alignment.
            return [praat, centroid]
        elif tokens[0] == "zcr":
            return librosa.feature.zero_crossing_rate(y+0.0001)
        return []
            
    # Goes through the array of pre-calculated data and runs calculateData on it.
    def extractIndividualFeature(self, feature: str, featureArray, headerData):
        tokens = feature.split("_")
        header_features = ["amplitude","duration","freqhd_min","freqhd_max"]
        if feature == "amplitude":
            if self.inputType == "mtsf":
                if "amplitude" in headerData.pe_header_features:
                    return headerData.pe_header_features["amplitude"]
                return np.nan
            return headerData.amplitude
        elif feature == "duration":
            return headerData.duration
        elif feature == "freqhd_min":
            return headerData.freqhd_min
        elif feature == "freqhd_max":
            return headerData.freqhd_max
        elif feature == "frange":
            return headerData.freqhd_max - headerData.freqhd_min
        elif feature == "fslopehd":
            return (headerData.freqhd_max - headerData.freqhd_min) / (headerData.duration / 1000)
        elif feature == "freqsdelbow":
            if self.inputType == "mtsf" and feature in headerData.pe_header_features:
                return headerData.pe_header_features[feature]
            if headerData.slice_data[0] == -1 or (self.inputType == "mtsf" and not feature in headerData.pe_header_features):
                return np.nan
            deriv2abs = np.abs(self.calculateFreqSD2ndDerivative(headerData.slice_data))
            elbow = headerData.slice_data[np.argmax(deriv2abs)+1]
            start = (1000 * (headerData.slice_data[0][0] - elbow[0]) / self.sr, headerData.slice_data[0][1] - elbow[1])
            end = (1000 * (headerData.slice_data[len(headerData.slice_data)-1][0] - elbow[0]) / self.sr, \
                   headerData.slice_data[len(headerData.slice_data)-1][1] - elbow[1])
            dot = np.dot(start, end)
            mag = np.sqrt(np.power(start[0],2)+np.power(start[1],2)) * np.sqrt(np.power(end[0],2)+np.power(end[1],2))
            if mag == 0 or np.abs(dot/mag) > 1:
                return 180.0
            return 180.0 * np.arccos(dot/mag) / np.pi
        elif feature == "freqsdslope" or tokens[0] in ["freqsd","freqsdd1","freqsdd2","wcfreqs","wcslopes","wccurves"]:
            if self.inputType == "mtsf" and feature in headerData.pe_header_features:
                return headerData.pe_header_features[feature]
            if headerData.slice_data[0] == -1 or (self.inputType == "mtsf" and not feature in headerData.pe_header_features):
                return np.nan
        if feature == "freqsdslope":
            return (headerData.slice_data[len(headerData.slice_data)-1][1] - headerData.slice_data[0][1]) / (headerData.duration / 1000)
        elif tokens[0] == "freqsd":
            outp = self.calculateUnit([x[1] for x in headerData.slice_data], tokens[len(tokens)-1])
            if np.isnan(outp):
                return 0.0
            return outp
        elif tokens[0] == "freqsdd1":
            outp = self.calculateUnit(self.calculateFreqSD1stDerivative(headerData.slice_data), tokens[len(tokens)-1])
            if np.isnan(outp):
                return 0.0
            return outp
        elif tokens[0] == "freqsdd2":
            outp = self.calculateUnit(self.calculateFreqSD2ndDerivative(headerData.slice_data), tokens[len(tokens)-1])
            if np.isnan(outp):
                return 0.0
            return outp
        #elif tokens[0] in ["wcfreqs","wcslopes","wccurves"]:
        #    fragments = self.fragmentContour(headerData.slice_data, int(tokens[2]))
        #    polys = []
        #    for i in np.arange(len(fragments)):
        #        if tokens[0] == "wcfreqs":
        #            polys.append(np.mean([x[1] for x in fragments[i]]))
        #        if tokens[0] == "wcslopes":
        #            polys.append(self.getQuadraticParameters(fragments[i], 1))
        #        if tokens[0] == "wccurves":
        #            polys.append(self.getQuadraticParameters(fragments[i], 2))
        #    outp = self.calculateUnit(polys, tokens[1])
        #    if np.isnan(outp):
        #        return 0.0
        #    return outp
        elif tokens[0] in ["rms","bandwidth","centroid","flatness","flux","specmag","rolloff","praat","zcr"]:
            return self.calculateUnit(featureArray, tokens[len(tokens)-1])
        elif tokens[0] == "formantfreq":
            outp = []
            for frame in featureArray:
                if len(frame) >= int(tokens[4]):
                    outp.append(frame[int(tokens[4])-1])
            return self.calculateUnit(outp, tokens[5])
        elif tokens[0] == "formantcount":
            outp = []
            for frame in featureArray:
                outp.append(len(frame))
            return self.calculateUnit(outp, tokens[4])
        elif tokens[0] == "formantdiff":
            outp = []
            num = int(tokens[4])-1
            for frame in featureArray:
                if num+1 < len(frame):
                    outp.append(frame[num+1] - frame[num])
            return self.calculateUnit(outp, tokens[5])
        elif tokens[0] == "formantratio":
            outp = []
            num = int(tokens[4])
            for frame in featureArray:
                if num <= len(frame):
                    outp.append(frame[num-1] / frame[0])
            return self.calculateUnit(outp, tokens[5])
        #elif tokens[0] in ["mfcc","poly"]:
        elif tokens[0] == "mfcc":
            if tokens[2] == "all":
                return self.calculateUnit(featureArray, tokens[len(tokens)-1])
            return self.calculateUnit(featureArray[int(tokens[2])-1], tokens[len(tokens)-1])
        elif tokens[0] == "poly":
            if tokens[2] == "all":
                return self.calculateUnit(featureArray, tokens[len(tokens)-1])
            else:
                # Remember - for polynomial features, array is of length order+1 and the first index is 0.
                return self.calculateUnit(featureArray[int(tokens[2])], tokens[len(tokens)-1])
        #elif tokens[0] == "harmmags":
            #if np.max(featureArray[0]) == 0:
            #    return 1.0
            #outp = np.sum([x/np.max(featureArray[0]) for x in featureArray[0]])
            #if np.isnan(outp):
            #    return 0.0
            #return outp
        elif tokens[0] == "contrast":
            if tokens[3] == "all":
                return self.calculateUnit(featureArray, tokens[len(tokens)-1])
            return self.calculateUnit(featureArray[int(tokens[3])], tokens[len(tokens)-1])
        elif tokens[0] == "thd":
            # Kudos: https://www.analog.com/media/en/training-seminars/design-handbooks/Practical-Analog-Design-Techniques/Section8.pdf'
            # and this: https://www.researchgate.net/profile/Sebastian-Westerhold/publication/362542224_Total_Harmonic_Distortion_THD_analysis_utilizing_the_FFT_capabilities_of_modern_digital_storage_oscilloscopes/links/62f047910b37cc34477ab55e/Total-Harmonic-Distortion-THD-analysis-utilizing-the-FFT-capabilities-of-modern-digital-storage-oscilloscopes.pdf
            # and also: https://www.youtube.com/watch?v=s_cVP5gu4SY for deriving THD out of FFT magnitudes
            if len(featureArray[0]) == 0 or len(featureArray[0][0]) < 2:
                return 0.0
            thd = []
            for i in np.arange(len(featureArray[0])):
                frame = featureArray[0][i]
                if (frame[0][1] > 0.0):
                    #thd.append(np.sqrt(np.sum([np.power(frame[x][1], 2) for x in np.arange(len(frame)-1)+1]))/frame[0][1])
                    harmonic_power_ratios = []
                    for j in np.arange(len(frame)-1)+1:
                        dbc = frame[j][1] - frame[0][1]
                        power_ratio = np.power(10, dbc/10)
                        harmonic_power_ratios.append(power_ratio)
                    thd.append(100*np.sqrt(np.sum(harmonic_power_ratios)))
            thd = [x for x in thd if str(x) != 'nan']
            return self.calculateUnit(thd, tokens[len(tokens)-1])
        elif tokens[0] == "hbr": # harmonic-mean divided by frame-median FFT magnitude
            if len(featureArray[0]) == 0:
                return 0.0
            ratios = []
            for i in np.arange(len(featureArray[0])):
                frame_t = np.transpose(featureArray[0][i])
                ratios.append(np.mean(frame_t[1])/np.median(featureArray[1][i]))
            ratios = [x for x in ratios if str(x) != 'nan']
            return self.calculateUnit(ratios, tokens[len(tokens)-1])
        #elif tokens[0] in ["hcentrmean","hcentrstd"]:
        elif tokens[0] == "behc":
            if len(featureArray[0]) == 0:
                return self.calculateUnit([1.0], tokens[len(tokens)-1])
            centroids = []
            for i in np.arange(len(featureArray[0])):
                frame_t = np.transpose(featureArray[0][i])
                harmonics_100 = []
                if np.max(frame_t[1]) == 0.0:
                    harmonics_100 = [1.0]
                else:
                    harmonics_100 = [100*x/np.max(frame_t[1]) for x in frame_t[1]]
                    #harmonics_100 = [x for x in harmonics_100 if str(x) != 'nan']
                if len(harmonics_100) == 0:
                    harmonics_100 = [1.0]
                harmonics_histo = []
                for j in np.arange(len(harmonics_100)):
                    for k in np.arange(int(harmonics_100[j])):
                        harmonics_histo.append(j+1)
                if tokens[len(tokens)-2] == "mean":
                    centroids.append(np.mean(harmonics_histo))
                elif tokens[len(tokens)-2] == "med":
                    centroids.append(np.median(harmonics_histo))
                elif tokens[len(tokens)-2] == "std":
                    centroids.append(np.std(harmonics_histo))
                elif tokens[len(tokens)-2] == "mode":
                    centroids.append(np.array(harmonics_100).argmax()+1)
                else:
                    return feature
            return self.calculateUnit(centroids, tokens[len(tokens)-1])
        elif tokens[0] == "hcentroid":
            hcentroids = []
            praat = featureArray[0]
            centroid = featureArray[1]
            #print("praat: "+str(len(praat))+", centroid: "+str(len(centroid)), flush=True)
            #print(str(centroid), flush=True)
            for i in np.arange(len(praat)):
                if praat[i] > 0.0:
                    hcentroids.append(centroid[i]/praat[i])
            return self.calculateUnit(hcentroids, tokens[len(tokens)-1])
        elif tokens[0] == "hfr":
            hn = int(tokens[1])-1
            max_ratio = float(tokens[4])
            ratios = []
            for i in np.arange(len(featureArray[0])):
                frame = featureArray[0][i]
                # Harmonic frequency must be below half of sampling rate.
                if 0.0 < frame[hn][0] < self.sr/2:
                    if frame[hn][1] == 0.0:
                        ratios.append(0.0)
                    elif frame[0][1] > 0.0:
                        ratio = frame[hn][1]/frame[0][1]
                        if ratio < max_ratio:
                            ratios.append(ratio)
                        else:
                            ratios.append(max_ratio)
                    else: # Fundamental has a magnitude of zero
                        ratios.append(max_ratio)
            return self.calculateUnit(ratios, tokens[len(tokens)-1])
        return feature
     
    # Extracts the mean, median, standard deviation, maximum or minimum from pre-calculated data.
    def calculateUnit(self, featureArray, function):
        if len(featureArray) == 0:
            return 0.0
        if function == "mean":
            return np.mean(featureArray)
        elif function == "med":
            return np.median(featureArray)
        elif function == "std":
            return np.std(featureArray)
        elif function == "max":
            return np.max(featureArray)
        elif function == "min":
            return np.min(featureArray)
        elif function == "rng":
            return np.max(featureArray) - np.min(featureArray)
        elif function == "skew":
            return scipy.stats.skew(featureArray)
        elif function == "fullclip":
            return featureArray[0]
        return 0.0;
    
    # Returns shorthands for window functions.
    def getWindowName(self, winname):
        if winname == "Bartlett-Hann":
            return "barthann"
        elif winname == "Blackman-Harris":
            return "blackmanharris"
        elif winname == "Flat top":
            return "flattop"
        elif winname == "Triangular":
            return "triang"
        return winname.lower()
    
    def calculateFreqSD1stDerivative(self, slice_data: list):
        if len(slice_data) <= 1:
            return [0.0]
        ms = [1000*(x[0]-slice_data[0][0])/self.sr for x in slice_data]
        freqs = [x[1] for x in slice_data]
        return [(freqs[i+1] - freqs[i])/(ms[i+1] - ms[i]) for i in np.arange(len(freqs)-1)]
    
    def calculateFreqSD2ndDerivative(self, slice_data: list):
        deriv = self.calculateFreqSD1stDerivative(slice_data)
        if len(deriv) <= 1:
            return [0.0]
        ms = [1000*(x[0]-slice_data[0][0])/self.sr for x in slice_data]
        return [(deriv[i+1] - deriv[i])/(ms[i+1] - ms[i]) for i in np.arange(len(deriv)-1)]
    
    # This function is a Pythonized recreation of whistleClassifier.OverlappingFragmenter
    #def fragmentContour(self, slice_data: list, fragment_length: int):
    #    timestamps = [(x[0]-slice_data[0][0])/self.sr for x in slice_data]
    #    freqs = [x[1] for x in slice_data]
    #    n_fragments = int(np.ceil(float(len(slice_data)/fragment_length)))
    #    if (n_fragments <= 1):
    #        return []
    #    step_size = (len(slice_data) - fragment_length) / (n_fragments - 1)
    #    fragment_starts = []
    #    for i in np.arange(n_fragments):
    #        fragment_starts.append(int(i * step_size))
    #    if (len(fragment_starts) == 0):
    #        return []
    #    outp = []
    #    for i in np.arange(len(fragment_starts)):
    #        curr = []
    #        for j in np.arange(fragment_length):
    #            curr_index = fragment_starts[i] + j
    #            curr.append([timestamps[curr_index],freqs[curr_index]])
    #        outp.append(curr)
    #    return outp
    
    # This function is a Pythonized replacement of whistleClassifier.QuadraticParameteriser
    #def getQuadraticParameters(self, fragment: list, degree: int):
    #    fit = np.polyfit([x[0] for x in fragment], [x[1] for x in fragment], degree)
    #    #return fit[degree]
    #    return fit[0] # Seems to order the polynomials in reverse from what the Java function does.
    
class HeaderData:
    def __init__(self, uid, datelong, amplitude, duration, freqhd_min, freqhd_max, slice_data, pe_cluster_id, pe_location, pe_label, pe_header_features):
        #np.set_printoptions(legacy='1.25')
        self.uid = uid
        self.datelong = datelong
        self.amplitude = amplitude
        self.duration = duration
        self.freqhd_min = freqhd_min
        self.freqhd_max = freqhd_max
        self.slice_data = slice_data
        self.pe_cluster_id = pe_cluster_id
        self.pe_location = pe_location
        self.pe_label = pe_label
        self.pe_header_features = pe_header_features

# Loads clip into a variable and then deletes the file, as it is no longer needed.
def loadAudio(fn: str, sr: int):
    #np.set_printoptions(legacy='1.25')
    warnings.simplefilter(action='ignore', category=FutureWarning)
    try:
        y, newsr = librosa.load(fn, sr=sr)
        #os.remove(fn)
        return y
    except (Exception, RuntimeError, FileNotFoundError) as e:
        print("loadAudio exception")
        print(traceback.format_exc(), file=sys.stderr)
    return []
