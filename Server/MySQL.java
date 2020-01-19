package paper;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

public class MySQL {
	private String[] sql_MAC_Array;
	private String[] tableName;
	
	public void setTableName(String name, int value) {
		tableName[value] = name;
	}
	
	public String[] getTableName() {
		return tableName;
	}
	
	public void setMaxMACQuantity(int value) {
		sql_MAC_Array = new String[value];
		tableName = new String[value];
	}
	
	public void set_SQL_MAC_Array(String MAC, int value) {
		sql_MAC_Array[value] = MAC;
	}
	
	public String[] get_SQL_MAC_Array() {
		return sql_MAC_Array;
	}
	
	//Connect Database
    public Connection getConnection(String dbName) throws SQLException
    {
        SQLiteConfig config = new SQLiteConfig();  
        config.setSharedCache(true);
        config.enableRecursiveTriggers(true);
    
        SQLiteDataSource ds = new SQLiteDataSource(config); 
        ds.setUrl("jdbc:sqlite:" + dbName);
        return ds.getConnection();
    }
    
    //Create Table
    public void createTable(Connection con, String tableName)throws SQLException{
        String sql = 
        		"DROP TABLE IF EXISTS " + tableName + "; "	+
        		" CREATE TABLE " + tableName + "("		+		
        		" 'MAC' VARCHAR(12) PRIMARY KEY, "		+
        		" 'Product' VARCHAR(2) NOT NULL, "		+
        		" 'FW_ver' VARCHAR(5) NOT NULL, "		+
        		" 'CheckFW' INTEGER NOT NULL, "			+
        		" 'Renew' VARCHAR(3) NOT NULL, "		+
        		" 'SetupCode' VARCHAR(8) NOT NULL, "	+
        		" 'SerialNumber' VARCHAR(5) NOT NULL, "	+
        		" 'Date' DATETIME);";
        Statement stat = null;
        stat = con.createStatement();
        stat.executeUpdate(sql);
    }
    
    //Drop table
    public void dropTable(Connection con, String tableName)throws SQLException{
        String sql = "drop table " + tableName;
        Statement stat = null;
        stat = con.createStatement();
        stat.executeUpdate(sql);
    }
    
    //MAC(VARCHAR) ' Product(VARCHAR) ' FW_ver(VARCHAR) ' CheckFW(INTEGER) ' Re-new(VARCHAR)
    public void insert(Connection con, String tableName, String MAC, String Product,
    		String FW_ver, int CheckFW, String Renew, String SC, String SN)throws SQLException{
    	String sql = "insert into " + tableName
    			+ " (MAC, Product, FW_ver, CheckFW, Renew, SetupCode, SerialNumber, Date)"
    			+ " values(?,?,?,?,?,?,?,?)";
        PreparedStatement pst = null;
        pst = con.prepareStatement(sql);
        int idx = 1; 
        pst.setString(idx++, MAC);
        pst.setString(idx++, Product);
        pst.setString(idx++, FW_ver);
        pst.setInt(idx++, CheckFW);
        pst.setString(idx++, Renew);
        pst.setString(idx++, SC);
        pst.setString(idx++, SN);
        pst.setTimestamp(idx++, null);
        pst.executeUpdate();
    }
    
    //�ק�  "set" sequence
    public void update(Connection con, String tableName, String MAC, String Product,
    		String FW_ver, int CheckFW, String Renew, String SC, String SN)throws SQLException{
        String sql = "update " + tableName + " set Product = ?, FW_ver = ?, "
        		+ "CheckFW = ?, Renew = ?, SetupCode = ?, SerialNumber = ?, Date = ? where MAC = ?";
        PreparedStatement pst = null;
        pst = con.prepareStatement(sql);
        int idx = 1;
        pst.setString(idx++, Product);
        pst.setString(idx++, FW_ver);
        pst.setInt(idx++, CheckFW);
        pst.setString(idx++, Renew);
        pst.setString(idx++, SC);
        pst.setString(idx++, SN);
        pst.setTimestamp(idx++, Timestamp.valueOf(LocalDateTime.now()));
        pst.setString(idx++, MAC);
        pst.executeUpdate();
    }
    
    //�R���Y�ӯS�wMAC
    public void delete(Connection con, String tableName, String MAC)throws SQLException{
        String sql = "delete from " + tableName + " where MAC = ?";
        PreparedStatement pst = null;
        pst = con.prepareStatement(sql);
        int idx = 1; 
        pst.setString(idx++, MAC);
        pst.executeUpdate();
    }
    
    //�L�X MAC ' Product ' FW_ver ' CheckFW ' Re-new ' SetupCode ' SerialNumber ' Date 
    public void selectAll(Connection con, String tableName)throws SQLException{
        String sql = "select * from " + tableName;
        Statement stat = null;
        ResultSet rs = null;
        stat = con.createStatement();
        rs = stat.executeQuery(sql);
        int count = 0;
        System.out.println("Print DataBase:");
        while(rs.next())
        {
            System.out.println(
            		rs.getString("MAC")			+ "\t" + 
					rs.getString("Product")		+ "\t" + 
					rs.getString("Fw_ver")		+ "\t" + 
					rs.getInt("CheckFW")		+ "\t" +
					rs.getString("Renew")		+ "\t" +
					rs.getString("SetupCode")	+ "\t" +
					rs.getString("SerialNumber")+ "\t" +
					rs.getString("Date"));
            set_SQL_MAC_Array(rs.getString("MAC"), count); //�]�wDB��ƨ�Array
            count++;
        }
    }
    
    
    public boolean selectMAC(Connection con, String tableName, String MAC)throws SQLException{
    	String sql = "select * from " + tableName;
        Statement stat = null;
        ResultSet rs = null;
        stat = con.createStatement();
        rs = stat.executeQuery(sql);
        while(rs.next()) {
	        if(rs.getString("MAC").compareTo(MAC) == 0) {
	        	return true;
	        }
        }
        return false;
    }
    
    public String selectField(Connection con, String tableName, 
    		String MAC, String Field)throws SQLException{
    	String sql = "select * from " + tableName;
        Statement stat = null;
        ResultSet rs = null;
        stat = con.createStatement();
        rs = stat.executeQuery(sql);
        while(rs.next()) {
	        if(rs.getString("MAC").compareTo(MAC) == 0) {
	        	return rs.getString(Field);
	        }
        }
        return null;
    }
    
    public ArrayList<Object> selectMACAllData(Connection con, 
    		String tableName, String MAC) {
    	String sql = "select * from " + tableName;
        Statement stat = null;
        ResultSet rs = null;
        try {
	        stat = con.createStatement();
	        rs = stat.executeQuery(sql);
	        while(rs.next()) {
		        if(rs.getString("MAC").compareTo(MAC) == 0) {
		        	ArrayList<Object> list = new ArrayList<Object>();
					list.add(rs.getString("MAC"));
		        	list.add(rs.getString("Product"));
		        	list.add(rs.getString("Fw_ver"));
		        	list.add(rs.getInt("CheckFW"));
		        	list.add(rs.getString("Renew"));
		        	list.add(rs.getString("SetupCode"));
		        	list.add(rs.getString("SerialNumber"));
		        	list.add(rs.getString("Date"));
		        	System.out.println(list);
		        	return list;
		        }
	        }
        } catch (SQLException e) {
        	System.out.println("ArrayList error");
        }
        return null;
    }
    
    public int selectCheckFW(Connection con, String tableName, 
    		String MAC)throws SQLException{
    	String sql = "select * from " + tableName;
        Statement stat = null;
        ResultSet rs = null;
        stat = con.createStatement();
        rs = stat.executeQuery(sql);
        while(rs.next()) {
	        if(rs.getString("MAC").compareTo(MAC) == 0) {
	        		return rs.getInt("CheckFW");
	        }
        }
        return -1;
    }
    
    //�ק�Renew
    public void updateRenew(Connection con, String tableName, String MAC, String Renew)throws SQLException{
        String sql = "update " + tableName + " set Renew = ? where MAC = ?";
        PreparedStatement pst = null;
        pst = con.prepareStatement(sql);
        int idx = 1 ; 
        pst.setString(idx++, Renew);
        pst.setString(idx, MAC);
        pst.executeUpdate();
    }
    
    //�ק�YMAC���YField(VARCHAR)
    public void updateData(Connection con, String tableName, 
    		String MAC, String Field, String Data)throws SQLException {
    	String sql = "update " + tableName + " set " + Field + " = ? where MAC = ?";
    	PreparedStatement pst = null;
        pst = con.prepareStatement(sql);
        int idx = 1 ; 
        pst.setString(idx++, Data);
        pst.setString(idx, MAC);
        pst.executeUpdate();
    }
    
	//Overloading method �ק�YMAC���YField(INTEGER)
    public void updateData(Connection con, String tableName, 
    		String MAC, String Field, int Data)throws SQLException {
    	String sql = "update " + tableName + " set " + Field + " = ? where MAC = ?";
    	PreparedStatement pst = null;
        pst = con.prepareStatement(sql);
        int idx = 1 ; 
        pst.setInt(idx++, Data);
        pst.setString(idx, MAC);
        pst.executeUpdate();
    }
    
    public void firstInsert(MySQL sql, Connection con, String table, String MAC, String SC, String SN) {
    	try {
			sql.insert(con, table, MAC, "null", "00000", 0, "yes", "null", "null");//Insert first data
		} catch (SQLException e) {
			System.out.println("First Insert error");
		}
    }
    
    public void dbCompareCSV(MySQL sql, Connection con, MyCSV csv) {
    	boolean exist = false;
    	for(String macTemp:get_SQL_MAC_Array()) { //�qArray�����XDB MAC
    		if(macTemp == null) {
    			return;
    		}
    		for(int i = 0; i < csv.getCSVQuantity(); i++) {
    			//DB MAC���CSV MAC
    			if(macTemp.compareTo(csv.get_CSV_MAC_Array(i)) == 0) { 
    				exist = true;
    				break;
    			}else {
    				exist = false;
    			}
    		}
    		if(exist == false) { //DB MAC��������
	    		try {//�j�M�ƥ�Table�O�_�s�b�P��MAC
	    			if(sql.selectMAC(con, sql.getTableName()[1], macTemp)) { 
	    				System.out.println("drop " + macTemp);
	    				sql.delete(con, sql.getTableName()[1], macTemp);//���ۦPMAC�h�NMAC�R��
	    			}
	    			copyTable(sql, con, macTemp); //�NMAC�ƻs��ƥ�Table
					sql.delete(con, sql.getTableName()[0], macTemp);//�R��MAC
				} catch (SQLException e) {
					System.out.println("delete MAC error");
				}
    		}
    	}
    }

    public void copyTable(MySQL sql, Connection con, String mac) {
    	ArrayList<Object> list = selectMACAllData(con, sql.getTableName()[0], mac);
    	//mac, product, fw_ver, checkfw, renew
    	String product = list.get(1).toString();
    	String fw_ver = list.get(2).toString();
    	int checkfw = Integer.parseInt(list.get(3).toString());
    	String renew = list.get(4).toString();
    	String sc = list.get(5).toString();
    	String sn = list.get(6).toString();
    	try {
			sql.insert(con, sql.getTableName()[1], mac, product, fw_ver, checkfw, renew, sc, sn);
		} catch (SQLException e) {
			System.out.println("SQL copy Table error");
		}
    }
    
    public void csvCompareDB(MySQL sql, Connection con, MyCSV csv, int count) {
    	try {
    		System.out.println(count + " " + csv.get_CSV_MAC_Array(count));
			if(!sql.selectMAC(con, sql.getTableName()[0], csv.get_CSV_MAC_Array(count)))
				sql.firstInsert(sql, con, sql.getTableName()[0], 
						csv.get_CSV_MAC_Array(count),
						csv.get_CSV_SetupCode_Array(count), 
						csv.get_CSV_SerialNumber_Array(count)); //insert MAC
		} catch (SQLException e) {
			System.out.println("MAC manager error");
		}
	}
    
    public void csvToDB(MySQL sql, Connection con, MyCSV csv) {
    	try {
			sql.selectAll(con, sql.getTableName()[0]); //�j�MDB�Ҧ�MAC��bArray
		} catch (SQLException e) {
			System.out.println("select error");
		}
    	for(int i = 0; i < csv.getCSVQuantity(); i++) {
			sql.csvCompareDB(sql, con, csv, i);//CSV���DB
		}
		sql.dbCompareCSV(sql, con, csv); //DB���CSV
    }

    public static void main(String args[]) { //for SQLite Test
    	MyCSV test = new MyCSV();
		test.myCSVInit(test, "File", 100);
		MySQL sql = new MySQL();
		sql.setMaxMACQuantity(100);
		sql.setTableName("Now", 0); //Table Name <= 5 char
		sql.setTableName("Old", 1);
		try {
			Connection con = sql.getConnection("Ameba.db");
			sql.createTable(con, sql.getTableName()[0]);
			sql.createTable(con, sql.getTableName()[1]);
			sql.firstInsert(sql, con, sql.getTableName()[0], "7c7a53999999", "00000000", "A0000");
			sql.firstInsert(sql, con, sql.getTableName()[0], "7c7a53999998", "11111111", "A0001");
			sql.csvToDB(sql, con, test);
			sql.update(con, sql.getTableName()[0], test.get_CSV_MAC_Array(0), 
					test.get_CSV_Product_Array(0), test.get_CSV_Update_Array(0), 1, "no",
					test.get_CSV_SetupCode_Array(0), test.get_CSV_SerialNumber_Array(0));
			sql.updateData(con, sql.getTableName()[0], test.get_CSV_MAC_Array(1), "Product", "Z1");
			sql.selectAll(con, sql.getTableName()[0]);
		} catch (SQLException e) {
			System.out.println("Connection SQL error");
		}
    }
}