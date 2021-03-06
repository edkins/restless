package io.pantheist.handler.java.backend;

import com.google.inject.PrivateModule;
import com.google.inject.Scopes;

public class HandlerJavaBackendModule extends PrivateModule
{

	@Override
	protected void configure()
	{
		expose(JavaStore.class);
		bind(JavaStore.class).to(JavaStoreImpl.class).in(Scopes.SINGLETON);
		bind(JavaSqlLogic.class).to(JavaSqlLogicImpl.class).in(Scopes.SINGLETON);
		bind(JavaParse.class).to(JavaParseImpl.class).in(Scopes.SINGLETON);
	}

}
