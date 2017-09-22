package servlet;

import java.nio.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import oracle.jdbc.pool.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.json.*;
 
public class DatabaseServlet extends HttpServlet {
	public static final String dbURL = "";
	public static final String localOracle = "";
	public static final String username = "";
	public static final String password = "";
	public static final String defaultQueryPath = "";
	
	/* Handles HTTP GET requests. */
   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException 
	{
		PrintWriter out = response.getWriter();
		System.setProperty("oracle.net.tns_admin", localOracle);
		
		ServletContext context = getServletContext();
		context.log("Received GET request");
		
		String queryString = readFile(defaultQueryPath);

		ResultSet rs = null;
		
		executeQuery(queryString);
	  
		//If the ResultSet was successfully retrieved from the db, write it back to the client in JSON
		if (rs != null)
		{
			JSONArray ja = null;
			try 
			{
				ja = convertToJSON(rs);
				
				if (ja != null)
					ja.write(out);
			}
			catch (Exception e)
			{
				context.log(e.toString());
			}
		}
	}
  
	/* Handles HTTP POST requests. */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
  {
		ServletContext context = getServletContext();
		context.log("Received POST request");
	
  }
  
	/* Executes an SQL query on the database. */
  private ResultSet executeQuery(String queryString)
  {
	ServletContext context = getServletContext();
  
	Driver d = null;
	
	boolean resultsExist = false;
	
	try
	{
		d = new oracle.jdbc.OracleDriver();
		DriverManager.registerDriver (d);
	}
	catch (Exception e)
	{
		context.log(e.toString());
	}

    Connection conn = null;
    Statement stmt = null;

    try 
	{
      conn = DriverManager.getConnection(dbURL, username, password);

      stmt = conn.createStatement();

      resultsExist = stmt.execute(queryString);
    } 
	catch (Exception e) 
	{
		context.log("Query failed: " + e.toString());
    }
    finally {
      if (stmt != null) try { stmt.close(); } catch (Exception e) {}
      if (conn != null) try { conn.close(); } catch (Exception e) {}
    }
	
	if (d != null)
	{
		try
		{
			DriverManager.deregisterDriver(d);
		}
		catch (Exception e) 
		{
			context.log("Deregister failed: " + e.toString());
		}
	}
	
	ResultSet rs = null;
	
	//Only attempt to retrieve results if they exist
	if (stmt != null && resultsExist)
	{
		try
		{
			stmt.getResultSet();
		}
		catch (SQLException e)
		{
			context.log(e.toString());
		}
	}
	
	//Return results if applicable
	if (rs != null)
		return rs;
	else
		return null;
}
  
  /* Converts a ResultSet from a SQL statement into an array of JSON data. */
  private JSONArray convertToJSON(ResultSet resultSet) throws Exception 
  {
        JSONArray jsonArray = new JSONArray();
        while (resultSet.next()) 
		{
            int total_rows = resultSet.getMetaData().getColumnCount();
            JSONObject obj = new JSONObject();
            for (int i = 0; i < total_rows; i++) 
			{
                obj.put(resultSet.getMetaData().getColumnLabel(i + 1).toLowerCase(), resultSet.getObject(i + 1));
            }
          jsonArray.put(obj);
        }
        return jsonArray;
    }
  
  /* Reads a file and returns a string of its contents. */
  private String readFile(String fileName) throws IOException 
  {
    BufferedReader br = new BufferedReader(new FileReader(fileName));
    try 
	{
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) 
		{
            sb.append(line);
            sb.append("\n");
            line = br.readLine();
        }
        return sb.toString();
	} 
	finally 
	{
		br.close();
	}
  }
}