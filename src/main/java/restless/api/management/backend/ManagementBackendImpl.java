package restless.api.management.backend;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;

import restless.api.management.model.ConfigRequest;
import restless.glue.nginx.filesystem.NginxFilesystemGlue;
import restless.handler.binding.backend.BindingStore;
import restless.handler.binding.backend.ManagementFunctions;
import restless.handler.binding.backend.PossibleData;
import restless.handler.binding.backend.PossibleEmpty;
import restless.handler.binding.backend.SchemaValidation;
import restless.handler.binding.model.Binding;
import restless.handler.binding.model.BindingMatch;
import restless.handler.binding.model.BindingModelFactory;
import restless.handler.binding.model.PathSpec;
import restless.handler.binding.model.PathSpecSegment;
import restless.handler.binding.model.Schema;
import restless.handler.filesystem.backend.FilesystemStore;
import restless.handler.filesystem.backend.FsPath;
import restless.handler.java.backend.JavaStore;
import restless.handler.nginx.manage.NginxService;

final class ManagementBackendImpl implements ManagementBackend
{
	private final BindingModelFactory bindingFactory;
	private final BindingStore bindingStore;
	private final FilesystemStore filesystem;
	private final NginxService nginxService;
	private final NginxFilesystemGlue nginxFilesystemGlue;
	private final SchemaValidation schemaValidation;
	private final JavaStore javaStore;

	@Inject
	ManagementBackendImpl(final BindingModelFactory bindingFactory,
			final BindingStore bindingStore,
			final FilesystemStore filesystem,
			final NginxService nginxService,
			final NginxFilesystemGlue nginxFilesystemGlue,
			final SchemaValidation schemaValidation,
			final JavaStore javaStore)
	{
		this.bindingFactory = checkNotNull(bindingFactory);
		this.bindingStore = checkNotNull(bindingStore);
		this.filesystem = checkNotNull(filesystem);
		this.nginxService = checkNotNull(nginxService);
		this.nginxFilesystemGlue = checkNotNull(nginxFilesystemGlue);
		this.schemaValidation = checkNotNull(schemaValidation);
		this.javaStore = checkNotNull(javaStore);
	}

	@Override
	public PathSpec pathSpec(final String path)
	{
		final ImmutableList.Builder<PathSpecSegment> builder = ImmutableList.builder();
		for (final String seg : path.split("\\/"))
		{
			builder.add(segment(seg));
		}
		return bindingFactory.pathSpec(builder.build());
	}

	private PathSpecSegment segment(final String seg)
	{
		if (seg.startsWith("+"))
		{
			return bindingFactory.literal(seg.substring(1));
		}
		else if (seg.equals("*"))
		{
			return bindingFactory.star();
		}
		else
		{
			throw new UnsupportedOperationException("Currently only literal path segments supported");
		}
	}

	private Binding changeFilesystemHandler(final FsPath bucket, final Binding b)
	{
		return bindingFactory.binding(
				b.pathSpec(),
				bindingFactory.filesystem(bucket),
				b.schema(),
				b.jerseyClass());
	}

	@Override
	public PossibleEmpty putConfig(final PathSpec pathSpec, final ConfigRequest config)
	{
		switch (config.handler()) {
		case filesystem:
			final FsPath bucket = filesystem.newBucket(pathSpec.nameHint());
			bindingStore.changeConfig(pathSpec, b -> changeFilesystemHandler(bucket, b));
			break;
		default:
			throw new UnsupportedOperationException("Unknown handler type: " + config.handler());
		}
		restartNginx();
		return PossibleEmpty.ok();
	}

	@Override
	public PossibleEmpty putData(final PathSpec path, final String data)
	{
		final BindingMatch match = bindingStore.lookup(path);
		return schemaValidation
				.validate(match.binding().schema(), data)
				.then(() -> functionsFor(match).putString(data));
	}

	@Override
	public PossibleData getData(final PathSpec path)
	{
		return functionsFor(bindingStore.lookup(path)).getString();
	}

	@Override
	public PossibleEmpty putJsonSchema(final PathSpec pathSpec, final JsonNode jsonNode)
	{
		final Schema schema = bindingFactory.jsonSchema(jsonNode);
		return schemaValidation.checkSchema(schema).then(() -> {
			bindingStore.changeConfig(pathSpec, b -> changeSchema(schema, b));
		});
	}

	private Binding changeSchema(final Schema schema, final Binding b)
	{
		return bindingFactory.binding(b.pathSpec(), b.handler(), schema, b.jerseyClass());
	}

	private ManagementFunctions functionsFor(final BindingMatch match)
	{
		final Binding binding = match.binding();
		switch (binding.handler().type()) {
		case filesystem:
			final FsPath path = binding.handler().filesystemBucket()
					.withPathSegments(match.pathMatch().nonLiteralChunk());
			return filesystem.manage(path);
		default:
			throw new UnsupportedOperationException("Unrecognized handler: " + binding.handler());
		}
	}

	private void restartNginx()
	{
		nginxService.configureAndStart(nginxFilesystemGlue.nginxConf());
	}

	@Override
	public Schema getSchema(final PathSpec pathSpec)
	{
		return bindingStore.exact(pathSpec).schema();
	}

	@Override
	public PossibleEmpty putJerseyFile(final PathSpec pathSpec, final String code)
	{
		return javaStore.storeJava(code).thenEmpty(className -> {
			bindingStore.changeConfig(pathSpec, b -> changeJerseyClass(className, b));
			return PossibleEmpty.ok();
		});
	}

	private Binding changeJerseyClass(final String jerseyClass, final Binding b)
	{
		return bindingFactory.binding(b.pathSpec(), b.handler(), b.schema(), jerseyClass);
	}

	@Override
	public PossibleData getJerseyFile(final PathSpec pathSpec)
	{
		final String jerseyClass = bindingStore.exact(pathSpec).jerseyClass();
		if (jerseyClass == null)
		{
			return PossibleData.doesNotExist();
		}
		else
		{
			return javaStore.getJava(jerseyClass);
		}
	}

}
