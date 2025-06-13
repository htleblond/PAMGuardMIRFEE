# Music Information Retrieval Feature-Extracting Ensemble (MIRFEE) Classifier for PAMGuard

This is an external PAMGuard plugin for classifying Whistle and Moan Detector (WMD) contours using music information retrieval (MIR) techniques and contour header and slice data in conjunction with an ensemble classifier model. While the primary goal of this project is to create a classifier that can discriminate between killer whale, humpback whale and vessel noise in high-traffic areas of the Salish Sea, the classifier should theoretically work with any sound source that produces WMD detections.

![alt text](https://github.com/htleblond/PamGuardMIRRF/blob/main/screenshots/Live%20Classifier%20and%20WMNT%20example.png?raw=true)
<p align="center">
  <em>Whistle and Moan Annotation Tool (left) with MIRFEE Live Classifier overlay markings in the spectrogram (right)</em>
</p>

## Includes the following modules:

Under "Classifiers":
- **Feature Extractor** - Extracts feature vector data from sound clips where WMD detections occur.
- **Training Set Builder** - Tool for combining feature vector data and annotation data to create and customize training sets.
- **Live Classifier** - Classifies feature vector data directly output by the Feature Extractor.
- **Test Classifier** - Performs cross-validation on pre-existing training sets.

Under "Utilities" (Viewer-mode only):
- **Whistle and Moan Annotation Tool (WMAT)** - Tool for annotating WMD detections and for easier navigation of the spectrogram after processing.

## Installation
(You can skip steps 1 and 2 if you're ONLY using the WMAT.)
1. Install Python 3 and ensure that pip works.
2. Run the .bat file from the latest release. (Note to Python developers: Check the .bat file first in case it creates any conflicts!)
3. Place the .jar file from the latest release into your version of PAMGuard's "plugins" folder.

## Citation
If you wish to cite this plugin in a publication, please cite the following:

LeBlond, H.T., Quayle, L.S., and Yurk, H. 2025. Evaluating the performance of the MIRFEE classifier plugin for PAMGuard at differentiating between whale vocalizations and anthropogenic noise in the Salish Sea. Can. Tech. Rep. Fish. Aquat. Sci. 3699: iv + 28 p. https://doi.org/10.60825/fmjr-ze05

## (More-than-likely-to-be-FA)Qs
- **Is this plugin a training model in itself?** - Technically, no. It's more a tool for creating models by building your own training sets out of manually-annotated data.
- **Does this actually work?** - Depending on the context, either "mostly" or "maybe". For our own orca-detection purposes, we're still developing a training set that can reliably differentiate killer whale detections from vessels and humpbacks. Our best test runs up to this point have managed to get accuracies between 85 and 90%, which means we're on to something, but clearly need to do better in order for it to be reliable for tracking whales. The methods are primarily based off of those used in the Orchive (https://arxiv.org/pdf/1307.0589.pdf), but with some major differences and a bunch of new experimental stuff thrown in. However, if you have your own WMD data with whatever species you want to be able to automatically tell apart, feel free to create your own training sets, and if not, the WMAT is at least pretty useful for easy annotation.
- **How do you use this?** - I'll get to that - a helpset will be added in a future update.
- **Can you provide any training sets here?** - Uhhhhhh, not yet, and no guarantees that I will be allowed to. I would eventually like to though.
- **Why does this plugin use Python? Why not just Java?** - The Feature Extractor extensively uses the Librosa Python library, which provides a good chunk of the calculations in the feature extraction process. While there are some Java libraries that could apparently provide some of the necessary functions, there just aren't any equivalent Java libraries that could totally replace Librosa. Additionally, the classifier models are provided by the Scikit-Learn Python library.
- **Will the WMAT eventually be compatible with SQLite?** - It is now! (As of 1.03a.)

![alt text](https://github.com/htleblond/PamGuardMIRRF/blob/main/screenshots/Feature%20Extractor%20example.png?raw=true)
<p align="center">
  <em>Selecting features in the MIRFEE Feature Extractor</em>
</p>

![alt text](https://github.com/htleblond/PamGuardMIRRF/blob/main/screenshots/Test%20Classifier%20example.png?raw=true)
<p align="center">
  <em>MIRFEE Test Classifier results table and confusion matrices</em>
</p>
