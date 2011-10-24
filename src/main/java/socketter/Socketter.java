package socketter;

import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Socketter {

	private Server server;
	
	public static void main(String[] args) {
		int port = 10443;
		boolean ssl = true;
		try {
			port = Integer.parseInt(args[0]);
		} catch (ArrayIndexOutOfBoundsException e){
			port = 10443;
		}
		try {
			if(args[1].length() > 0) ssl = false;
		} catch (ArrayIndexOutOfBoundsException e){
			ssl = true;
		}

		try {
			new Socketter(port, ssl);
		} catch (Exception e) {
//			e.printStackTrace();
		}
	}

	public Socketter(int port, boolean ssl) throws Exception {

		server = new Server();
		
		if(ssl){
			String keystorePath = this.getClass().getClassLoader().getResource("ssl/localhost.keystore").toExternalForm();
			String keystorePassword = "localhost";
			SslContextFactory scf = new SslContextFactory();
			scf.setKeyStore(keystorePath);
			scf.setKeyStorePassword(keystorePassword);
			scf.setTrustStore(keystorePath);
			scf.setTrustStorePassword(keystorePassword);
			try{
				scf.checkKeyStore();
			}catch(Exception e){
				throw new Exception("checkKeyStore error : " + e.getMessage());
			}
			SslSelectChannelConnector sslconnector = new SslSelectChannelConnector(scf);
			sslconnector.setPort(port);
			server.addConnector(sslconnector);
		} else {
			Connector connector = new SelectChannelConnector();
			connector.setPort(10080);
			server.addConnector(connector);
		}
	
		ResourceHandler rh = new ResourceHandler();
		rh.setResourceBase(this.getClass().getClassLoader().getResource("html").toExternalForm());
		
		SocketterServlet wss = new SocketterServlet();
		ServletHolder sh = new ServletHolder(wss);
		ServletContextHandler sch = new ServletContextHandler();
		sch.addServlet(sh, "/ws/*");
		
		HandlerList hl = new HandlerList();
		hl.setHandlers(new Handler[] {rh, sch});
		server.setHandler(hl);

		try {
			server.start();
		} catch (Exception e) {
		}

	  }

}
