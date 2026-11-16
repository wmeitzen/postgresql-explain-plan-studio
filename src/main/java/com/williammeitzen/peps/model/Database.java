package com.williammeitzen.peps.model;

//import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;

//import com.github.cliftonlabs.json_simple.JsonException;
//import com.github.cliftonlabs.json_simple.JsonArray;
//import com.github.cliftonlabs.json_simple.JsonObject;
//import com.github.cliftonlabs.json_simple.JsonKey;


import com.williammeitzen.peps.gui.Utils;

public final class Database {
	
	private static double SQLITE_DB_SCHEMA_VERSION = 4.13;
	private static Connection objConnection = null;
	private static Connection objPGConnection = null;
	
	public Database() {
	}

	public static double dbSchemaVersionNumber() {
		return SQLITE_DB_SCHEMA_VERSION;
	}
	
	//public static void pg_init(String strServer, String strPort, String strDatabase, String strUser, String strPassword, String strPreCommand) {
	public static List<Object> pg_init(String strServer, String strPort, String strDatabase, String strUser, String strPassword, String strPreCommand) {

        String strURL;
    	boolean bolExceptionOccurred = false;
    	StringWriter objError = new StringWriter();
    	String strError = null;

    	if (bolExceptionOccurred == false) {
			try {
				Class.forName("org.postgresql.Driver");
			} catch (Exception e) {
	        	strError = "Error from Database.pg_init().Class.forName:\n" + e.getMessage() + "\n";
				bolExceptionOccurred = true;
			}
    	}
		
		strURL = "jdbc:postgresql://" + strServer;
		if (strPort != null && strPort.length() > 0) {
			strURL += ":" + strPort;
		}
		if (strDatabase != null && strDatabase.length() > 0) {
			strURL += "/" + strDatabase;
		}

		if (bolExceptionOccurred == false) {
			try {
				objPGConnection = DriverManager.getConnection(strURL, strUser, strPassword);
			} catch (SQLException e) {
				//System.out.println("function pg(): DriverManager.getConnection");
	        	strError = "Error from Database.pg_init().DriverManager.getConnection():\nJDBC URL: " + strURL + " as " + strUser + "\n"+ e.getMessage() + "\n";
				bolExceptionOccurred = true;
			}
		}

		if (bolExceptionOccurred == false) {
			if (strPreCommand != null && strPreCommand.length() > 0) {
				List<Object> objPGPreCommand = pg_dml(strPreCommand, ";");
				bolExceptionOccurred = (boolean) objPGPreCommand.get(0);
				strError = (String) objPGPreCommand.get(1);
			}
		}
		return Arrays.asList(bolExceptionOccurred, strError);
	}
	
	///public static String pg_getPlan(String strSQL) {
	public static List<Object> pg_getPlan(String strSQL) {

		Statement objS = null;
        ResultSet objRS = null;
    	String strJsonPlan = null;
    	boolean bolExceptionOccurred = false;
    	StringWriter objError = new StringWriter();
    	String strError = null;
    	
    	boolean bolShowTiming = false;

		if (bolShowTiming == true) { System.out.println("before Database.pg_getPlan(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_getPlan().objPGConnection.createStatement(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolExceptionOccurred == false) {
			try {
				objS = objPGConnection.createStatement();
			} catch (SQLException e) {
				strError = "Error from Database.pg_getPlan().objPGConnection.createStatement():\n" + e.getMessage() + "\n";
				bolExceptionOccurred = true;
			}
		}
		if (bolShowTiming == true) { System.out.println("after Database.pg_getPlan().objPGConnection.createStatement(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_getPlan().objS.executeQuery(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolExceptionOccurred == false) {
			try {
	            objRS = objS.executeQuery(strSQL);
	        } catch (SQLException e) {
	        	strError = "Error from Database.pg_getPlan().objS.executeQuery():\n" + e.getMessage() + "\n";
				bolExceptionOccurred = true;
	        }
		}
		if (bolShowTiming == true) { System.out.println("after Database.pg_getPlan().objS.executeQuery(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_getPlan().strJsonPlan = objRS.getString(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolExceptionOccurred == false) {
			try {
				StringBuilder planBuilder = new StringBuilder();
				boolean firstRow = true;
				while (objRS.next()) {
					String row = objRS.getString(1);
					if (row == null) {
						continue;
					}
					if (!firstRow) {
						planBuilder.append('\n');
					}
					planBuilder.append(row);
					firstRow = false;
				}
				strJsonPlan = planBuilder.length() == 0 ? null : planBuilder.toString();
			} catch (SQLException e) {
				strError = "Error from Database.pg_getPlan().objRS.getString():\n" + e.getMessage() + "\n";
				bolExceptionOccurred = true;
			}
		}
		if (bolShowTiming == true) { System.out.println("before Database.pg_getPlan().strJsonPlan = objRS.getString(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_getPlan().objRS.close(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		if (bolExceptionOccurred == false) {
			if (objRS != null) {
	            try {
	                objRS.close();
	            } catch (SQLException e) {
	            	strError = "Error from Database.pg_getPlan().objRS.close():\n" + e.getMessage() + "\n";
	    			bolExceptionOccurred = true;
	            }
	        }
		}
		if (bolShowTiming == true) { System.out.println("after Database.pg_getPlan().objRS.close(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("after Database.pg_getPlan(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

        //return strJsonPlan;
		return Arrays.asList(bolExceptionOccurred, strError, strJsonPlan);
		//return List<bolExceptionOccurred, strError, strJsonPlan>;
	}

	// 
    //public static void pg_dml(String strSQLStrings, String strSeparator) {
	public static List<Object> pg_dml(String strSQLStrings, String strSeparator) {
        Statement objStmt = null;
        String[] arySQLStrings;
        String strSQL;
    	boolean bolExceptionOccurred = false;
    	StringWriter objError = new StringWriter();
    	String strError = null;

        arySQLStrings = strSQLStrings.split(strSeparator);
        
        for (int intString = 0; intString < arySQLStrings.length; intString++) {
        	strSQL = arySQLStrings[intString];
        	if (strSQL.equals("") == false && strSQL.matches("^ +$") == false) { // if we get only spaces, skip
		        try {
		            objStmt = objPGConnection.createStatement();
		            objStmt.executeUpdate(strSQL);
		        } catch (SQLException e) {
		            strError = "Error from Database.pg_dml().objPGConnection.createStatement():\nSQL: " + strSQL + "\n" + e.getMessage() + "\n";
	    			bolExceptionOccurred = true;
		        }
	        }
        }
        return Arrays.asList(bolExceptionOccurred, strError);
    }

	public static JsonObject pg_row(String strSQL) {
    	boolean bolShowTiming = false;
    	
		Statement objS = null;
        ResultSet objRS = null;
        ResultSetMetaData objRSMD = null;
        
        JsonObject jsnRow = new JsonObject();
        
    	int numColumns = 0;

		if (bolShowTiming == true) { System.out.println("before Database.pg_row(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_row().objPGConnection.createStatement(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		try {
			objS = objPGConnection.createStatement();
		} catch (SQLException e) {
			System.out.println("function Database.pg_row(): objConnection.createStatement");
			e.printStackTrace();
		}
		if (bolShowTiming == true) { System.out.println("after Database.pg_row().objPGConnection.createStatement(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_row().objS.executeQuery(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
        try {
            objRS = objS.executeQuery(strSQL);
        } catch (SQLException ex) {
        	System.out.println("function Database.pg_row(): objS.executeQuery");
            System.out.println("strSQL: " + strSQL);
            ex.printStackTrace();
        }
		if (bolShowTiming == true) { System.out.println("after Database.pg_row().objS.executeQuery(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		
		if (bolShowTiming == true) { System.out.println("before Database.pg_row().objRS.getMetaData(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
        try {
            objRSMD = objRS.getMetaData();
        } catch (SQLException ex) {
        	System.out.println("function Database.pg_row() 3");
            ex.printStackTrace();
        }
        try {
            numColumns = objRSMD.getColumnCount();
        } catch (SQLException ex) {
        	System.out.println("function Database.pg_row() 4");
            ex.printStackTrace();
        }
		if (bolShowTiming == true) { System.out.println("after Database.pg_row().objRS.getMetaData(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_row().strJsonPlan = objRS.getString(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

        try {
			if (objRS.next()) {
			  //JsonObject obj = new JsonObject();
			  for (int i=1; i <= numColumns; i++) {
			    //String column_name = objRSMD.getColumnName(i);
			    String column_name = "";
			    try {
			        column_name = objRSMD.getColumnName(i);
			    } catch (SQLException ex) {
			    	System.out.println("function Database.pg_row() 11");
			        ex.printStackTrace();
			    }
			    try {
			        //obj.put(column_name, rs.getObject(column_name));
			        jsnRow.put(column_name, objRS.getObject(column_name));
			        //obj.put(column_name, i)
			    } catch (SQLException ex) {
			    	System.out.println("function Database.pg_row() 6");
			        ex.printStackTrace();
			    }
			  }
			  //json.put(obj);
			  //json.add(obj);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (bolShowTiming == true) { System.out.println("before Database.pg_row().strJsonPlan = objRS.getString(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_row().objRS.close(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
        if (objRS != null) {
            try {
                objRS.close();
            } catch (SQLException ex) {
            	System.out.println("function pg(): objRS.close");
                ex.printStackTrace();
            }
        }
		if (bolShowTiming == true) { System.out.println("after Database.pg_row().objRS.close(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("after Database.pg_row(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

        return jsnRow;
    }
    
    public static JsonArray pg_rows(String strSQL) {
    	boolean bolShowTiming = false;
    	
		Statement objS = null;
        ResultSet objRS = null;
        ResultSetMetaData objRSMD = null;
        JsonArray json = new JsonArray();

        JsonObject jsnRow = new JsonObject();
        
    	int numColumns = 0;

		if (bolShowTiming == true) { System.out.println("before Database.pg_rows(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_rows().objPGConnection.createStatement(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		try {
			objS = objPGConnection.createStatement();
		} catch (SQLException e) {
			System.out.println("function Database.pg_rows(): objConnection.createStatement");
			e.printStackTrace();
		}
		if (bolShowTiming == true) { System.out.println("after Database.pg_rows().objPGConnection.createStatement(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_rows().objS.executeQuery(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
        try {
            objRS = objS.executeQuery(strSQL);
        } catch (SQLException ex) {
        	System.out.println("function Database.pg_rows(): objS.executeQuery");
            System.out.println("strSQL: " + strSQL);
            ex.printStackTrace();
        }
		if (bolShowTiming == true) { System.out.println("after Database.pg_rows().objS.executeQuery(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		
		if (bolShowTiming == true) { System.out.println("before Database.pg_rows().objRS.getMetaData(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
        try {
            objRSMD = objRS.getMetaData();
        } catch (SQLException ex) {
        	System.out.println("function Database.pg_rows() 3");
            ex.printStackTrace();
        }
        try {
            numColumns = objRSMD.getColumnCount();
        } catch (SQLException ex) {
        	System.out.println("function Database.pg_rows() 4");
            ex.printStackTrace();
        }
		if (bolShowTiming == true) { System.out.println("after Database.pg_rows().objRS.getMetaData(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_rows().strJsonPlan = objRS.getString(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
/*
		try {
			if (objRS.next()) {
				strJsonPlan = objRS.getString(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
*/
        try {
			if (objRS.next()) {
			  //JsonObject obj = new JsonObject();
			  for (int i=1; i <= numColumns; i++) {
			    //String column_name = objRSMD.getColumnName(i);
			    String column_name = "";
			    try {
			        column_name = objRSMD.getColumnName(i);
			    } catch (SQLException ex) {
			    	System.out.println("function Database.pg_rows() 11");
			        ex.printStackTrace();
			    }
			    try {
			        //obj.put(column_name, rs.getObject(column_name));
			        jsnRow.put(column_name, objRS.getObject(column_name));
			        //obj.put(column_name, i)
			    } catch (SQLException ex) {
			    	System.out.println("function Database.pg_rows() 6");
			        ex.printStackTrace();
			    }
			  }
			  //json.put(obj);
			  //json.add(obj);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

        try {
			while (objRS.next()) {
			  JsonObject obj = new JsonObject();
			  for (int i=1; i <= numColumns; i++) {
			    //String column_name = objRSMD.getColumnName(i);
			    String column_name = "";
			    try {
			        column_name = objRSMD.getColumnName(i);
			    } catch (SQLException ex) {
			    	System.out.println("function Database.pg_rows() 10");
			        //Logger.getLogger(PEG.class.getName()).log(Level.SEVERE, null, ex);
			    }
			    try {
			        //obj.put(column_name, rs.getObject(column_name));
			        obj.put(column_name, objRS.getObject(column_name));
			        //obj.put(column_name, i)
			    } catch (SQLException ex) {
			    	System.out.println("function Database.pg_rows() 6");
			        //Logger.getLogger(PEG.class.getName()).log(Level.SEVERE, null, ex);
			    }
			  }
			  //json.put(obj);
			  json.add(obj);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
        
		if (bolShowTiming == true) { System.out.println("before Database.pg_rows().strJsonPlan = objRS.getString(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("before Database.pg_rows().objRS.close(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
        if (objRS != null) {
            try {
                objRS.close();
            } catch (SQLException ex) {
            	System.out.println("function pg(): objRS.close");
                ex.printStackTrace();
            }
        }
		if (bolShowTiming == true) { System.out.println("after Database.pg_rows().objRS.close(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

		if (bolShowTiming == true) { System.out.println("after Database.pg_rows(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }

        return json;
    }
    
	public static void init() {
		String strSQL;

		boolean bolShowTiming = false;
		if (bolShowTiming == true) { System.out.println("before Database.init(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
		Statement stmtObj = null;

		try {
			Class.forName("org.sqlite.JDBC");
		} catch (Exception e) {
			System.out.println("function Database() 1");
			e.printStackTrace();
		}
		
		String url = AppPaths.sqliteJdbcUrl();
		Path sqlitePath = AppPaths.sqliteDatabaseFile();
		try {
			objConnection = DriverManager.getConnection(url);
			// Runtime defaults can still be ~1MB; raise so large EXPLAIN JSON is allowed.
			if (objConnection instanceof org.sqlite.SQLiteConnection) {
				org.sqlite.SQLiteConnection sc = (org.sqlite.SQLiteConnection) objConnection;
				sc.setLimit(org.sqlite.SQLiteLimits.SQLITE_LIMIT_LENGTH, 1_000_000_000);
				sc.setLimit(org.sqlite.SQLiteLimits.SQLITE_LIMIT_SQL_LENGTH, 1_000_000_000);
			}
			// Apply once at connect. Toggling journal_mode around each dml() fails with
			// "Safety level may not be changed inside a transaction" when callers pass
			// begin/commit batches (plan property ingest).
			try (Statement pragmaStmt = objConnection.createStatement()) {
				pragmaStmt.executeUpdate("PRAGMA synchronous=OFF");
				pragmaStmt.executeUpdate("PRAGMA journal_mode=MEMORY");
			}
		} catch (SQLException e) {
			//System.out.println("function Database() 2");
			//e.printStackTrace(); //  too much info
			//System.out.println("getSQLState(): " + e.getSQLState());
			//System.out.println("getErrorCode(): " + e.getErrorCode());
			System.out.println("PostgreSQL Explain Plan Studio | SQLite error:");
			System.out.println(e.getMessage());
			System.out.println("The application is unable to create or open SQLite database:");
			System.out.println(sqlitePath.toAbsolutePath());
			System.out.println("It might not have write permissions to the data directory.");
			System.out.println("Override with PEPS_DATA_DIR or -Dpeps.data.dir=<path>.");
			System.out.println("The application cannot function without a SQLite database file.  Exiting.");
			System.exit(-1);
		}
		try {
			stmtObj = objConnection.createStatement();
		} catch (SQLException e) {
			System.out.println("function Database() 3");
			e.printStackTrace();
		}
		
		// see if config table exists
		// if so, check db alter version.
		// if it doesn't match dblDBAlterVersion, or , drop all tables. they'll be created after this.
		JsonObject joRow;
		//JsonKey joRow;
		JsonArray jaRows;
		double dblDBConfigVersion;
		int intDBConfigVersionMajor; 
		String strDBConfigVersionMinor;
		int intDBSchemaVersionNumberMajor = (int) dbSchemaVersionNumber();
		String strDBSchemaVersionNumberMinor = Utils.roundValue(dbSchemaVersionNumber() - intDBSchemaVersionNumberMajor, 4);

		strSQL = "select count(*) as num from sqlite_master where type='table' and name='config';";
		joRow = Database.row(strSQL);
		if (joRow.getInteger("num") > 0) {
		//if (joRow.getObject("num") > 0) {
			strSQL = "select db_alter_version from config;";
			joRow = Database.row(strSQL);
			dblDBConfigVersion = joRow.getDouble("db_alter_version");
		} else {
			dblDBConfigVersion = 0.0;
		}
		intDBConfigVersionMajor = (int) dblDBConfigVersion;
		strDBConfigVersionMinor = Utils.roundValue(dblDBConfigVersion - intDBConfigVersionMajor, 4);

		if (intDBConfigVersionMajor != intDBSchemaVersionNumberMajor) {
			strSQL = "select name as table_name from sqlite_master where type='table';";
			jaRows = Database.rows(strSQL);
			for (int intRow = 0; intRow < jaRows.size(); intRow++) {
				strSQL = "drop table if exists " + ((JsonObject) jaRows.get(intRow)).get("table_name") + ";";
				Database.dml(strSQL);
			}
		}

        strSQL = "create table if not exists config (\n"
        		+ "db_alter_version float not null default " + dbSchemaVersionNumber() + "\n"
        		+ ", outside_box_left_margin int not null default 5\n"
        		+ ", outside_box_right_margin int not null default 5\n"
        		+ ", outside_box_top_margin int not null default 3\n"
        		+ ", outside_box_bottom_margin int not null default 5\n"
        		+ ", inside_box_left_margin int not null default 5\n"
        		+ ", inside_box_right_margin int not null default 5\n"
        		+ ", inside_box_top_margin int not null default 0\n"
        		+ ", inside_box_bottom_margin int not null default 0\n"
        		+ ", arrow_text_bottom_margin int not null default 2\n"
        		+ ", application_width int not null default 900\n"
        		+ ", application_height int not null default 600\n"
        		+ ", color_scheme varchar(32) not null default 'dark'\n"
        		+ ");\n";
        Database.dml(strSQL);

		strSQL = "select count(*) as num_rows from config";
		joRow = Database.row(strSQL);
		if (joRow.getInteger("num_rows") == 0) {
	        strSQL = "insert into config default values;\n";
	        Database.dml(strSQL);
		}

		// Existing DBs created before color_scheme: add the column if missing
		ensureConfigColumn("color_scheme", "varchar(32) not null default 'dark'");
		ensureConfigColumn("docking_layout", "text");
		ensureConfigColumn("plan_source_mode", "varchar(32) not null default 'paste'");
		ensureConfigColumn("selected_plan_name", "text");
		ensureConfigColumn("selected_plan_id", "integer");
		ensureConfigColumn("plans_sort_name_order", "varchar(16) not null default 'asc'");
		ensureConfigColumn("plans_sort_grouping", "varchar(32) not null default 'folders_first'");
		ensureConfigColumn("plans_sort_mode", "varchar(16) not null default 'by_name'");

		// 4.09: factory color scheme is Dark (was Default). Migrate only the old factory value.
		if (dblDBConfigVersion > 0.0 && dblDBConfigVersion < 4.09) {
			Database.dml("update config set color_scheme = 'dark' where color_scheme = 'default';");
		}
		
		// just in case the db schema minor version changed
        strSQL = "update config set db_alter_version = " + dbSchemaVersionNumber() + ";\n";
        Database.dml(strSQL);
        
		// if major or minor db version number change, drop / recreate table
		if (intDBConfigVersionMajor != intDBSchemaVersionNumberMajor || strDBConfigVersionMinor.equals(strDBSchemaVersionNumberMinor) == false) {
			strSQL = "drop table if exists explain_plan_operator_annotations;";
			Database.dml(strSQL);
		}

        // create table to annotate each operator
        strSQL = "create table if not exists explain_plan_operator_annotations (\n"
    		+ "id_explain_plan int not null\n"
    		+ ", parent_id_explain_plan_property int\n"
    		+ ", id_explain_plan_property int not null\n"
    		+ ", x_cart int\n"
    		+ ", y_cart int\n"
    		+ ", parent_x_cart int\n"
    		+ ", parent_y_cart int\n"
    		+ ", column_width int\n"
    		+ ", row_height int\n"
    		+ ", column_boundary_left int\n"
    		//+ ", previous_x_rect int\n"
    		+ ", column_boundary_right int\n"
    		+ ", row_boundary_top int\n"
    		+ ", row_boundary_bottom int\n"
    		//+ ", previous_y_rect int\n"
    		+ ", box_left int\n"
    		+ ", box_right int\n"
    		+ ", box_top int\n"
    		+ ", box_bottom int\n"
    		//+ ", operator_box_top_y_rect int\n"
    		//+ ", operator_box_bottom_y_rect int\n"
    		+ ", gather_ancestor_actual_loops int\n"

			// values from operator properties
    		+ ", actual_loops varchar(256)\n"
    		+ ", actual_rows varchar(256)\n"
    		+ ", actual_startup_time_ms float\n"
    		+ ", actual_total_time_ms float\n"
    		+ ", alias varchar(256)\n"
    		+ ", append varchar(256)\n"
    		+ ", command varchar(256)\n"
    		+ ", cte_name varchar(256)\n"
    		+ ", execution_time varchar(256)\n"
    		+ ", filter varchar(256)\n"
    		+ ", function_call varchar(256)\n"
    		+ ", cache_evictions varchar(256)\n"
    		+ ", cache_hits varchar(256)\n"
    		+ ", cache_key varchar(256)\n"
    		+ ", cache_misses varchar(256)\n"
    		+ ", cache_mode varchar(256)\n"
    		+ ", cache_overflows varchar(256)\n"
    		+ ", estimated_capacity varchar(256)\n"
    		+ ", estimated_distinct_lookup_keys varchar(256)\n"
    		+ ", estimated_hit_percent varchar(256)\n"
    		+ ", estimated_lookups varchar(256)\n"
    		+ ", hash_batches varchar(256)\n"
    		+ ", hash_buckets varchar(256)\n"
    		+ ", hash_cond varchar(256)\n"
    		+ ", index_cond varchar(256)\n"
    		+ ", index_name varchar(256)\n"
    		+ ", initplan varchar(256)\n"
    		+ ", io_read_time varchar(256)\n"
    		+ ", io_write_time varchar(256)\n"
    		+ ", join_filter varchar(256)\n"
    		+ ", join_type varchar(256)\n"
    		+ ", local_dirtied_blocks varchar(256)\n"
    		+ ", local_hit_blocks varchar(256)\n"
    		+ ", local_written_blocks varchar(256)\n"
    		+ ", local_read_blocks varchar(256)\n"
    		+ ", merge_cond varchar(256)\n"
    		+ ", node_type varchar(256)\n"
    		+ ", one_time_filter varchar(256)\n"
    		+ ", operation varchar(256)\n"
    		+ ", original_hash_batches varchar(256)\n"
    		+ ", original_hash_buckets varchar(256)\n"
    		+ ", parallel_aware varchar(256)\n"
    		+ ", parent_relationship varchar(256)\n"
    		+ ", partial_mode varchar(256)\n"
    		+ ", peak_memory_usage varchar(256)\n"
    		+ ", planning_time varchar(256)\n"
    		+ ", plan_rows varchar(256)\n"
    		+ ", plan_width varchar(256)\n"
    		+ ", relation_name varchar(256)\n"
    		+ ", rows_removed_by_join_filter varchar(256)\n"
    		+ ", rows_removed_by_filter varchar(256)\n"
    		+ ", rows_removed_by_index_recheck varchar(256)\n"
    		+ ", repeatable_seed varchar(256)\n"
    		+ ", sampling_method varchar(256)\n"
    		+ ", scan_direction varchar(256)\n"
    		+ ", shared_dirtied_blocks varchar(256)\n"
    		+ ", shared_hit_blocks varchar(256)\n"
    		+ ", shared_read_blocks varchar(256)\n"
    		+ ", shared_written_blocks varchar(256)\n"
    		+ ", sort_method varchar(256)\n"
    		+ ", sort_space_type varchar(256)\n"
    		+ ", sort_space_used varchar(256)\n"
    		+ ", startup_cost varchar(256)\n"
    		+ ", strategy varchar(256)\n"
    		+ ", subplan_name varchar(256)\n"
    		+ ", temp_read_blocks varchar(256)\n"
    		+ ", temp_written_blocks varchar(256)\n"
    		+ ", total_cost varchar(256)\n"
    		+ ", total_runtime varchar(256)\n"
    		+ ", workers_launched varchar(256)\n"
    		+ ", workers_planned varchar(256)\n"

    		// if workers are involved, write how much work the operator (a.k.a the parallel leader) is doing
/*
    		+ ", parallel_leader_local_dirtied_blocks int\n"
    		+ ", parallel_leader_local_hit_blocks int\n"
    		+ ", parallel_leader_local_read_blocks int\n"
    		+ ", parallel_leader_local_written_blocks int\n"
    		+ ", parallel_leader_shared_dirtied_blocks int\n"
    		+ ", parallel_leader_shared_hit_blocks int\n"
    		+ ", parallel_leader_shared_read_blocks int\n"
    		+ ", parallel_leader_shared_written_blocks int\n"
    		+ ", parallel_leader_temp_read_blocks int\n"
    		+ ", parallel_leader_temp_written_blocks int\n"
  */  		
    		// calculated parallel leader values and strings
    		+ ", parallel_leader_title_label_string varchar(256)\n" // "8.3"
    		+ ", parallel_leader_title_label_fontmetrics_width int\n" // "320"
    		+ ", parallel_leader_title_label_fontmetrics_height int\n" // "320"

    		+ ", parallel_leader_difference_time_ms float\n"
    		+ ", parallel_leader_difference_time_percentage_string varchar(256)\n" // "8.3"
    		+ ", parallel_leader_difference_time_units_string varchar(256)\n" // "5.5GB"
    		+ ", parallel_leader_difference_time_label_string varchar(256)\n" // "Disk: 8.3% (5.5GB)"
    		+ ", parallel_leader_difference_time_highlight int\n" // 1 = true
    		+ ", parallel_leader_difference_time_label_fontmetrics_width int\n" // "320"
    		+ ", parallel_leader_difference_time_label_fontmetrics_height int\n" // "320"
/*
    		+ ", parallel_leader_disk_pages int\n"
    		+ ", parallel_leader_disk_percentage_string varchar(256)\n" // "8.3"
    		+ ", parallel_leader_disk_units_string varchar(256)\n" // "5.5GB"
    		+ ", parallel_leader_disk_label_string varchar(256)\n" // "Disk: 8.3% (5.5GB)"
    		+ ", parallel_leader_disk_highlight int\n" // 1 = true
    		+ ", parallel_leader_disk_label_fontmetrics_width int\n" // "320"
    		+ ", parallel_leader_disk_label_fontmetrics_height int\n" // "320"
    		
    		+ ", parallel_leader_disk_plus_ram_pages int\n"
    		+ ", parallel_leader_disk_plus_ram_percentage_string varchar(256)\n" // "8.3"
    		+ ", parallel_leader_disk_plus_ram_units_string varchar(256)\n" // "5.5GB"
    		+ ", parallel_leader_disk_plus_ram_label_string varchar(256)\n" // "Disk: 8.3% (5.5GB)"
    		+ ", parallel_leader_disk_plus_ram_highlight int\n" // 1 = true
    		+ ", parallel_leader_disk_plus_ram_label_fontmetrics_width int\n" // "320"
    		+ ", parallel_leader_disk_plus_ram_label_fontmetrics_height int\n" // "320"
    		
    		+ ", parallel_leader_ram_pages int\n"
    		+ ", parallel_leader_ram_percentage_string varchar(256)\n" // "8.3"
    		+ ", parallel_leader_ram_units_string varchar(256)\n" // "5.5GB"
    		+ ", parallel_leader_ram_label_string varchar(256)\n" // "Disk: 8.3% (5.5GB)"
    		+ ", parallel_leader_ram_highlight int\n" // 1 = true
    		+ ", parallel_leader_ram_label_fontmetrics_width int\n" // "320"
    		+ ", parallel_leader_ram_label_fontmetrics_height int\n" // "320"
*/
    		+ ", parallel_leader_exclusive_time_ms float\n"
    		+ ", parallel_leader_exclusive_time_percentage_string varchar(256)\n" // "8.3"
    		+ ", parallel_leader_exclusive_time_units_string varchar(256)\n" // "5.5GB"
    		+ ", parallel_leader_exclusive_time_label_string varchar(256)\n" // "Disk: 8.3% (5.5GB)"
    		+ ", parallel_leader_exclusive_time_highlight int\n" // 1 = true
    		+ ", parallel_leader_exclusive_time_label_fontmetrics_width int\n" // "320"
    		+ ", parallel_leader_exclusive_time_label_fontmetrics_height int\n" // "320"
    		
    		+ ", parallel_leader_actual_rows int\n" // "1.5MB" // from "plan_rows"
    		+ ", parallel_leader_actual_rows_percentage_string varchar(256)\n" // "8.3"
    		+ ", parallel_leader_actual_rows_units_string varchar(256)\n" // "5.5GB"
    		+ ", parallel_leader_actual_rows_highlight int\n" // 1 = true
    		+ ", parallel_leader_actual_rows_label_string varchar(256)\n" // "13kB"
    		+ ", parallel_leader_actual_rows_label_fontmetrics_width int\n" // "30"
    		+ ", parallel_leader_actual_rows_label_fontmetrics_height int\n" // "30"

    		+ ", parallel_leader_actual_data_size_bytes int\n"
    		+ ", parallel_leader_actual_data_size_percentage_string varchar(256)\n" // "8.3"
    		+ ", parallel_leader_actual_data_size_units_string varchar(256)\n" // "5.5GB"
    		+ ", parallel_leader_actual_data_size_label_string varchar(256)\n" // "Disk: 8.3% (5.5GB)"
    		+ ", parallel_leader_actual_data_size_highlight int\n" // 1 = true
    		+ ", parallel_leader_actual_data_size_label_fontmetrics_width int\n" // "320"
    		+ ", parallel_leader_actual_data_size_label_fontmetrics_height int\n" // "320"
    		
    		// parallel leader highlights
    		//+ ", parallel_leader_cost_highlight int\n" // 1 = true
    		//+ ", parallel_leader_disk_highlight int\n" // 1 = true
    		//+ ", parallel_leader_disk_plus_ram_highlight int\n" // 1 = true
    		//+ ", parallel_leader_exclusive_time_highlight int\n" // 1 = true
    		//+ ", parallel_leader_ram_highlight int\n" // 1 = true
    		
    		// exclusive and difference time are calculated from these
    		+ ", min_actual_startup_time_ms float\n" // min(parallel_leader_actual_startup_time_ms, workers.actual_startup_time_ms)
    		+ ", max_actual_total_time_ms float\n" // max(parallel_leader_actual_total_time_ms, workers.actual_total_time_ms)

    		// calculated values and strings
    		// cost_* - used for north 1 text
    		+ ", cost_percentage_string varchar(256)\n" // "100"
    		+ ", cost_label_string varchar(256)\n" // "Cost: 100%"
    		+ ", cost_highlight_1 int\n" // 1 = true
    		+ ", cost_highlight_2 int\n" // 1 = true
    		+ ", cost_label_fontmetrics_width int\n" // "156"
    		+ ", cost_label_fontmetrics_height int\n" // "156"
/*
			+ ", disk_pages int\n" // 5443453
    		+ ", disk_percentage_string varchar(256)\n" // "8.3"
    		+ ", disk_units_string varchar(256)\n" // "5.5GB"
    		+ ", disk_label_string varchar(256)\n" // "Disk: 8.3% (5.5GB)"
    		+ ", disk_highlight_1 int\n" // 1 = true
    		+ ", disk_highlight_2 int\n" // 1 = true
    		+ ", disk_label_fontmetrics_width int\n" // "320"
    		+ ", disk_label_fontmetrics_height int\n" // "320"
    		
			+ ", disk_plus_ram_pages int\n" // 5443453
    		+ ", disk_plus_ram_units_string varchar(256)\n" // "10GB"
    		+ ", disk_plus_ram_percentage_string varchar(256)\n" // "13"
    		+ ", disk_plus_ram_label_string varchar(256)\n" // "Disk+RAM: 13% (10GB)"
    		+ ", disk_plus_ram_highlight_1 int\n" // 1 = true
    		+ ", disk_plus_ram_highlight_2 int\n" // 1 = true
    		+ ", disk_plus_ram_label_fontmetrics_width int\n" // "320"
    		+ ", disk_plus_ram_label_fontmetrics_height int\n" // "320"

    		+ ", ram_pages int\n" // 5443453
    		+ ", ram_units_string varchar(256)\n" // "1.5MB"
    		+ ", ram_percentage_string varchar(256)\n" // "2.3"
    		+ ", ram_label_string varchar(256)\n" // "RAM: 2.3% (1.5MB)"
    		+ ", ram_highlight_1 int\n" // 1 = true
    		+ ", ram_highlight_2 int\n" // 0 = false
    		+ ", ram_label_fontmetrics_width int\n" // "310"
    		+ ", ram_label_fontmetrics_height int\n" // "310"
*/
    		
    		+ ", parallel_leader_actual_startup_time_ms float\n"
    		+ ", parallel_leader_actual_total_time_ms float\n"
    		
    		// exclusive_time_* - used for north 1 text
    		+ ", exclusive_time_ms float\n" // for the entire operator, calc. from min_actual_startup_time and max_actual_total_time
    		+ ", exclusive_time_units_string varchar(256)\n"
    		+ ", exclusive_time_percentage_string varchar(256)\n"
    		+ ", exclusive_time_label_string varchar(256)\n"
    		+ ", exclusive_time_highlight_1 int\n" // 1 = true
    		+ ", exclusive_time_highlight_2 int\n" // 0 = false
    		+ ", exclusive_time_label_fontmetrics_width int\n"
    		+ ", exclusive_time_label_fontmetrics_height int\n"
    		
    		// difference_time_* - used for north 1 text
    		+ ", difference_time_ms float\n"
    		+ ", difference_time_units_string varchar(256)\n"
    		+ ", difference_time_percentage_string varchar(256)\n"
    		+ ", difference_time_label_string varchar(256)\n"
    		+ ", difference_time_highlight_1 int\n"
    		+ ", difference_time_highlight_2 int\n"
    		+ ", difference_time_label_fontmetrics_width int\n"
    		+ ", difference_time_label_fontmetrics_height int\n"
    		
    		//+ ", parallel_leader_difference_time_highlight_1 int\n"
    		//+ ", parallel_leader_difference_time_highlight_2 int\n"
    		
    		+ ", actual_data_size_bytes int\n" // "13000"
    		+ ", actual_data_size_units_string varchar(256)\n" // "1.5MB"
    		+ ", actual_data_size_label_string varchar(256)\n" // "13kB"
    		+ ", actual_data_size_label_fontmetrics_width int\n" // "30"
    		+ ", actual_data_size_label_fontmetrics_height int\n" // "30"
    		
    		+ ", estimated_data_size_bytes int\n" // "13000"
    		+ ", estimated_data_size_units_string varchar(256)\n" // "1.5MB"
    		+ ", estimated_data_size_label_string varchar(256)\n" // "13kB"
    		+ ", estimated_data_size_label_fontmetrics_width int\n" // "30"
    		+ ", estimated_data_size_label_fontmetrics_height int\n" // "30"

    		+ ", actual_rows_units_string varchar(256)\n" // "1.5M" // from "plan_rows"
    		+ ", actual_rows_label_string varchar(256)\n" // "13k"
    		+ ", actual_rows_label_fontmetrics_width int\n" // "30"
    		+ ", actual_rows_label_fontmetrics_height int\n" // "30"

    		+ ", estimated_rows_units_string varchar(256)\n" // "1.5M" // from "plan_rows"
    		+ ", estimated_rows_label_string varchar(256)\n" // "13k"
    		+ ", estimated_rows_label_fontmetrics_width int\n" // "30"
    		+ ", estimated_rows_label_fontmetrics_height int\n" // "30"

			+ ", ancestor_workers_planned int\n"
			+ ", ancestor_workers_launched int\n"
			+ ", exact_actual_worker_count int\n" // number of workers with actuals (not sorts) used by this operator; null if no workers are found anywhere; otherwise will be >=0
			+ ", exact_sort_worker_count int\n" // number of sort workers used by this operator; null if no workers are found anywhere; otherwise will be >=0
			+ ", sum_workers_actual_rows int\n"
			
    		+ ", plan_rows_times_ancestor_workers_planned int\n" // estimated rows * (leader + # of workers)
    		+ ", plan_rows_times_ancestor_workers_launched int\n" // estimated rows * (leader + # of workers)
    		
    		// set total_estimated_rows to either estimated_rows_times_ancestor_workers_launched or estimated_rows_plus_current_worker_actual_rows, whichever is more accurate
			+ ", total_actual_rows int\n" // "1.5M" from actual_rows plus workers (from ancestor gather or workers, if any) 
			+ ", total_estimated_rows int\n" // "1.5M" from estimated_rows plus workers (from ancestor gather or workers, if any)
    		
			+ ", actual_rows_times_ancestor_workers_planned int\n" // "1.5M" from actual_rows * ancestor workers launched
			+ ", actual_rows_times_ancestor_workers_launched int\n" // "1.5M" from actual_rows * ancestor workers launched
			+ ", actual_rows_plus_sum_workers_actual_rows int\n" // "1.5M" from actual_rows + sum of all worker plan_rows (if any)
			+ ", total_actual_rows_is_exact int\n" // flag - actual_rows and actual_rows_plus_sum_workers_actual_rows are exact, all others are not
			+ ", total_actual_mass_is_exact int\n" // flag - actual_mass and actual_mass_plus_sum_workers_actual_mass are exact, all others are not
			
			+ ", actual_mass_times_sum_workers_actual_rows int\n" // "1.5M" from plan_width * actual_rows * ancestor workers launched
			+ ", actual_mass_times_ancestor_workers_planned int\n" // "1.5M" from plan_width * actual_rows * ancestor workers launched
			+ ", actual_mass_times_ancestor_workers_launched int\n" // "1.5M" from plan_width * actual_rows * ancestor workers launched
			+ ", estimated_mass_times_ancestor_workers_planned int\n" // "1.5M" from plan_width * actual_rows * ancestor workers launched
			+ ", estimated_mass_times_ancestor_workers_launched int\n" // "1.5M" from plan_width * actual_rows * ancestor workers launched

			+ ", rows_removed_by_filter_times_ancestor_workers_planned int\n"
			+ ", rows_removed_by_filter_times_ancestor_workers_launched int\n"
			+ ", rows_removed_by_filter_times_exact_actual_worker_count int\n" // NEW
			+ ", total_rows_removed_by_filter int\n"
			+ ", total_rows_removed_by_filter_is_exact boolean\n"
			
			+ ", rows_removed_by_join_filter_times_ancestor_workers_planned int\n"
			+ ", rows_removed_by_join_filter_times_ancestor_workers_launched int\n"
			+ ", rows_removed_by_join_filter_times_exact_actual_worker_count int\n" // NEW
			+ ", total_rows_removed_by_join_filter int\n"
			+ ", total_rows_removed_by_join_filter_is_exact boolean\n"
			
			+ ", rows_removed_by_index_recheck_times_ancestor_workers_planned int\n"
			+ ", rows_removed_by_index_recheck_times_ancestor_workers_launched int\n"
			+ ", rows_removed_by_index_recheck_times_exact_actual_worker_count int\n" // NEW
			+ ", total_rows_removed_by_index_recheck int\n"
			+ ", total_rows_removed_by_index_recheck_is_exact boolean\n"
			
    		// line thicknesses
    		//+ ", actual_rows_line_thickness int\n" // "5"
    		+ ", actual_rows_line_thickness int\n" // "5"
    		//+ ", estimated_rows_line_thickness int\n" // "5"
    		+ ", estimated_rows_line_thickness int\n" // "5"
    		+ ", actual_data_size_line_thickness int\n" // "5"
    		+ ", estimated_data_size_line_thickness int\n" // "5"
    		//+ ", actual_vs_estimated_ratio_line_thickness int\n" // "5"
    		+ ", actual_vs_estimated_rows_ratio_line_thickness int\n"
    		+ ", actual_vs_estimated_mass_ratio_line_thickness int\n"
    		
    		// north, highlight, arrow, and south labels 
    		+ ", north_1_text varchar(256)\n" // "Disk+RAM: 13% (10GB)"
    		+ ", north_1_fontmetrics_width int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", north_1_fontmetrics_height int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", north_1_highlight_1 int\n" // true
    		+ ", north_1_highlight_2 int\n" // false
    		+ ", arrow_text varchar(256)\n" // "5MB"
    		+ ", arrow_text_fontmetrics_width int\n" // "5MB"
    		+ ", arrow_text_fontmetrics_height int\n" // "5MB"
    		+ ", arrow_line_thickness int\n" // "8"
    		+ ", south_1_text varchar(256)\n" // "Nested Loop"
    		+ ", south_1_fontmetrics_width int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", south_1_fontmetrics_height int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", south_2_text varchar(256)\n" // "Inner Join"
    		+ ", south_2_fontmetrics_width int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", south_2_fontmetrics_height int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", south_3_text varchar(256)\n"
    		+ ", south_3_fontmetrics_width int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", south_3_fontmetrics_height int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", south_4_text varchar(256)\n"
    		+ ", south_4_fontmetrics_width int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", south_4_fontmetrics_height int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", south_5_text varchar(256)\n"
    		+ ", south_5_fontmetrics_width int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", south_5_fontmetrics_height int\n" // "Disk+RAM: 13% (10GB)"
    		+ ", operator_width int\n"
    		+ ", operator_height int\n"

    		+ ", box_width int\n" // boundary width for box, includes worker text, excludes box margins
    		+ ", box_height int\n" // boundary height for box, excludes box margins
			+ ", workers_fontmetrics_width int\n"
			+ ", workers_fontmetrics_height int\n"
			
			// peg values
			+ ", pg_version_number float\n"
			+ ", pg_version_environment varchar(250)\n"
			
			// add pk - must be done at table creation 
			+ ", constraint pk_epoa primary key (id_explain_plan, id_explain_plan_property)\n"
    		
    		+ ");";
		strSQL += "create index if not exists idx_epoa_test_1 on explain_plan_operator_annotations (id_explain_plan, parent_id_explain_plan_property);\n";
		strSQL += "create index if not exists idx_epoa_test_2 on explain_plan_operator_annotations (id_explain_plan);\n";
		strSQL += "create index if not exists idx_epoa_test_3 on explain_plan_operator_annotations (id_explain_plan, id_explain_plan_property);\n";
		strSQL += "create index if not exists idx_epoa_test_4 on explain_plan_operator_annotations (id_explain_plan, node_type, join_type, operation, strategy, command);\n";

        Database.dml(strSQL);

/*
update explain_plan_operator_annotations set
arrow_text_fontmetrics_width = 10;
select arrow_text_fontmetrics_width, south_1_fontmetrics_width, column_right_operation, left_box
from explain_plan_operator_annotations;
update explain_plan_operator_annotations set
south_1_fontmetrics_width = 20;
update explain_plan_operator_annotations set
south_1_fontmetrics_width = null;
*/

		
		// if major or minor db version number change, drop / recreate table
		if (intDBConfigVersionMajor != intDBSchemaVersionNumberMajor || strDBConfigVersionMinor.equals(strDBSchemaVersionNumberMinor) == false) {
			strSQL = "drop table if exists explain_plan_operator_workers;";
			Database.dml(strSQL);
		}
		
        strSQL = "create table if not exists explain_plan_operator_workers (\n"
    		+ "id_explain_plan int not null\n"
    		+ ", id_explain_plan_property int not null\n"
    		+ ", worker_number int not null\n"

    		+ ", actual_loops int\n"
    		+ ", actual_rows int\n"
    		+ ", actual_startup_time_ms float\n"
    		+ ", actual_total_time_ms float\n"
    		+ ", local_dirtied_blocks int\n"
    		+ ", local_hit_blocks int\n"
    		+ ", local_read_blocks int\n"
    		+ ", local_written_blocks int\n"
    		//+ ", plan_width varchar(256)\n" // workers do not have plan width
    		+ ", shared_dirtied_blocks int\n"
    		+ ", shared_hit_blocks int\n"
    		+ ", shared_read_blocks int\n"
    		+ ", shared_written_blocks int\n"
    		+ ", sort_method varchar(50)\n"
    		+ ", sort_space_type varchar(50)\n"
    		+ ", sort_space_used_kb int\n"
    		+ ", temp_read_blocks int\n"
    		+ ", temp_written_blocks int\n"

    		+ ", actual_data_size_bytes int\n"
    		+ ", difference_time_ms float\n"
    		+ ", disk_pages int\n"
    		+ ", disk_plus_ram_pages int\n"
    		+ ", exclusive_time_ms float\n"
    		+ ", ram_pages int\n"
    		
    		+ ", actual_data_size_units_string varchar(256)\n"
    		+ ", actual_rows_units_string varchar(256)\n"
    		+ ", difference_time_units_string varchar(256)\n"
    		+ ", disk_units_string varchar(256)\n"
    		+ ", disk_plus_ram_units_string varchar(256)\n"
    		+ ", exclusive_time_units_string varchar(256)\n"
    		+ ", ram_units_string varchar(256)\n"
    		    		
    		+ ", actual_data_size_percentage_string varchar(256)\n"
    		+ ", actual_rows_percentage_string varchar(256)\n"
    		+ ", difference_time_percentage_string varchar(256)\n"
    		+ ", disk_percentage_string varchar(256)\n"
    		+ ", disk_plus_ram_percentage_string varchar(256)\n"
    		+ ", exclusive_time_percentage_string varchar(256)\n"
    		+ ", ram_percentage_string varchar(256)\n"

			+ ", worker_title_label_string varchar(256)\n"
			+ ", actual_data_size_label_string varchar(256)\n"
			+ ", actual_rows_label_string varchar(256)\n"
    		+ ", difference_time_label_string varchar(256)\n"
    		+ ", disk_label_string varchar(256)\n"
    		+ ", disk_plus_ram_label_string varchar(256)\n"
    		+ ", exclusive_time_label_string varchar(256)\n"
    		+ ", ram_label_string varchar(256)\n"

    		+ ", actual_data_size_label_fontmetrics_width int\n"
    		+ ", actual_data_size_label_fontmetrics_height int\n"
			+ ", actual_rows_label_fontmetrics_width int\n"
			+ ", actual_rows_label_fontmetrics_height int\n"
			+ ", difference_time_label_fontmetrics_width int\n"
			+ ", difference_time_label_fontmetrics_height int\n"
			+ ", disk_label_fontmetrics_width int\n"
			+ ", disk_label_fontmetrics_height int\n"
			+ ", disk_plus_ram_label_fontmetrics_width int\n"
			+ ", disk_plus_ram_label_fontmetrics_height int\n"
			+ ", exclusive_time_label_fontmetrics_width int\n"
			+ ", exclusive_time_label_fontmetrics_height int\n"
			+ ", ram_label_fontmetrics_width int\n"
			+ ", ram_label_fontmetrics_height int\n"
    		+ ", worker_title_label_fontmetrics_width int\n"
    		+ ", worker_title_label_fontmetrics_height int\n"
    		
    		+ ", actual_data_size_highlight_1 int\n"
    		+ ", actual_rows_highlight_1 int\n"
    		+ ", difference_time_highlight_1 int\n"
    		+ ", disk_highlight_1 int\n"
    		+ ", disk_plus_ram_highlight_1 int\n"
    		+ ", ram_highlight_1 int\n"
    		+ ", exclusive_time_highlight_1 int\n"

    		// add pk - must be done at table creation
			+ ", constraint pk_epow primary key (worker_number, id_explain_plan, id_explain_plan_property)\n"
    		+ ");";

        Database.dml(strSQL);
		
		// if major or minor db version number change, drop / recreate table
		if (intDBConfigVersionMajor != intDBSchemaVersionNumberMajor || strDBConfigVersionMinor.equals(strDBSchemaVersionNumberMinor) == false) {
			strSQL = "drop table if exists explain_plan_operator_show_workers;";
			Database.dml(strSQL);
		}
		
        strSQL = "create table if not exists explain_plan_operator_show_workers (\n"
    		+ "id_explain_plan int not null\n"
    		+ ", id_explain_plan_property int not null\n"
    		+ ");";
        Database.dml(strSQL);
		
		// if major or minor db version number change, drop / recreate table
		if (intDBConfigVersionMajor != intDBSchemaVersionNumberMajor || strDBConfigVersionMinor.equals(strDBSchemaVersionNumberMinor) == false) {
			strSQL = "drop table if exists explain_plan_operator_outputs;";
			Database.dml(strSQL);
		}
		
        strSQL = "create table if not exists explain_plan_operator_outputs (\n"
    		+ "id_explain_plan int not null\n"
    		+ ", id_explain_plan_property int not null\n"
    		+ ", column_order int not null\n"
    		+ ", column_name varchar(256) not null\n"
    		+ ");";
        Database.dml(strSQL);
		
		// Nested folders for the Plans tree
		strSQL = "create table if not exists plan_folders (\n"
				+ "id_folder integer primary key\n"
				+ ", folder_name varchar(256) not null\n"
				+ ", parent_id_folder integer\n"
				+ ", sort_order integer not null default 0\n"
				+ ");\n";
		Database.dml(strSQL);
		Database.dml("create unique index if not exists idx_plan_folders_parent_name\n"
				+ "on plan_folders (ifnull(parent_id_folder, -1), folder_name);");

        // create table for the list of plans
        // updating major db schema version drops / recreates this table
        strSQL = "create table if not exists explain_plans (";
        strSQL += "id_explain_plan int unique not null\n";
        strSQL += ", plan_name varchar(256) not null\n";
        strSQL += ", folder_id integer\n";
        strSQL += ", plan_text varchar(8192)\n";
        strSQL += ", server varchar(256)\n";
        strSQL += ", port integer\n";
        strSQL += ", database varchar(256)\n";
        strSQL += ", username varchar(256)\n";
        strSQL += ", password varchar(256)\n";
        strSQL += ", pre_explain_command varchar(256)\n";
        strSQL += ", explain_sql_command varchar(8192)\n";
        strSQL += ", post_explain_command varchar(256)\n";
        strSQL += ", remember_password int not null default 0";
        strSQL += ", auth_type varchar(64) not null default 'password'";
        strSQL += ", write_version_environment_to_json int not null default 1";
        strSQL += ", write_query_to_json int not null default 1";
        strSQL += ", write_pg_environment_settings_to_json int not null default 1";
        strSQL += ");";
        Database.dml(strSQL);
		ensureExplainPlansColumn("auth_type", "varchar(64) not null default 'password'");
		ensureExplainPlansColumn("folder_id", "integer");
		migrateExplainPlansUniqueToFolderScope(dblDBConfigVersion);
		// After any table rebuild so sort_order is not dropped by the 4.11 migration.
		ensureExplainPlansColumn("sort_order", "integer not null default 0");
		ensureExplainPlansColumn("anonymize", "integer not null default 0");
		ensureExplainPlansColumn("anonymize_aliases", "integer not null default 1");
		backfillPlanTreeSortOrders(dblDBConfigVersion);
		Database.dml("create unique index if not exists idx_explain_plans_folder_name\n"
				+ "on explain_plans (ifnull(folder_id, -1), plan_name);");

		// if major or minor db version number change, drop / recreate table
		if (intDBConfigVersionMajor != intDBSchemaVersionNumberMajor || strDBConfigVersionMinor.equals(strDBSchemaVersionNumberMinor) == false) {
			strSQL = "drop table if exists explain_plan_generated_values;";
			Database.dml(strSQL);
		}
		
        // create table for the list of calculated values for explain plans
        // 1:1 with explain_plan so we can wipe out plans less often
        strSQL = "create table if not exists explain_plan_generated_values (";
        strSQL += "id_explain_plan int unique not null\n";
        strSQL += ", is_active int not null\n";
        
        // can't think of any reason not to write the pg version number
        
        strSQL += ", pg_version_number float\n";
        strSQL += ", pg_version_environment varchar(250)\n";

        strSQL += ", delta_costs_grand_total float";
        strSQL += ", sum_total_costs float";
        strSQL += ", max_total_cost float";
        strSQL += ", execution_time_ms float";
        strSQL += ", planning_time_ms float";
        strSQL += ", total_disk_blocks int";
        strSQL += ", total_disk_plus_ram_blocks int";
        strSQL += ", total_ram_blocks int";
        strSQL += ", total_runtime_ms float\n"; // for pg 9.3

        // remember which buttons are enabled for this plan
        strSQL += ", actual_mass_enabled int not null default 0";
        strSQL += ", estimated_mass_enabled int not null default 0";
        strSQL += ", actual_rows_enabled int not null default 0";
        strSQL += ", estimated_rows_enabled int not null default 0";
        strSQL += ", actual_vs_estimated_rows_ratio_enabled int not null default 0";
        strSQL += ", actual_vs_estimated_mass_ratio_enabled int not null default 0";
        strSQL += ", cost_enabled int not null default 0";
        strSQL += ", disk_enabled int not null default 0";
        strSQL += ", disk_plus_ram_enabled int not null default 0";
        strSQL += ", ram_enabled int not null default 0";
        strSQL += ", exclusive_time_enabled int not null default 0";
        strSQL += ", time_difference_enabled int not null default 0";
        
        // if _any_ workers are found, set verbose to 1
        strSQL += ", verbose_setting int not null default 0";

        strSQL += ", max_1_operator_cost float";
        strSQL += ", max_2_operator_cost float";
        strSQL += ", max_1_operator_time float";
        strSQL += ", max_2_operator_time float";
        strSQL += ", max_1_operator_ram float";
        strSQL += ", max_2_operator_ram float";
        //strSQL += ", zoom float not null\n";
        //strSQL += ", operator_box_width_bz int not null\n";
        //strSQL += ", operator_box_width int\n";
        //strSQL += ", operator_box_thickness int not null\n"; // not affected by zoom
        strSQL += ", show_worker_info int not null default 1\n";
        strSQL += ", show_worker_actual_data_size int not null default 1\n";
        strSQL += ", show_worker_actual_rows int not null default 1\n";
        strSQL += ", show_worker_difference_time int not null default 1\n";
        strSQL += ", show_worker_disk int not null default 0\n";
        strSQL += ", show_worker_disk_plus_ram int not null default 0\n";
        strSQL += ", show_worker_exclusive_time int not null default 1\n";
        strSQL += ", show_worker_ram int not null default 0\n";

        // pg environment settings
        strSQL += ");";
        Database.dml(strSQL);

		// if major or minor db version number change, drop / recreate table
		if (intDBConfigVersionMajor != intDBSchemaVersionNumberMajor || strDBConfigVersionMinor.equals(strDBSchemaVersionNumberMinor) == false) {
			strSQL = "drop table if exists explain_plan_properties;";
			Database.dml(strSQL);
		}
		
        strSQL = "create table if not exists explain_plan_properties (";
        strSQL += "parent_id_explain_plan_property int not null";
        strSQL += ", id_explain_plan int not null";
        strSQL += ", id_explain_plan_property int not null";
        strSQL += ", operator_name varchar(100) not null, operator_value varchar(100)";

        // add pk - must be done at table creation 
        strSQL += ", constraint pk_epp primary key (id_explain_plan, parent_id_explain_plan_property, id_explain_plan_property)\n";
        
		strSQL += ");\n";
		strSQL += "create index if not exists idx_epp_test_1 on explain_plan_properties (id_explain_plan, parent_id_explain_plan_property);\n";
		strSQL += "create index if not exists idx_epp_test_2 on explain_plan_properties (id_explain_plan);\n";
        Database.dml(strSQL);
		
		if (bolShowTiming == true) { System.out.println("after Database.init(): " + new SimpleDateFormat("kk:mm:ss.S").format(new Date())); }
	}
	

    public static void dml(String strSQL) {
        Statement objStmt = null;
        if (objConnection == null) {
        	return;
        }
        
        try {
            objStmt = objConnection.createStatement();
            objStmt.setQueryTimeout(30);
            objStmt.executeUpdate(strSQL);
        } catch (SQLException ex) {
            // Callers often embed "begin transaction; ... commit;". If that batch fails
            // after BEGIN, the connection stays in a transaction and later PRAGMA/DML
            // blows up with "Safety level may not be changed inside a transaction".
            try {
            	objConnection.createStatement().executeUpdate("rollback");
            } catch (SQLException ignore) {
            }
            try {
            	objConnection.setAutoCommit(true);
            } catch (SQLException ignore) {
            }
            System.out.println("function Database.dml() 1");
            System.out.println("strSQL: " + truncateForLog(strSQL));
            ex.printStackTrace();
        }
    }

	/**
	 * Bind large plan fields so JSON is not embedded in the SQL text (avoids SQLITE_TOOBIG).
	 * If {@code planName} already exists on another row, {@code conflictPlanName} is stored instead.
	 */
	public static void updateExplainPlan(int idExplainPlan, String planName, String conflictPlanName,
			String server, String port, String database, String username, String password, boolean rememberPassword,
			String authType, String preExplainCommand, String explainSqlCommand, String postExplainCommand, String planText) {
		if (objConnection == null) {
			return;
		}
		String sql = "update explain_plans set "
				+ "plan_name = case when exists(select 1 from explain_plans where plan_name = ? and id_explain_plan <> ?) then ? else ? end, "
				+ "server = ?, "
				+ "port = ?, "
				+ "database = ?, "
				+ "username = ?, "
				+ "password = ?, "
				+ "remember_password = ?, "
				+ "auth_type = ?, "
				+ "pre_explain_command = ?, "
				+ "explain_sql_command = ?, "
				+ "post_explain_command = ?, "
				+ "plan_text = ? "
				+ "where id_explain_plan = ?";
		try (PreparedStatement ps = objConnection.prepareStatement(sql)) {
			int i = 1;
			ps.setString(i++, nullToEmpty(planName));
			ps.setInt(i++, idExplainPlan);
			ps.setString(i++, nullToEmpty(conflictPlanName));
			ps.setString(i++, nullToEmpty(planName));
			ps.setString(i++, nullToEmpty(server));
			if (port == null || port.isBlank()) {
				ps.setNull(i++, Types.INTEGER);
			} else {
				ps.setInt(i++, Integer.parseInt(port.trim()));
			}
			ps.setString(i++, nullToEmpty(database));
			ps.setString(i++, nullToEmpty(username));
			ps.setString(i++, rememberPassword ? nullToEmpty(password) : "");
			ps.setInt(i++, rememberPassword ? 1 : 0);
			ps.setString(i++, normalizeAuthType(authType));
			ps.setString(i++, nullToEmpty(preExplainCommand));
			ps.setString(i++, nullToEmpty(explainSqlCommand));
			ps.setString(i++, nullToEmpty(postExplainCommand));
			ps.setString(i++, nullToEmpty(planText));
			ps.setInt(i++, idExplainPlan);
			ps.executeUpdate();
		} catch (SQLException | NumberFormatException ex) {
			System.out.println("function Database.updateExplainPlan()");
			ex.printStackTrace();
		}
	}

	public static void updateExplainPlanText(int idExplainPlan, String planText) {
		if (objConnection == null) {
			return;
		}
		String sql = "update explain_plans set plan_text = ? where id_explain_plan = ?";
		try (PreparedStatement ps = objConnection.prepareStatement(sql)) {
			ps.setString(1, nullToEmpty(planText));
			ps.setInt(2, idExplainPlan);
			ps.executeUpdate();
		} catch (SQLException ex) {
			System.out.println("function Database.updateExplainPlanText()");
			ex.printStackTrace();
		}
	}

	/** Persist per-plan Anonymize / Anonymize aliases checkboxes (not scrubbed text). */
	public static void updateExplainPlanAnonymizeFlags(int idExplainPlan, boolean anonymize,
			boolean anonymizeAliases) {
		if (objConnection == null) {
			return;
		}
		String sql = "update explain_plans set anonymize = ?, anonymize_aliases = ? where id_explain_plan = ?";
		try (PreparedStatement ps = objConnection.prepareStatement(sql)) {
			ps.setInt(1, anonymize ? 1 : 0);
			ps.setInt(2, anonymizeAliases ? 1 : 0);
			ps.setInt(3, idExplainPlan);
			ps.executeUpdate();
		} catch (SQLException ex) {
			System.out.println("function Database.updateExplainPlanAnonymizeFlags()");
			ex.printStackTrace();
		}
	}

	public static void updateExplainPlanFields(int idExplainPlan, String planName, String server, String port,
			String database, String username, String password, boolean rememberPassword, String authType,
			String preExplainCommand, String explainSqlCommand, String postExplainCommand, String planText) {
		if (objConnection == null) {
			return;
		}
		String sql = "update explain_plans set "
				+ "server = ?, port = ?, database = ?, username = ?, password = ?, remember_password = ?, "
				+ "auth_type = ?, "
				+ "pre_explain_command = ?, explain_sql_command = ?, post_explain_command = ?, "
				+ "plan_text = ?, plan_name = ? where id_explain_plan = ?";
		try (PreparedStatement ps = objConnection.prepareStatement(sql)) {
			int i = 1;
			ps.setString(i++, nullToEmpty(server));
			if (port == null || port.isBlank()) {
				ps.setNull(i++, Types.INTEGER);
			} else {
				ps.setInt(i++, Integer.parseInt(port.trim()));
			}
			ps.setString(i++, nullToEmpty(database));
			ps.setString(i++, nullToEmpty(username));
			ps.setString(i++, rememberPassword ? nullToEmpty(password) : "");
			ps.setInt(i++, rememberPassword ? 1 : 0);
			ps.setString(i++, normalizeAuthType(authType));
			ps.setString(i++, nullToEmpty(preExplainCommand));
			ps.setString(i++, nullToEmpty(explainSqlCommand));
			ps.setString(i++, nullToEmpty(postExplainCommand));
			ps.setString(i++, nullToEmpty(planText));
			ps.setString(i++, nullToEmpty(planName));
			ps.setInt(i++, idExplainPlan);
			ps.executeUpdate();
		} catch (SQLException | NumberFormatException ex) {
			System.out.println("function Database.updateExplainPlanFields()");
			ex.printStackTrace();
		}
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private static String normalizeAuthType(String authType) {
		if (authType != null && authType.equalsIgnoreCase("trust")) {
			return "trust";
		}
		return "password";
	}

	private static String truncateForLog(String sql) {
		if (sql == null) {
			return "null";
		}
		final int max = 2000;
		if (sql.length() <= max) {
			return sql;
		}
		return sql.substring(0, max) + "\n... [" + sql.length() + " chars total, truncated for log]";
	}
    
    public static JsonArray rows(String strSQL) {
        Statement objS = null;
        ResultSet objRS = null;
        ResultSetMetaData objRSMD = null;
        
        JsonArray json = new JsonArray();
        int numColumns = 0;

        if (objConnection == null) {
            return (JsonArray) objConnection;
        }

        try {
            objS = objConnection.createStatement();
        } catch (SQLException ex) {
        	System.out.println("function Database.rows() 1");
            ex.printStackTrace();
        }
        
        try {
            objRS = objS.executeQuery(strSQL);
        } catch (SQLException ex) {
        	System.out.println("function Database.rows() 2");
            System.out.println("strSQL: " + strSQL);
            ex.printStackTrace();
        }

        try {
            objRSMD = objRS.getMetaData();
        } catch (SQLException ex) {
        	System.out.println("function Database.rows() 3");
            ex.printStackTrace();
        }

        try {
            numColumns = objRSMD.getColumnCount();
        } catch (SQLException ex) {
        	System.out.println("function Database.rows() 4");
            ex.printStackTrace();
        }

        try {
			while (objRS.next()) {
			  JsonObject obj = new JsonObject();
			  for (int i=1; i <= numColumns; i++) {
			    //String column_name = objRSMD.getColumnName(i);
			    String column_name = "";
			    try {
			        column_name = objRSMD.getColumnName(i);
			    } catch (SQLException ex) {
			    	System.out.println("function Database.rows() 10");
			        //Logger.getLogger(PEG.class.getName()).log(Level.SEVERE, null, ex);
			    }
			    try {
			        //obj.put(column_name, rs.getObject(column_name));
			        obj.put(column_name, objRS.getObject(column_name));
			        //obj.put(column_name, i)
			    } catch (SQLException ex) {
			    	System.out.println("function Database.rows() 6");
			        //Logger.getLogger(PEG.class.getName()).log(Level.SEVERE, null, ex);
			    }
			  }
			  //json.put(obj);
			  json.add(obj);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

        if (objRS != null) {
            try {
                objRS.close();
            } catch (SQLException ex) {
            	System.out.println("function Database.rows() 7");
                ex.printStackTrace();
            }
        }
        return json;
    }
    
    public static JsonObject row(String strSQL) {
    //public static JsonKey row(String strSQL) {
        Statement objS = null;
        ResultSet objRS = null;
        ResultSetMetaData objRSMD = null;
        JsonObject jsnRow = new JsonObject();
        //JsonKey jsnRow;
        int numColumns = 0;

        if (objConnection == null) {
            return (JsonObject) objConnection;
        	//return (JsonKey) objConnection;
        }

        try {
            objS = objConnection.createStatement();
        } catch (SQLException ex) {
        	System.out.println("function Database.row() 1");
            ex.printStackTrace();
        }
        try {
            objRS = objS.executeQuery(strSQL);
        } catch (SQLException e) {
        	System.out.println("function Database.row() 2");
            e.printStackTrace();
        }

        try {
            objRSMD = objRS.getMetaData();
        } catch (SQLException ex) {
        	System.out.println("function Database.row() 3");
            ex.printStackTrace();
        }
        try {
            numColumns = objRSMD.getColumnCount();
        } catch (SQLException ex) {
        	System.out.println("function Database.row() 4");
            ex.printStackTrace();
        }
        //while (objRS.next()) {
        //while(objRSMD.next()) {
        try {
			if (objRS.next()) {
			  //JsonObject obj = new JsonObject();
			  for (int i = 1; i <= numColumns; i++) {
			    //String column_name = objRSMD.getColumnName(i);
			    String column_name = "";
			    try {
			        column_name = objRSMD.getColumnName(i);
			    } catch (SQLException ex) {
			    	System.out.println("function Database.row() 11");
			        ex.printStackTrace();
			    }
			    try {
			        jsnRow.put(column_name, objRS.getObject(column_name));
			    } catch (Exception e) {
			    	System.out.println("function Database.row() 6");
			        e.printStackTrace();
			    }
			  }
			  //json.put(obj);
			  //json.add(obj);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

        if (objRS != null) {
            try {
                objRS.close();
            } catch (SQLException ex) {
            	System.out.println("function Database.row() 7");
                ex.printStackTrace();
            }
        }
        return jsnRow;
    }
 
    public static String explainPlanOperatorAnnotationsIndexes() {
    	String strSQL = "";
    	return strSQL;
    }
    
    public static String explainPlanOperatorWorkersIndexes() {
    	String strSQL = "";
    	return strSQL;
    }

	/** Persist Docking Frames layout XML (nullable = use default layout). */
	public static void saveDockingLayout(String layoutXml) {
		if (objConnection == null) {
			return;
		}
		String sql = "update config set docking_layout = ?";
		try (PreparedStatement ps = objConnection.prepareStatement(sql)) {
			if (layoutXml == null || layoutXml.isBlank()) {
				ps.setNull(1, Types.VARCHAR);
			} else {
				ps.setString(1, layoutXml);
			}
			ps.executeUpdate();
		} catch (SQLException ex) {
			System.out.println("function Database.saveDockingLayout()");
			ex.printStackTrace();
		}
	}

	/** Load Docking Frames layout XML, or null if missing. */
	public static String loadDockingLayout() {
		JsonObject row = row("select docking_layout from config;");
		if (row == null || row.get("docking_layout") == null) {
			return null;
		}
		String value = String.valueOf(row.get("docking_layout"));
		if (value.isBlank() || "null".equalsIgnoreCase(value)) {
			return null;
		}
		return value;
	}

	/** Remember which Plans-list entry was highlighted (restored on next launch). */
	public static void saveSelectedPlanName(String planName) {
		if (objConnection == null) {
			return;
		}
		String sql = "update config set selected_plan_name = ?";
		try (PreparedStatement ps = objConnection.prepareStatement(sql)) {
			if (planName == null || planName.isBlank()) {
				ps.setNull(1, Types.VARCHAR);
			} else {
				ps.setString(1, planName);
			}
			ps.executeUpdate();
		} catch (SQLException ex) {
			System.out.println("function Database.saveSelectedPlanName()");
			ex.printStackTrace();
		}
	}

	/** Last highlighted plan name, or null if none / missing. */
	public static String loadSelectedPlanName() {
		JsonObject row = row("select selected_plan_name from config;");
		if (row == null || row.get("selected_plan_name") == null) {
			return null;
		}
		String value = String.valueOf(row.get("selected_plan_name"));
		if (value.isBlank() || "null".equalsIgnoreCase(value)) {
			return null;
		}
		return value;
	}

	/** Remember which plan id was highlighted (preferred over name once folders exist). */
	public static void saveSelectedPlanId(Integer planId) {
		if (objConnection == null) {
			return;
		}
		String sql = "update config set selected_plan_id = ?";
		try (PreparedStatement ps = objConnection.prepareStatement(sql)) {
			if (planId == null) {
				ps.setNull(1, Types.INTEGER);
			} else {
				ps.setInt(1, planId.intValue());
			}
			ps.executeUpdate();
		} catch (SQLException ex) {
			System.out.println("function Database.saveSelectedPlanId()");
			ex.printStackTrace();
		}
	}

	/** Last highlighted plan id, or null if none / missing. */
	public static Integer loadSelectedPlanId() {
		JsonObject row = row("select selected_plan_id from config;");
		if (row == null || row.get("selected_plan_id") == null) {
			return null;
		}
		Object value = row.get("selected_plan_id");
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		String text = String.valueOf(value);
		if (text.isBlank() || "null".equalsIgnoreCase(text)) {
			return null;
		}
		try {
			return Integer.valueOf(text);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	/** Plans tree order mode: {@code by_name} or {@code manual}. */
	public static String loadPlansSortMode() {
		return loadConfigString("plans_sort_mode", "by_name");
	}

	/** Plans tree name order: {@code asc} or {@code desc}. */
	public static String loadPlansSortNameOrder() {
		return loadConfigString("plans_sort_name_order", "asc");
	}

	/** Plans tree grouping: {@code folders_first}, {@code plans_first}, or {@code mixed}. */
	public static String loadPlansSortGrouping() {
		return loadConfigString("plans_sort_grouping", "folders_first");
	}

	public static void savePlansSortPreferences(String mode, String nameOrder, String grouping) {
		if (objConnection == null) {
			return;
		}
		String sql = "update config set plans_sort_mode = ?, plans_sort_name_order = ?, plans_sort_grouping = ?";
		try (PreparedStatement ps = objConnection.prepareStatement(sql)) {
			ps.setString(1, mode == null || mode.isBlank() ? "by_name" : mode);
			ps.setString(2, nameOrder == null || nameOrder.isBlank() ? "asc" : nameOrder);
			ps.setString(3, grouping == null || grouping.isBlank() ? "folders_first" : grouping);
			ps.executeUpdate();
		} catch (SQLException ex) {
			System.out.println("function Database.savePlansSortPreferences()");
			ex.printStackTrace();
		}
	}

	/**
	 * One-time (schema &lt; 4.12): assign sort_order by name within each parent so Manual
	 * mode has a stable starting order.
	 */
	private static void backfillPlanTreeSortOrders(double dblDBConfigVersion) {
		if (dblDBConfigVersion >= 4.12) {
			return;
		}
		JsonArray folderParents = rows(
				"select distinct ifnull(parent_id_folder, -1) as parent_key from plan_folders;");
		if (folderParents != null) {
			for (int i = 0; i < folderParents.size(); i++) {
				JsonObject row = (JsonObject) folderParents.get(i);
				Integer parentKey = coerceInteger(row.get("parent_key"));
				if (parentKey == null) {
					continue;
				}
				String scope = parentKey.intValue() < 0
						? "parent_id_folder is null"
						: "parent_id_folder = " + parentKey.intValue();
				JsonArray kids = rows("select id_folder from plan_folders where " + scope
						+ " order by lower(folder_name), id_folder;");
				if (kids == null) {
					continue;
				}
				for (int j = 0; j < kids.size(); j++) {
					JsonObject kid = (JsonObject) kids.get(j);
					Integer id = coerceInteger(kid.get("id_folder"));
					if (id != null) {
						dml("update plan_folders set sort_order = " + j + " where id_folder = " + id + ";");
					}
				}
			}
		}
		JsonArray planParents = rows(
				"select distinct ifnull(folder_id, -1) as parent_key from explain_plans;");
		if (planParents != null) {
			for (int i = 0; i < planParents.size(); i++) {
				JsonObject row = (JsonObject) planParents.get(i);
				Integer parentKey = coerceInteger(row.get("parent_key"));
				if (parentKey == null) {
					continue;
				}
				String scope = parentKey.intValue() < 0
						? "folder_id is null"
						: "folder_id = " + parentKey.intValue();
				JsonArray kids = rows("select id_explain_plan from explain_plans where " + scope
						+ " order by lower(plan_name), id_explain_plan;");
				if (kids == null) {
					continue;
				}
				for (int j = 0; j < kids.size(); j++) {
					JsonObject kid = (JsonObject) kids.get(j);
					Integer id = coerceInteger(kid.get("id_explain_plan"));
					if (id != null) {
						dml("update explain_plans set sort_order = " + j
								+ " where id_explain_plan = " + id + ";");
					}
				}
			}
		}
	}

	private static Integer coerceInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return Integer.valueOf(((Number) value).intValue());
		}
		String text = String.valueOf(value);
		if (text.isBlank() || "null".equalsIgnoreCase(text)) {
			return null;
		}
		try {
			return Integer.valueOf(text.trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static String loadConfigString(String column, String defaultValue) {
		JsonObject row = row("select " + column + " from config;");
		if (row == null || row.get(column) == null) {
			return defaultValue;
		}
		String value = String.valueOf(row.get(column));
		if (value.isBlank() || "null".equalsIgnoreCase(value)) {
			return defaultValue;
		}
		return value;
	}

	/**
	 * Older schemas had a global UNIQUE on plan_name. Rebuild once so names are unique per folder.
	 */
	private static void migrateExplainPlansUniqueToFolderScope(double dblDBConfigVersion) {
		if (dblDBConfigVersion >= 4.11) {
			return;
		}
		JsonObject sqlRow = row("select sql from sqlite_master where type='table' and name='explain_plans';");
		if (sqlRow == null || sqlRow.get("sql") == null) {
			return;
		}
		String createSql = String.valueOf(sqlRow.get("sql")).toLowerCase();
		// Already folder-scoped (no table-level unique on plan_name alone).
		if (createSql.contains("plan_name varchar(256) not null")
				&& !createSql.contains("plan_name varchar(256) unique")) {
			return;
		}
		if (!createSql.contains("unique") || !createSql.contains("plan_name")) {
			return;
		}

		dml("begin transaction;");
		dml("create table explain_plans_new (\n"
				+ "id_explain_plan int unique not null\n"
				+ ", plan_name varchar(256) not null\n"
				+ ", folder_id integer\n"
				+ ", plan_text varchar(8192)\n"
				+ ", server varchar(256)\n"
				+ ", port integer\n"
				+ ", database varchar(256)\n"
				+ ", username varchar(256)\n"
				+ ", password varchar(256)\n"
				+ ", pre_explain_command varchar(256)\n"
				+ ", explain_sql_command varchar(8192)\n"
				+ ", post_explain_command varchar(256)\n"
				+ ", remember_password int not null default 0\n"
				+ ", auth_type varchar(64) not null default 'password'\n"
				+ ", write_version_environment_to_json int not null default 1\n"
				+ ", write_query_to_json int not null default 1\n"
				+ ", write_pg_environment_settings_to_json int not null default 1\n"
				+ ");");
		dml("insert into explain_plans_new (\n"
				+ "id_explain_plan, plan_name, folder_id, plan_text, server, port, database,\n"
				+ "username, password, pre_explain_command, explain_sql_command, post_explain_command,\n"
				+ "remember_password, auth_type, write_version_environment_to_json, write_query_to_json,\n"
				+ "write_pg_environment_settings_to_json)\n"
				+ "select id_explain_plan, plan_name, folder_id, plan_text, server, port, database,\n"
				+ "username, password, pre_explain_command, explain_sql_command, post_explain_command,\n"
				+ "remember_password, auth_type, write_version_environment_to_json, write_query_to_json,\n"
				+ "write_pg_environment_settings_to_json from explain_plans;");
		dml("drop table explain_plans;");
		dml("alter table explain_plans_new rename to explain_plans;");
		dml("commit transaction;");
	}

	/** Add a config column if an older database is missing it. */
	private static void ensureConfigColumn(String columnName, String columnSqlType) {
		ensureTableColumn("config", columnName, columnSqlType);
	}

	/** Add an explain_plans column if an older database is missing it. */
	private static void ensureExplainPlansColumn(String columnName, String columnSqlType) {
		ensureTableColumn("explain_plans", columnName, columnSqlType);
	}

	private static void ensureTableColumn(String tableName, String columnName, String columnSqlType) {
		JsonArray cols = rows("pragma table_info(" + tableName + ");");
		if (cols == null) {
			return;
		}
		for (int i = 0; i < cols.size(); i++) {
			JsonObject col = (JsonObject) cols.get(i);
			Object name = col.get("name");
			if (name != null && columnName.equalsIgnoreCase(String.valueOf(name))) {
				return;
			}
		}
		dml("alter table " + tableName + " add column " + columnName + " " + columnSqlType + ";");
	}
    
}
