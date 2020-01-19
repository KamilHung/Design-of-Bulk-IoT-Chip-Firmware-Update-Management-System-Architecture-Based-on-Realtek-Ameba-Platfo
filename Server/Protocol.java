package paper;

public class Protocol {
	
	public boolean protocolCheck(byte[] protocol, int total, int order) {
		if(order == 1) {
			if(protocol[0] == (byte)0xAA && protocol[23 + protocol[21]] == 0x55 && 
            		(checkSum(protocol, 24 + protocol[21]) == protocol[22 + protocol[21]])) {
				return true;
			}else {
				return false;
			}
		}else {
			if(protocol[0] == (byte)0xAA && protocol[4] == 0x55 &&
					(checkSum(protocol, 5) == protocol[3])){
				return true;
			}else {
				return false;
			}
		}
	}
	
	public int receiveProtocolSize(byte[] protocol) {
		int count = 0;
		for(int i = 0; protocol[i] != 0x55; i++) {
			if(protocol[i] == (byte)0xAA) {
				count = 1;
			}
			if(count > 0) {
				count++;
			}
		}
		return count;
	}
	
	public byte checkSum(byte[] protocol, int total) {
    	byte sum = 0x00;
    	for(int a = 0; a < total; a++) {
    		System.out.printf("0x%x ", protocol[a]);
    	}
    	System.out.println(" ");
    	for(int i = 1; i <= total - 3; i++) { //Len~SN
    		sum += protocol[i];
    	}
    	//System.out.printf("sum = %2x\n", sum);
    	return sum;
    }

	//Protocol(28byte) = AA、Len(1byte)、MAC(6byte)、Product(1byte)、Version(3byte)、
    //                   SC(1byte){+SC(8byte)}、SN(1byte+?)、Update(1byte)、CheckSum(1byte)、55  
    public String protocolAnalysis(byte[] protocol, int order) {
    	String result = "";
    	switch(order) {
    		case 0: //MAC
    			byte[] mac_byte = new byte[6];
	    		System.arraycopy(protocol, 2, mac_byte, 0, 6); //截取protocol
	    		StringBuilder sb = new StringBuilder(12);
	    		for(byte a : mac_byte) {
	    			sb.append(String.format("%02x", a)); //將mac存在StringBuilder
	    		}
	    		result = new String(sb); //StringBuilder to String
	    		break;
    		case 1: //Product
    			if(protocol[8] == 00) {
        			result = "H1";
        		}else if(protocol[8] == 01) {
        			result = "M1";
        		}else if(protocol[8] == 02) {
        			result = "C1";
        		}else {
        			result = "H1";
        		}
    			break;
    		case 2: //Version
    			byte[] ver_byte = new byte[3];
        		System.arraycopy(protocol, 9, ver_byte, 0, 3);
        		StringBuilder ver = new StringBuilder(3);
        		for(byte a : ver_byte) {
        			ver.append(String.format("%02x", a));
        		}
        		result = new String(ver).substring(1, 6);
        		break;
    		case 3: //SetupCode
    			if(protocol[12] == 0x01) {
        			for(int i = 13; i < 21; i++) {
        				result += Character.toString((char)protocol[i]); //ASCII to Char
        			}
        		}
    			break;
    		case 4: //SerialNumber
    			if(protocol[21] > 0x00) {
        			for(int i = 22; i < 22 + protocol[21]; i++) {
        				result += Character.toString((char)protocol[i]); //ASCII to Char
        			}
        		}
    			break;
    		default:
    			break;
    	}
    	System.out.println("order = " + order + ", result = " + result);
    	return result;
    }
    
    public byte[] combinationArray2(int ans) {
    	byte[] result = new byte[5];
    	
    	result[0] = (byte)0xAA;
    	result[1] = (byte)3;
    	result[2] = (byte)ans;
    	result[3] = (byte)(result[1] + result[2]);
    	result[4] = 0x55;
   
    	return result;
    }
    
    //Protocol = AA、Len(1byte)、SC(1byte){+SC(8byte)}、SN(1byte){+SN(?byte)}、Update(1byte)、checkSum(1byte)、55
    public byte[] combinationArray(MyCSV csv, String A_SC, String A_SN, int order, boolean fw_check) {
		int sc_length = 1 + 8;
		int csv_sn_length = csv.get_CSV_SerialNumber_Array(order).length();
		int sn_length = 1 + csv_sn_length;
		int update_length = 1;
		int checksum_length = 1;
		int protocol_length = sc_length + sn_length + update_length + checksum_length;
    	byte[] result = new byte[protocol_length + 3];
    	
    	result[0] = (byte)0xAA;
    	result[1] = (byte)protocol_length;
    	
    	if(A_SC.equals(csv.get_CSV_SetupCode_Array(order))) { //SC equals Setupcode
    		result[2] = 0x00;
    	}else {
    		result[2] = 0x01;
    	}
    	
    	byte[] csv_sc_byte = csv.get_CSV_SetupCode_Array(order).getBytes();
    	for(int i = 3; i < 11; i++) { //SetupCode
    		result[i] = csv_sc_byte[i - 3];
    	}
    	
    	if(A_SN.equals(csv.get_CSV_SerialNumber_Array(order))) { //SN equals SerialNumber
    		result[11] = 0x00;
    	}else {
    		result[11] = (byte)csv_sn_length;
    	}
    	
    	int after_sn = 12 + csv_sn_length;
    	byte[] csv_sn_byte = csv.get_CSV_SerialNumber_Array(order).getBytes();
    	for(int i = 12; i < 12 + csv_sn_length; i++) { //SerialNumber
    		result[i] = csv_sn_byte[i - 12];
    	}
    	
    	if(fw_check) {
    		result[after_sn] = 0x01; //Update
    	}else {
    		result[after_sn] = 0x00; //not Update
    	}
    	
    	result[after_sn + 2] = 0x55;
    	//System.out.println("checksum: " + checkSum(result, result.length));
    	result[after_sn + 1] = checkSum(result, result.length); //checksum
    	
    	return result;
    }

	public static void main(String[] args) {
		Protocol p = new Protocol();
		MyCSV csv = new MyCSV();
		csv.myCSVInit(csv, "File", 100);
		
		int mac_lenght = 6;
		int product_length = 1;
		int version_length = 3;
		int sc_length = 1 + 8;
		int sn_length = 1 + 5;
		int checksum_length = 1;
		int protocol_length = mac_lenght + product_length + version_length + sc_length + sn_length + checksum_length;
		byte[] test = new byte[] {
				(byte)0xAA, //Start
				(byte)protocol_length,
				(byte)0x7C, (byte)0x7A, (byte)0x53, (byte)0x00, (byte)0x00, (byte)0x00, //mac
				0x01, //product
				0x01, 0x01, 0x01, //version
				0x01, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, //SC ascii : 0~9 => 0x30~0x39
				0x05, 0x4E, 0x30, 0x30, 0x30, 0x31, //SN ascii : A~Z => 0x41~0x5A
				0x00, //checksum
				0x55//End
				};
		
		String mac = p.protocolAnalysis(test, 0);
		System.out.println(mac);
		String product = p.protocolAnalysis(test, 1);
		System.out.println(product);
		String ver = p.protocolAnalysis(test, 2);
		System.out.println(ver);
		String sc = p.protocolAnalysis(test, 3);
		System.out.println(sc);
		String sn = p.protocolAnalysis(test, 4);
		System.out.println(sn);
		String ll = p.protocolAnalysis(test, 5);
		System.out.println(ll);
		
		byte sum = 0x00;
		int i = 1;
		for(i = 1; i <= protocol_length; i++) {
			sum += test[i];
		}
		p.receiveProtocolSize(test);
		
		System.out.printf("sum = 0x%2x, i = %d\n", sum, i);
		test[27] = sum;
		System.out.println(p.checkSum(test, 29) == test[27]);
		
		byte[] a = new byte[20];
		a = p.combinationArray(csv, "11223344", "N0002", 0, false);
		for(i = 0; i < 20; i++) {
			System.out.printf("0x%x ", a[i]);
		}
	}

}
