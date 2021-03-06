package org.nutz.ngqa;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

public class ServerLauncher {

	public static void main(String[] args) throws Throwable {
		String warUrlString = "ROOT";
		WebAppContext appContext = new WebAppContext(warUrlString, "/");
		Server server = new Server(80);
		server.setHandler(appContext);

		// 启动
		server.start();
	}
}
