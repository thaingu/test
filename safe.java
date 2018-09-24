import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class safe implements Runnable
{
	private Socket clientSocket;
	private final String CRLF = "\r\n";
	private final String SP = " ";
	private int statusCode;W
	private String filePath;
	
	public safe(Socket soc)
	{
		this.clientSocket = soc;
	}

	@Override
	public void run()
	{
		BufferedReader in = null; 
		DataOutputStream out = null;
		FileInputStream fis = null;
		
		try
		{
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			out = new DataOutputStream(clientSocket.getOutputStream());
			String line = in.readLine();
			StringBuilder raw = new StringBuilder();
			raw.append("" + line);
			boolean isPost = line.startsWith("POST");
			boolean isGet = line.startsWith("GET");
			int contentLength = 0;
			
			String pathName = readPacket(line);
			File file = new File(pathName);
			try
			{
				if (file.isFile() && file.exists())
				{						
					/**HTTP Response Format:
					 * HTTP/1.0 200 OK CRLF
					 * Content-type: text/html CRLF
					 * CRLF
					 * FILE_CONTENT....
					 * FILE_CONTENT....
					 * FILE_CONTENT....
					 */
					setStatus(200);
					String responseLine = "HTTP/1.0" + SP + "statusCode" + SP + getStatusMessage(statusCode) + CRLF;
					out.writeBytes(responseLine);
					out.writeBytes("Content-type: " + getContentType(filePath) + CRLF);					
					out.writeBytes(CRLF);			
					fis = new FileInputStream(file);
	
					byte[] buffer = new byte[1024];
					int bytes = 0;
	
					while((bytes = fis.read(buffer)) != -1 )
					{
						out.write(buffer, 0, bytes);
					}
					
					System.out.println("Sending Response with status line: " + responseLine);
					out.flush();
					System.out.println("HTTP Response sent");
					
				}
				else
				{
					setStatus(404);
					System.out.println("ERROR: Requested filePath " + filePath + " does not exist");
					String responseLine = "HTTP/1.0" + SP + "statusCode" + SP + getStatusMessage(statusCode) + CRLF;
					out.writeBytes(responseLine);
					out.writeBytes("Content-type: text/html" + CRLF);							
					out.writeBytes(CRLF);							
					out.writeBytes(getErrorFile());							
					System.out.println("Sending Response with status line: " + responseLine);							
					out.flush();
					System.out.println("HTTP Response sent");
				}			
			}
			catch (FileNotFoundException e)
			{
				System.err.println("EXCEPTION: Requested filePath " + filePath + " does not exist");
			}
			catch (IOException e)
			{
				System.err.println("EXCEPTION in processing request." + e.getMessage());
			}
		} 
		catch (IOException e) 
		{
			System.err.println("EXCEPTION in processing request." + e.getMessage());
			
		}
		finally
		{
			try
			{
				if (fis != null)
				{
					fis.close();
				}
				if (in != null)
				{
					in.close();
				}
				if (out != null)
				{
					out.close();
				}
				if (clientSocket != null)
				{
					clientSocket.close();
					System.out.println("Closing the connection.\n");
				}
			}
			catch (IOException e)
			{
				System.err.println("EXCEPTION in closing resource." + e);
			}
		}
	}
	
	/**
	 * Get Content-type of the file using its extension
	 * @param filePath
	 * @return content type
	 */
	
	private String getContentType(String filePath)
	{
		if(filePath.endsWith(".html") || filePath.endsWith(".htm"))
		{
			return "text/html";
		}
		return "application/octet-stream";
	}
	
	/**
	 * Get content of a general 404 error file
	 * @return errorFile content
	 */
	private String getErrorFile()
	{
		String errorFileContent = 	"<!doctype html>" + "\n" +
									"<html lang=\"en\">" + "\n" +
									"<head>" + "\n" +
									"    <meta charset=\"UTF-8\">" + "\n" +
									"    <title>Error 404</title>" + "\n" +
									"</head>" + "\n" +
									"<body>" + "\n" +
									"    <b>ErrorCode:</b> 404" + "\n" +
									"    <br>" + "\n" +
									"    <b>Error Message:</b> The requested file does not exist on this server." + "\n" +
									"</body>" + "\n" +
									"</html>";
		return errorFileContent;
	}
	
	private String readPacket(String line)
	{
		if(line != null)
		{
			String[] msgParts = line.split(SP);
			if (msgParts[0].equals("GET") && msgParts.length == 3)
			{
				filePath = msgParts[1];
				setFilePath(filePath);
				if(filePath.indexOf("/") != 0)
				{	
					filePath = "/" + filePath;
				}				
				System.out.println("Requested filePath: " + filePath);				
				if(filePath.equals("/"))
				{
					System.out.println("Respond with default /login.html file");
					filePath = filePath + "login.html";
				}
				
				filePath = "." + filePath;

				
				return filePath;
			}
			else if (msgParts[0].equals("POST"))
			{
				System.out.println(line);
			}
			else
			{
				System.err.println("Invalid HTTP GET Request. " + msgParts[0]);
			}			
		}
		else
		{
			System.err.println("Discarding a NULL/unknown HTTP request.");
		}
		return null;

	}
	
	private void setStatus(int statusCode)
	{
		statusCode = this.statusCode;
	}
	
	private void setFilePath(String filePath)
	{
		filePath = this.filePath;
	}
	
	private static String getStatusMessage(int statusCode)
	{
		switch (statusCode)
		{
		case 200: return "OK";
		case 404: return "Not Found";
		default: return "Unknown Status";
		}
	}
}
