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
import restless.handler.binding.model.ConfigId;
import restless.handler.binding.model.PathSpec;
import restless.handler.binding.model.PathSpecSegment;
import restless.handler.binding.model.Schema;
import restless.handler.filesystem.backend.FilesystemStore;
import restless.handler.filesystem.backend.FsPath;
import restless.handler.java.backend.JavaStore;

final class ManagementBackendImpl implements ManagementBackend
{
	private final BindingModelFactory bindingFactory;
	private final BindingStore bindingStore;
	private final FilesystemStore filesystem;
	private final NginxFilesystemGlue nginxFilesystemGlue;
	private final SchemaValidation schemaValidation;
	private final JavaStore javaStore;

	@Inject
	ManagementBackendImpl(final BindingModelFactory bindingFactory,
			final BindingStore bindingStore,
			final FilesystemStore filesystem,
			final NginxFilesystemGlue nginxFilesystemGlue,
			final SchemaValidation schemaValidation,
			final JavaStore javaStore)
	{
		this.bindingFactory = checkNotNull(bindingFactory);
		this.bindingStore = checkNotNull(bindingStore);
		this.filesystem = checkNotNull(filesystem);
		this.nginxFilesystemGlue = checkNotNull(nginxFilesystemGlue);
		this.schemaValidation = checkNotNull(schemaValidation);
		this.javaStore = checkNotNull(javaStore);
	}

	@Override
	public ConfigId pathSpec(final String path)
	{
		final ImmutableList.Builder<PathSpecSegment> builder = ImmutableList.builder();
		for (final String seg : path.split("\\/"))
		{
			builder.add(segment(seg));
		}
		return bindingFactory.configId(builder.build());
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
				b.jerseyClass(),
				b.configId());
	}

	@Override
	public PossibleEmpty putConfig(final ConfigId pathSpec, final ConfigRequest config)
	{
		switch (config.handler()) {
		case filesystem:
		{
			final FsPath bucket = filesystem.newBucket(pathSpec.nameHint());
			bindingStore.changeConfig(pathSpec, b -> changeFilesystemHandler(bucket, b));
			break;
		}
		case resource_files:
		{
			final FsPath bucket = filesystem.systemBucket().segment("resource-files")
					.slashSeparatedSegments(config.handlerPath());
			bindingStore.changeConfig(pathSpec, b -> changeFilesystemHandler(bucket, b));
			break;
		}
		default:
			throw new UnsupportedOperationException("Unknown handler type: " + config.handler());
		}
		restartServers();
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
	public PossibleEmpty putJsonSchema(final ConfigId pathSpec, final JsonNode jsonNode)
	{
		final Schema schema = bindingFactory.jsonSchema(jsonNode);
		return schemaValidation.checkSchema(schema).then(() -> {
			bindingStore.changeConfig(pathSpec, b -> changeSchema(schema, b));
		});
	}

	private Binding changeSchema(final Schema schema, final Binding b)
	{
		return bindingFactory.binding(b.pathSpec(), b.handler(), schema, b.jerseyClass(), b.configId());
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

	private void restartServers()
	{
		nginxFilesystemGlue.startStopOrRestart();
	}

	@Override
	public Schema getSchema(final ConfigId pathSpec)
	{
		return bindingStore.exact(pathSpec).schema();
	}

	@Override
	public PossibleEmpty putJerseyFile(final ConfigId pathSpec, final String code)
	{
		return javaStore.storeJava(code).thenEmpty(className -> {
			bindingStore.changeConfig(pathSpec, b -> changeJerseyClass(className, b));
			return PossibleEmpty.ok();
		});
	}

	private Binding changeJerseyClass(final String jerseyClass, final Binding b)
	{
		return bindingFactory.binding(b.pathSpec(), b.handler(), b.schema(), jerseyClass, b.configId());
	}

	@Override
	public PossibleData getJerseyFile(final ConfigId pathSpec)
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

	@Override
	public PathSpec literalPath(final String path)
	{
		final ImmutableList.Builder<PathSpecSegment> builder = ImmutableList.builder();
		for (final String seg : path.split("\\/"))
		{
			builder.add(segment(seg));
		}
		return bindingFactory.pathSpec(builder.build());
	}

}
