
public class StartTestingServer
{
	public static void main(String[] args)
	{
		System.out.println("Starting server at port: 8000");
		TestingServer ts = new TestingServer();
		new Thread(ts).start();
	}
}
