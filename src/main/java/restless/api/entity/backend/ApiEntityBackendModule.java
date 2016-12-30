package restless.api.entity.backend;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

public class ApiEntityBackendModule extends PrivateModule
{

	@Override
	protected void configure()
	{
		expose(EntityBackend.class);
		bind(EntityBackend.class).to(EntityBackendImpl.class).in(Scopes.SINGLETON);
	}

}
