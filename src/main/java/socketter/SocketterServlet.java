package socketter;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

public class SocketterServlet extends WebSocketServlet {

	private static final long serialVersionUID = 1L;

	public SocketterServlet(){
	}

	@Override
	public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
		return new WebSocketTwitter();
	}

}
