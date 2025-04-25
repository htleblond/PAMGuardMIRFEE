package mirfeeLiveClassifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import PamguardMVC.PamDataUnit;
import binaryFileStorage.BinaryDataSource;
import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.ModuleFooter;
import binaryFileStorage.ModuleHeader;
import mirfeeFeatureExtractor.FEDataBlock;

/**
 * The Live Classifier's BinaryDataSource.
 * @author Holly LeBlond
 */
public class LCBinaryDataSource extends BinaryDataSource {
	
	private LCControl lcControl;
	private String streamName;
	
	private static final int currentVersion = 6;
	private static final int moduleID = 1901;
	
	private LCBinaryModuleHeader currMH;
	
	public LCBinaryDataSource(LCControl lcControl, LCDataBlock sisterDataBlock, String streamName) {
		super(sisterDataBlock);
		this.lcControl = lcControl;
		this.streamName = streamName;
	}
	
	private ByteArrayOutputStream bos;
	private DataOutputStream dos;
	private DataOutputStream headerOutputStream;
	private ByteArrayOutputStream headerBytes;
	
	
	@Override
	public BinaryObjectData getPackedData(PamDataUnit pamDataUnit) {
		//if (lcControl.getParams().printJava)
		//	System.out.println("REACHED getPackedData");
		LCDataUnit du = (LCDataUnit) pamDataUnit;
		if (dos == null || bos == null) {
			dos = new DataOutputStream(bos = new ByteArrayOutputStream());
		}
		else {
			bos.reset();
		}
		
		//this.getBinaryStorageStream().writeModuleHeader(getModuleHeaderData());
		
		LCCallCluster cc = du.getCluster();
		try {
			dos.writeUTF(cc.clusterID);
			dos.writeInt(cc.getSize());
			for (int i = 0; i < cc.getSize(); i++) {
				dos.writeLong(cc.uids[i]);
				dos.writeLong(cc.datetimes[i]);
				dos.writeFloat((float) cc.durations[i]);
				dos.writeFloat((float) cc.lfs[i]);
				dos.writeFloat((float) cc.hfs[i]);
				for (int j = 0; j < cc.probaList[i].length; j++) {
					dos.writeFloat((float) cc.probaList[i][j]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		BinaryObjectData pbo = new BinaryObjectData(moduleID, bos.toByteArray());
		try {
			dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return pbo;
	}

	@Override
	public String getStreamName() {
		return streamName;
	}

	@Override
	public int getStreamVersion() {
		return 0;
	}

	@Override
	public int getModuleVersion() {
		return currentVersion;
	}

	@Override
	public byte[] getModuleHeaderData() {
		//if (lcControl.getParams().printJava)
		//	System.out.println("REACHED getModuleHeaderData");
		if (headerOutputStream == null) {
			headerOutputStream = new DataOutputStream(headerBytes = new ByteArrayOutputStream());
		}
		headerBytes.reset();
		try {
			headerOutputStream.writeShort(lcControl.getParams().labelOrder.length);
			for (int i = 0; i < lcControl.getParams().labelOrder.length; i++) {
				headerOutputStream.writeUTF(lcControl.getParams().labelOrder[i]);
			}
			if (lcControl.isViewer()) {
				headerOutputStream.writeShort(currMH.features.length);
				for (int i = 0; i < currMH.features.length; i++) {
					headerOutputStream.writeUTF(currMH.features[i]);
				}
			} else {
				FEDataBlock vectorBlock = (FEDataBlock) lcControl.getProcess().getParentDataBlock();
				headerOutputStream.writeShort(vectorBlock.getFeatureNames().length);
				for (int i = 0; i < vectorBlock.getFeatureNames().length; i++) {
					headerOutputStream.writeUTF(vectorBlock.getFeatureNames()[i]);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return headerBytes.toByteArray();
	}

	@Override
	public ModuleHeader sinkModuleHeader(BinaryObjectData binaryObjectData, BinaryHeader bh) {
		//if (lcControl.getParams().printJava)
		//	System.out.println("REACHED sinkModuleHeader");
		ByteArrayInputStream bis = new ByteArrayInputStream(binaryObjectData.getData());
		DataInputStream dis = new DataInputStream(bis);
		LCBinaryModuleHeader mh = new LCBinaryModuleHeader(binaryObjectData.getVersionNumber());
		try {
			mh.species = new String[dis.readShort()];
			for (int i = 0; i < mh.species.length; i++) {
				mh.species[i] = dis.readUTF();
			}
			mh.features = new String[dis.readShort()];
			for (int i = 0; i < mh.features.length; i++) {
				mh.features[i] = dis.readUTF();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		currMH = mh;
		return mh;
	}

	@Override
	public PamDataUnit sinkData(BinaryObjectData binaryObjectData, BinaryHeader bh, int moduleVersion) {
		//if (lcControl.getParams().printJava)
		//	System.out.println("REACHED sinkData");
		
		// This doesn't seem to do anything.
	/*	BinaryStore bs = BinaryStore.findBinaryStoreControl();
		if (bs.getBinaryStoreSettings().getStoreLocation() == null) {
			currentBinaryDataFolder = "";
			lcControl.getTabPanel().getPanel().getTableModel().setRowCount(0);
			lcControl.getProcess().resultsDataBlock.clearAll();
		} else if (!bs.getBinaryStoreSettings().getStoreLocation().equals(currentBinaryDataFolder)) {
			currentBinaryDataFolder = bs.getBinaryStoreSettings().getStoreLocation();
			lcControl.getTabPanel().getPanel().getTableModel().setRowCount(0);
			lcControl.getProcess().resultsDataBlock.clearAll();
		} */
		if (!lcControl.isViewer()) {
			currMH = (LCBinaryModuleHeader) sinkModuleHeader(binaryObjectData, bh);
		}
		LCDataUnit du = null;
		
		ByteArrayInputStream bis = new ByteArrayInputStream(binaryObjectData.getData(), 
				0, binaryObjectData.getDataLength());
		DataInputStream dis = new DataInputStream(bis);
		
		try {
			String clusterID = dis.readUTF();
			int nDetections = dis.readInt();
			LCCallCluster cc = new LCCallCluster(lcControl.getParams().labelOrder, nDetections);
			cc.clusterID = clusterID;
			if (currMH.species.length != cc.labelList.size()) {
				System.out.println("LCBinaryDataSource.sinkData: Cluster at "
						+lcControl.convertDateLongToString(lcControl.convertFromLocalToUTC(bh.getDataDate()))
						+" does not contain the same species as those specified in the parameters.");
				return null;
			}
			for (int i = 0; i < currMH.species.length; i++) {
				if (!cc.labelList.contains(currMH.species[i])) {
					System.out.println("LCBinaryDataSource.sinkData: Cluster at "
							+lcControl.convertDateLongToString(lcControl.convertFromLocalToUTC(bh.getDataDate()))
							+" does not contain the same species as those specified in the parameters.");
					return null;
				}
			}
			//System.out.println(clusterID+": "+String.valueOf(nDetections)+" detections");
			for (int i = 0; i < nDetections; i++) {
				cc.uids[i] = dis.readLong();
				cc.datetimes[i] = dis.readLong(); // NOTE that this is in local time.
				cc.durations[i] = (int) dis.readFloat();
				cc.lfs[i] = (int) dis.readFloat();
				cc.hfs[i] = (int) dis.readFloat();
				for (int j = 0; j < currMH.species.length; j++) {
					// This ensures that you can use binary files where the label order may be different, as long as the exact same species are present.
					cc.probaList[i][cc.labelList.indexOf(currMH.species[j])] = dis.readFloat();
				}
			}
			du = new LCDataUnit(lcControl, cc);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return du;
	}

	@Override
	public ModuleFooter sinkModuleFooter(BinaryObjectData binaryObjectData, BinaryHeader bh,
			ModuleHeader moduleHeader) {
		//if (lcControl.getParams().printJava)
		//	System.out.println("REACHED sinkModuleFooter");
		return null;
	}

	@Override
	public void newFileOpened(File outputFile) {}
}