package utils.samples.tcp.clients;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPGreetingsClient {
	private Socket clientSocket;
	private PrintWriter out;
	private BufferedReader in;

	public void startConnection(String ip, int port) throws Exception {
		clientSocket = new Socket(ip, port);
		out = new PrintWriter(clientSocket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	}

	public String sendMessage(String msg) throws Exception {
		out.println(msg);
		String resp = in.readLine();
		return resp;
	}

	public void stopConnection() throws Exception {
		in.close();
		out.close();
		clientSocket.close();
	}

	public static void main(String... args) {
		TCPGreetingsClient client = new TCPGreetingsClient();
		try {
			client.startConnection("127.0.0.1", 6666);
			String response = client.sendMessage("hello server");
			System.out.println(String.format("Server responded %s", response));
		} catch (Exception ex) {
			// Ooch!
			ex.printStackTrace();
		}
	}
}
