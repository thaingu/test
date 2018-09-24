import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class TestingServer implements Runnable
{
	private String serverHost = "localhost";
	private int serverPort = 8000;
	private KeyStore clientKeyStore;
	private KeyStore serverKeyStore;
	private SSLContext sslContext;
	static private final String passphrase = "serverpw";
	static private SecureRandom secureRandom;
	private SSLServerSocket ss;
	
	@Override
	public void run()
	{
		try
		{
			InetAddress serverInet = InetAddress.getByName(serverHost);
			setupClientKeyStore();
			setupServerKeystore();
			setupSSLContext();
			
			SSLServerSocketFactory sf = sslContext.getServerSocketFactory();
		    SSLServerSocket ss = (SSLServerSocket) sf.createServerSocket(serverPort, 0, serverInet);
		    ss.setNeedClientAuth(false);
			System.out.println("Server started at host: " + ss.getInetAddress() + " port: " + ss.getLocalPort() + "\n");			
			while(true)
			{
				Socket socket = ss.accept();
				System.out.println("Connection established with the client at " + socket.getInetAddress() + ":" + socket.getPort());
				RequestHandler rh = new RequestHandler(socket);			
				new Thread(rh).start();				
			}		
		}
		catch (UnknownHostException e)
		{
			System.err.println("UnknownHostException for the hostname: " + serverHost);
		}
		catch (IllegalArgumentException iae)
		{
			System.err.println("EXCEPTION in starting the SERVER: " + iae.getMessage());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (GeneralSecurityException gse)
		{
			gse.printStackTrace();
		}
		finally
		{
			try
			{
				if(ss != null)
				{
					ss.close();
				}
			}
			catch (IOException e)
			{
				System.err.println("EXCEPTION in closing the server socket." + e);
			}
		}
		
	}
	
	private void setupClientKeyStore() throws GeneralSecurityException, IOException
	{
		clientKeyStore = KeyStore.getInstance("JKS");
	    clientKeyStore.load( new FileInputStream("client.public"), "public".toCharArray());
    }
	
	private void setupServerKeystore() throws GeneralSecurityException, IOException
	{
		serverKeyStore = KeyStore.getInstance("JKS");
	    serverKeyStore.load( new FileInputStream("server.private"), passphrase.toCharArray());
	}
	
	private void setupSSLContext() throws GeneralSecurityException, IOException
	{
	    TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	    tmf.init( clientKeyStore );

	    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	    kmf.init( serverKeyStore, passphrase.toCharArray() );

	    sslContext = SSLContext.getInstance( "SSLv3" );
	    sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), secureRandom );
	  }

}
