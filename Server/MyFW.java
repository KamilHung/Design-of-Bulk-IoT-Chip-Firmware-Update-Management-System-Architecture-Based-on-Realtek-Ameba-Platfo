package paper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class MyFW {
	private String fwName;
	private byte[] fileInfo;
	private int fileSize;
	private int fileCheckSum;
	private File fwFile = null;
	
	public File getFWFile(MyFW fw) {
		fwFile = new File(fw.getFWName());
		return fwFile;
	}
	
	public void setFileCheckSum(int value) {
		fileCheckSum = value;
	}
	
	public int getFileCheckSum() {
		return fileCheckSum;
	}
	
	public void setFileSize(int value) {
		fileSize = value;
	}
	
	public int getFileSize() {
		return fileSize;
	}
	/*
	 * fileInfo[]
	 * checksum 0x19d6d0c => fileInfo[0] = 0x0c, [1] = 0x6d, [2] = 0x9d, [3] = 0x01 
	 * [4][5][6][7] = 0x00
	 * size 0x40254 => fileInfo[8] = 0x54, [9] = 0x02, [10] = 0x04, [11] = 0x00
	 */
	public void setFileInfo(int length, int checksum) {
		fileInfo = new byte[12];
		fileInfo[0] = (byte) (checksum & 0xff);
		fileInfo[1] = (byte) ((checksum >> 8) & 0xff);
		fileInfo[2] = (byte) ((checksum >> 16) & 0xff);
		fileInfo[3] = (byte) ((checksum >> 24) & 0xff);
		fileInfo[4] = fileInfo[5] = fileInfo[6] = fileInfo[7] = (byte) 0x00;
		for(int i = 8; i <= 11; i++){
            fileInfo[i] = (byte)(length & 0xFF);
            length >>= 8;
        }
	}
	
	public byte[] getFileInfo() {
		return fileInfo;
	}
	
	public void setFWName(String name) {
		this.fwName = name;
	}
	
	public String getFWName() {
		return this.fwName;
	}
	
	public boolean checkFWExist(String name) {
		File FW = new File(name);
		if(FW.exists()) { //韌體檔案是否存在
			return true;
		}	
		else {
			return false;
		}
	}
	
	public boolean fwName(MyFW fw, MyCSV csv, int value) {
		String pro = csv.get_CSV_Product_Array(value); //CSV Product
		String ver = csv.get_CSV_Update_Array(value); //CSV Update
		fw.setFWName(pro + "_" + ver + ".bin"); //組合成韌體檔名
		if(fw.checkFWExist(fw.getFWName())){ //判斷韌體檔案是否存在
			return true;
		}else {
			return false;
		}
	}
	
	public void runEXE(MyFW fw, String name) {
		Runtime rt = Runtime.getRuntime();
		try {
			String cmd = "checksum.exe " + name;
			System.out.println(cmd);
			Process pr = rt.exec(cmd);
			BufferedReader input = new BufferedReader(
					new InputStreamReader(pr.getInputStream(), "GBK"));
			String line = null;
			while((line = input.readLine()) != null) {
				if(!line.equals("")) {
					String[] getInfo = line.split(" = ");
					if(getInfo[0].equals("size")) {
						fw.setFileSize(Integer.parseInt(getInfo[1]));
					}else if(getInfo[0].equals("checksum")) {
						fw.setFileCheckSum(Integer.parseInt(getInfo[1]));
					}
				}
			}
			//System.out.println(fw.getFileSize());
			//System.out.println(fw.getFileCheckSum());
		} catch (IOException e) {
			System.out.println("exec error");
		}
	}
	
	public static void main(String[] args) {
		MyFW fw = new MyFW();
		MyCSV csv = new MyCSV();
		csv.myCSVInit(csv, "File", 100);
		if(fw.fwName(fw, csv, 0)) {
			fw.runEXE(fw, fw.getFWName());
			fw.setFileInfo(fw.getFileSize(), fw.getFileCheckSum());
		}
		if(fw.fwName(fw, csv, 1)) {
			fw.runEXE(fw, fw.getFWName());
			fw.setFileInfo(fw.getFileSize(), fw.getFileCheckSum());
		}
	}
}
