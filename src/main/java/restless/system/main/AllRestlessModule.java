package restless.system.main;

import com.google.inject.PrivateModule;

import restless.api.kind.backend.ApiKindBackendModule;
import restless.api.kind.model.ApiKindModelModule;
import restless.api.kind.resource.ApiKindResourceModule;
import restless.api.management.backend.ApiManagementBackendModule;
import restless.api.management.model.ApiManagementModelModule;
import restless.api.management.resource.ApiManagementResourceModule;
import restless.common.http.CommonHttpModule;
import restless.handler.entity.backend.HandlerEntityBackendModule;
import restless.handler.entity.model.HandlerEntityModuleModule;
import restless.handler.filesystem.backend.HandlerFilesystemBackendModule;
import restless.handler.java.backend.HandlerJavaBackendModule;
import restless.handler.java.model.HandlerJavaModelModule;
import restless.handler.kind.backend.HandlerKindBackendModule;
import restless.handler.kind.model.HandlerKindModelModule;
import restless.handler.nginx.manage.HandlerNginxManageModule;
import restless.handler.nginx.parser.HandlerNginxParserModule;
import restless.handler.schema.backend.HandlerSchemaBackendModule;
import restless.handler.schema.model.HandlerSchemaModelModule;
import restless.handler.uri.HandlerUriModule;
import restless.system.config.SystemConfigModule;
import restless.system.initializer.Initializer;
import restless.system.initializer.SystemInitializerModule;
import restless.system.inject.SystemInjectModule;
import restless.system.server.SystemServerModule;

public class AllRestlessModule extends PrivateModule
{

	@Override
	protected void configure()
	{
		expose(Initializer.class);
		install(new ApiKindBackendModule());
		install(new ApiKindModelModule());
		install(new ApiKindResourceModule());
		install(new ApiManagementBackendModule());
		install(new ApiManagementModelModule());
		install(new ApiManagementResourceModule());
		install(new CommonHttpModule());
		install(new HandlerEntityBackendModule());
		install(new HandlerEntityModuleModule());
		install(new HandlerFilesystemBackendModule());
		install(new HandlerJavaBackendModule());
		install(new HandlerJavaModelModule());
		install(new HandlerKindBackendModule());
		install(new HandlerKindModelModule());
		install(new HandlerNginxManageModule());
		install(new HandlerNginxParserModule());
		install(new HandlerSchemaBackendModule());
		install(new HandlerSchemaModelModule());
		install(new HandlerUriModule());
		install(new SystemConfigModule());
		install(new SystemInitializerModule());
		install(new SystemInjectModule());
		install(new SystemServerModule());
	}

}
