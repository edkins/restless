package restless.api.kind.resource;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import restless.common.annotations.ResourceTag;

public class ApiKindResourceModule extends AbstractModule
{

	@Override
	protected void configure()
	{
		final Multibinder<ResourceTag> multi = Multibinder.newSetBinder(binder(), ResourceTag.class);
		multi.addBinding().to(KindResourceImpl.class);
	}

}
