package paper;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

public class MyCSV {
	private String csvName;
	private File csvFile;
	private long lastTime;
	private String[] csv_MAC_Array;
	private String[] csv_Product_Array;
	private String[] csv_Update_Array;
	private String[] csv_SetupCode_Array;
	private String[] csv_SerialNumber_Array;
	private int csvQuantity;
	
	public void set_CSV_SetupCode_Array(String SC, int value) {
		this.csv_SetupCode_Array[value] = SC;
	}
	
	public String get_CSV_SetupCode_Array(int value) {
		return this.csv_SetupCode_Array[value];
	}
	
	public void set_CSV_SerialNumber_Array(String SN, int value) {
		this.csv_SerialNumber_Array[value] = SN;
	}
	
	public String get_CSV_SerialNumber_Array(int value) {
		return this.csv_SerialNumber_Array[value];
	}
	
	public void set_CSV_MAC_Array(String MAC, int value) {
		this.csv_MAC_Array[value] = MAC;
	}
	
	public String get_CSV_MAC_Array(int value) {
		return this.csv_MAC_Array[value];
	}
	
	public void set_CSV_Product_Array(String Product, int value) {
		this.csv_Product_Array[value] = Product;
	}
	
	public String get_CSV_Product_Array(int value) {
		return this.csv_Product_Array[value];
	}
	
	public void set_CSV_Update_Array(String Update, int value) {
		this.csv_Update_Array[value] = Update;
	}
	
	public String get_CSV_Update_Array(int value) {
		return this.csv_Update_Array[value];
	}
	
	public void setCSVQuantity(int value) {
		this.csvQuantity = value;
	}
	
	public int getCSVQuantity() {
		return this.csvQuantity;
	}
	
	public void setMaxQuantity(int value) {
		csv_MAC_Array = new String[value];
		csv_Product_Array = new String[value];
		csv_Update_Array = new String[value];
		csv_SetupCode_Array = new String[value];
		csv_SerialNumber_Array = new String[value];
	}
	
	public void setCSVName(String name) {
		this.csvName = name + ".csv";
		this.csvFile = new File(this.csvName);
	}
	
	public String getCSVName() {
		return this.csvName;
	}
	
	public boolean csvFileExistCheck() {
    	return this.csvFile.exists();
    }
	
	public void createCSVFile() {
		try {
			// use FileWriter constructor that specifies open for appending
			CsvWriter csvOutput = new CsvWriter(new FileWriter(getCSVName(), true), ',');
			// if the file didn't already exist then we need to write out the header line
			writeCSV(csvOutput, "MAC", "Product", "Update", "SetupCode", "SerialNumber");
			//writeCSV(csvOutput, "ac35ee155fc7", "H1", "10101", "11111111", "H0001");
			csvOutput.close();
		} catch (Exception e){
			System.out.println("Create CSV File error");
		}
	}
	
	public void writeCSV(CsvWriter w, String MAC, String Product, 
			String FW_ver, String SC, String SN) {
		try {
			w.write(MAC);
			w.write(Product);
			w.write(FW_ver);
			w.write(SC); //Setup Code
			w.write(SN); //Serial Number
			w.endRecord();
		} catch (Exception e) {
			System.out.println("Write CSV error");
		}
	}
	
	public void readCSV() {
		try {
			CsvReader rc = new CsvReader(getCSVName());
			rc.readHeaders();
			int count = 0;
			System.out.println("Print CSV:");
			while(rc.readRecord()) {
				String MAC_Temp = rc.get("MAC");
				String Product_Temp = rc.get("Product");
				String Update_Temp = rc.get("Update");
				String SC_Temp = rc.get("SetupCode");
				String SN_Temp = rc.get("SerialNumber");
				setArray(count, MAC_Temp, Product_Temp, Update_Temp, SC_Temp, SN_Temp);
				printCSV(count);
				count++;
			}
			setCSVQuantity(count);
			rc.close();
		} catch (Exception e) {
			System.out.println("Read CSV error");
		}
	}
	
	public int readCSV(String mac) {
		int count = 0;
		try {
			CsvReader rc = new CsvReader(getCSVName());
			rc.readHeaders();
			while(rc.readRecord()) {
				String MAC_Temp = rc.get("MAC");
				if(MAC_Temp.compareTo(mac) == 0) {
					return count;
				}
				count++;
			}
			rc.close();
		} catch (Exception e) {
			System.out.println("Read CSV error");
		}
		return -1;
	}
	
	public void setArray(int c, String MAC, String Product, String Update, String SC, String SN) {
		set_CSV_MAC_Array(MAC, c);
		set_CSV_Product_Array(Product, c);
		set_CSV_Update_Array(Update, c);
		set_CSV_SetupCode_Array(SC, c);
		set_CSV_SerialNumber_Array(SN, c);
	}
	
	public void printCSV(int c) {
		System.out.println(c + "," + get_CSV_MAC_Array(c) + "," + 
				get_CSV_Product_Array(c) + "," + get_CSV_Update_Array(c) + "," +
				get_CSV_SetupCode_Array(c) + "," + get_CSV_SerialNumber_Array(c));
	}

    private void listenerCSVFile(MyCSV csv) {
    	lastTime = csvFile.lastModified();
    	ScheduledExecutorService scheduleExecutorService = Executors.newScheduledThreadPool(1);
    	scheduleExecutorService.scheduleAtFixedRate(new Runnable() {
    		@Override
    		public void run() {
    			if(!csv.csvFileExistCheck()) {
    				csv.createCSVFile();
    			} else {
    				if(csvFile.lastModified() > lastTime) {
    					Date date = new Date(csvFile.lastModified());
    					SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 hh:mm:ss a");
    					System.out.println("CSV File update time: " + format.format(date));
	    				lastTime = csvFile.lastModified();
	    				csv.readCSV();
	    				
    				}
    			}
    		}
    	}, 0, 1, TimeUnit.SECONDS);
    	
    	try {
    		Thread.sleep(3);
    	}catch(InterruptedException e){
    		e.printStackTrace();
    	}
    }
    
    //Overloading method
    public void listenerCSVFile(MyCSV csv, MySQL sql, Connection con) {
    	lastTime = csvFile.lastModified();
    	ScheduledExecutorService scheduleExecutorService = Executors.newScheduledThreadPool(1);
    	scheduleExecutorService.scheduleAtFixedRate(new Runnable() {
    		@Override
    		public void run() {
    			if(!csv.csvFileExistCheck()) {
    				csv.createCSVFile();
    			} else {
    				if(csvFile.lastModified() > lastTime) {
    					Date date = new Date(csvFile.lastModified());
    					SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 hh:mm:ss a");
    					System.out.println("CSV File update time: " + format.format(date));
	    				lastTime = csvFile.lastModified();
	    				csv.readCSV();
	    				sql.csvToDB(sql, con, csv);
    				}
    			}
    		}
    	}, 0, 1, TimeUnit.SECONDS);
    	
    	try {
    		Thread.sleep(3);
    	}catch(InterruptedException e){
    		e.printStackTrace();
    	}
    }
    
    public void myCSVInit(MyCSV mCSV, String name, int macQuantity) {
		mCSV.setCSVName(name);
		mCSV.setMaxQuantity(macQuantity);
		mCSV.listenerCSVFile(mCSV);
		mCSV.readCSV();
    }
    
    //Overloading method
    public void myCSVInit(MyCSV mCSV, MySQL sql, Connection con, String name, int macQuantity) {
		mCSV.setCSVName(name);
		mCSV.setMaxQuantity(macQuantity);
		mCSV.listenerCSVFile(mCSV, sql, con);
		mCSV.readCSV();
    }
          
	public static void main(String[] args) {
		MyCSV test = new MyCSV();
		test.myCSVInit(test, "File", 100);
		System.out.println(test.readCSV(test.get_CSV_MAC_Array(0)));
	}

}
