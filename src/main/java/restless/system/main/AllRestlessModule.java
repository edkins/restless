package restless.system.main;

import com.google.inject.PrivateModule;

import restless.api.something.resource.ApiSomethingResourceModule;
import restless.system.config.SystemConfigModule;
import restless.system.server.RestlessServer;
import restless.system.server.SystemServerModule;

public class AllRestlessModule extends PrivateModule
{

	@Override
	protected void configure()
	{
		expose(RestlessServer.class);
		install(new ApiSomethingResourceModule());
		install(new SystemConfigModule());
		install(new SystemServerModule());
	}

}
