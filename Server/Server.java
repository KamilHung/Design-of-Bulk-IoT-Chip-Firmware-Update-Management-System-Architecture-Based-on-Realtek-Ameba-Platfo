package paper;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
/**
 *
 * @author Kamil
 */
public class Server
{
    private final int LISTEN_PORT = 1234;
    private boolean linkState;
    private MySQL mSQL;
    private MyCSV mCSV;
    private MyFW mFW;
    private Protocol protocol;
    private Connection con;
    
    public Protocol getProtocol() {
    	return protocol;
    }
    
    public void setProtocol(Protocol protocol) {
    	this.protocol = protocol;
    }
    
    public MyCSV getCSV() {
		return mCSV;
	}

	public void setCSV(MyCSV mCSV) {
		this.mCSV = mCSV;
	}
	
	public MyFW getFW() {
		return mFW;
	}
	
	public void setFW(MyFW mFW) {
		this.mFW = mFW;
	}
	
	public MySQL getSQL() {
		return mSQL;
	}
	
	public void setSQL(MySQL mSQL) {
		this.mSQL = mSQL;
	}
    
    public void setLinkState(boolean linkState) {
        this.linkState = linkState;
    }
    
    public boolean getLinkState() {
        return this.linkState;
    }
    
    public void listenRequest() {
        ServerSocket serverSocket = null;
        ExecutorService threadExecutor = Executors.newCachedThreadPool();
        try {
            serverSocket = new ServerSocket(LISTEN_PORT); //建立socket和bind
            System.out.println("Server listening requests...");
            setLinkState(true);
            
            while(getLinkState()) {
                Socket socket = serverSocket.accept(); //建立listen和aceeept
                threadExecutor.execute(new RequestThread(socket)); //若有連線啟動Thread
            }
        } catch ( Exception e ) {
        	System.out.println("listenRequest Exception");
            e.printStackTrace();
        } finally {
            if (threadExecutor != null)
                threadExecutor.shutdown();
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (Exception e) {
                	System.out.println("serverSocket Exception");
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void serverInit(MyCSV mCSV, MySQL mSQL, int Value) {
    	getSQL().setMaxMACQuantity(Value); //Set MAX MAC Quantity
		getSQL().setTableName("Now", 0); //Table Name <= 5 char
		getSQL().setTableName("Old", 1); //Table Name <= 5 char
		try {
			con = getSQL().getConnection("Ameba.db"); //DB Name
			getSQL().createTable(con, getSQL().getTableName()[0]); //Create Table
			getSQL().createTable(con, getSQL().getTableName()[1]); //Create Table
			//Set CSV name, MAC quantity, listener CSV, read CSV
			getCSV().myCSVInit(mCSV, mSQL, con, "Ameba", Value);
			//CSV Copy to DB
			getSQL().csvToDB(mSQL, con, mCSV);
		} catch (SQLException e) {
			System.out.println("Initial error");
		}
    }
    
    /**
     * @param args
     */
    public static void main( String[] args )
    {
    	Server server = new Server();
    	server.setSQL(new MySQL());
    	server.setCSV(new MyCSV());
    	server.setFW(new MyFW());
    	server.setProtocol(new Protocol());
        server.serverInit(server.getCSV(), server.getSQL(), 100); //MAX 100 MAC
        server.listenRequest();
    }
    
	/**
     * Process Client Request
     *
     * @version
     */
    class RequestThread implements Runnable
    {
        private final Socket clientSocket;
        private DataInputStream input = null;
        private DataOutputStream output = null;
        private OutputStream temp = null;
        private FileInputStream fos = null;
        private int readlen;
        
        public RequestThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        public String socketRead(int size) {
        	byte[] buffer = new byte[size];
        	try {
				while((readlen = input.read(buffer)) != -1){
					System.out.println("read length = " + readlen);
					String read = new String(buffer, 0, readlen);
					return read;
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("socket Read error");
			}
        	return "";
        }
        
        public byte[] socketRead() {
        	byte[] buffer = new byte[50];
        	try {
				if((readlen = input.read(buffer)) != -1){
					System.out.println("read length = " + readlen);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("socket Read error");
			}
        	return buffer;
        }
        
        public void socketWrite(byte[] buf, int value) {
        	try {
	        	if(value == 0) {
	        		output.writeUTF("yes");
	        	}else if(value == -1) {
	        		output.writeUTF("no");
	        	}else {
	        		output.write(buf, 0, value);
	        	}
	        	output.flush();
        	} catch (Exception e) {
        		System.out.println("socket Write error");
        	}
        }
        
        public void writeFWFile(File file) {
        	byte[] fwFile = new byte[50];
        	int num;
			try {
				fos = new FileInputStream(file);//error
				num = fos.read(fwFile);
				while(num != (-1)){
	                temp.write(fwFile, 0, num);
	                temp.flush();
	                num = fos.read(fwFile);
	            }
			} catch (IOException e) {
				System.out.println("write FW File error");
			}
        }
        
        public void setInitial() {
        	try {
				input = new DataInputStream( this.clientSocket.getInputStream());
				output = new DataOutputStream( this.clientSocket.getOutputStream());
	            //data package
	            OutputStream netOut = clientSocket.getOutputStream();
	            temp = new DataOutputStream(new BufferedOutputStream(netOut));
			} catch (IOException e) {
				System.out.println("Set Stream error");
			}
        }
        
        
        public int updateLinkCount(String mac) {
        	try {
				if(getSQL().selectMAC(con, getSQL().getTableName()[0], mac)) {
					int linkCount = getSQL().selectCheckFW(con, getSQL().getTableName()[0], mac);
					getSQL().updateData(con, getSQL().getTableName()[0], mac, "CheckFW", ++linkCount);
					System.out.println("This MAC:" + mac + ", Link Count:" + linkCount);
					return linkCount;
				}else{
					return 0;
				}
			} catch (SQLException e) {
				System.out.println("selectMAC error");
				return 0;
			}
        }
        
        public boolean compareUpdate(String mac, String product, String fw_ver) {
        	int csvCount = getCSV().readCSV(mac);
        	if((product.compareTo(getCSV().get_CSV_Product_Array(csvCount)) != 0) || 
        		(fw_ver.compareTo(getCSV().get_CSV_Update_Array(csvCount)) != 0)) {
        		return true;
        	}else {
        		return false;
        	}
        }
        
        public boolean setFWReady(int order) {
        	if(getFW().fwName(getFW(), getCSV(), order)) { //判斷韌體檔案是否存在
    			return true;
    		}else {
    			return false;
    		}
        }
                
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            System.out.printf("Server %s,", clientSocket.getLocalAddress());
            System.out.println(" has Address " + clientSocket.getRemoteSocketAddress() + " Link!");
            setInitial();
            
            byte[] protocol = new byte[50];
            protocol = socketRead();

            if(getProtocol().protocolCheck(protocol, 24 + protocol[21], 1)) {
            	
	            String mac = getProtocol().protocolAnalysis(protocol, 0);
	    		String product = getProtocol().protocolAnalysis(protocol, 1);
	    		String fw_ver = getProtocol().protocolAnalysis(protocol, 2);
	    		String sc = getProtocol().protocolAnalysis(protocol, 3);
	    		String sn = getProtocol().protocolAnalysis(protocol, 4);
	            
	            int link = updateLinkCount(mac); //Check MAC in DB
	            int order = getCSV().readCSV(mac);
	            int checkSum = 0, fileSize = 0;
	            File sendFile = null;
	            if(link > 0) {
	            	int value = getCSV().readCSV(mac);
	            	boolean fw_check = compareUpdate(mac, product, fw_ver) && setFWReady(value); //Compare CSV product and fw_ver. FW Ready.
	            	if(fw_check) {
	            		try {
	            			sendFile = getFW().getFWFile(getFW()); //取得FW檔案
	            			getFW().runEXE(getFW(), getFW().getFWName()); //執行C語言exe檔
	            			checkSum = getFW().getFileSize(); //取得Checksum
	            			fileSize = getFW().getFileCheckSum(); //取得FW size
							getSQL().updateRenew(con, getSQL().getTableName()[0], mac, "yes");
						} catch (SQLException e) {
							System.out.println("updateRenew error");
						}
	            	}
	            	byte[] proto = new byte[50];
	            	proto = getProtocol().combinationArray(mCSV, sc, sn, order, fw_check);
	            	socketWrite(proto, 15 + getCSV().get_CSV_SerialNumber_Array(order).length()); //Send Protocol
	            	
	            	byte[] protocol2 = new byte[5];
	            	byte[] protocol3 = new byte[5];
	            	protocol2 = socketRead();
	            	if(getProtocol().protocolCheck(protocol2, 5, 2)) {
	            		if(protocol2[2] == 0x01) { //Start
	            			getFW().setFileInfo(checkSum, fileSize);//取得fileInfo及Checksum
		            		socketWrite(getFW().getFileInfo(), 12); //send info
		            		writeFWFile(sendFile); //send fw
		            		System.out.println("Send FW OK! ");
		            		protocol3 = socketRead();
		            		if(getProtocol().protocolCheck(protocol3, 5, 2)) {
		            			if(protocol3[2] == 0x02) { //result ok
		            				System.out.println("result ok");
		            				try {
										getSQL().update(con, getSQL().getTableName()[0], getCSV().get_CSV_MAC_Array(value), 
												getCSV().get_CSV_Product_Array(value), getCSV().get_CSV_Update_Array(value), link, "no",
												getCSV().get_CSV_SetupCode_Array(value), getCSV().get_CSV_SerialNumber_Array(value));
									} catch (SQLException e) {
										System.out.println("Update result ok " + mac + " error");
									}
		            			}else if(protocol3[2] == 0x00){ //result fail
		            				System.out.println("result fail");
		            				try {
										getSQL().update(con, getSQL().getTableName()[0], mac, product, fw_ver, link, "yes",
												getCSV().get_CSV_SetupCode_Array(value), getCSV().get_CSV_SerialNumber_Array(value));
									} catch (SQLException e) {
										System.out.println("Update result fail " + mac + " error");
									}
		            			}else {
		            				System.out.printf("Protocol fail = 0x%2x", protocol3[2]);
		            			}
		            		}else {
		            			System.out.println("Protocol3 checksum fail");
		            		}
		            		try {
								getSQL().updateData(con, getSQL().getTableName()[0], getCSV().get_CSV_MAC_Array(value), 
										"SetupCode", getCSV().get_CSV_SetupCode_Array(value));
								getSQL().updateData(con, getSQL().getTableName()[0], getCSV().get_CSV_MAC_Array(value), 
										"SerialNumber", getCSV().get_CSV_SerialNumber_Array(value));
		            		} catch (SQLException e) {
								System.out.println("Update SC or SN error");
							}
	            		}else {
	            			System.out.println("Ameba not need upgrade");
	            			try {
								getSQL().update(con, getSQL().getTableName()[0], mac, product, fw_ver, link, "no",
										getCSV().get_CSV_SetupCode_Array(value), getCSV().get_CSV_SerialNumber_Array(value));
							} catch (SQLException e) {
								e.printStackTrace();
							}
	            		}
	            	}else {
	            		System.out.println("Protocol fail");
	            	}
	            }
	
	            try
	            {
	            	if(temp != null) {
	            		temp.close();
	            	}
	            	if(fos != null) {
	            		fos.close();
	            	}
	                if(input != null) {
	                    input.close();
	                }
	                if(output != null) {
	                    output.close();
	                }
	                if(this.clientSocket != null || !this.clientSocket.isClosed()){
	                    this.clientSocket.close();
	                    System.out.println("MAC: " + mac + " Client Socket close");
	                }
	            } catch (Exception e ) {
	            	System.out.println("input or output or clientSocket not close");
	                e.printStackTrace();
	            }
	            System.out.println("================Dividing Line=====================\n");
	        } else {
	        	System.out.println("Protocol read error");
	        }
        }
    }
}
