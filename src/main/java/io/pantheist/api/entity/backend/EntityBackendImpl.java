package io.pantheist.api.entity.backend;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.pantheist.api.entity.model.AddRequest;
import io.pantheist.api.entity.model.ApiEntityModelFactory;
import io.pantheist.api.entity.model.ListEntityItem;
import io.pantheist.api.entity.model.ListEntityResponse;
import io.pantheist.common.api.model.CommonApiModelFactory;
import io.pantheist.common.api.model.KindedMime;
import io.pantheist.common.api.model.ListClassifierItem;
import io.pantheist.common.api.model.ListClassifierResponse;
import io.pantheist.common.api.url.UrlTranslation;
import io.pantheist.common.util.FailureReason;
import io.pantheist.common.util.OtherPreconditions;
import io.pantheist.common.util.Possible;
import io.pantheist.common.util.View;
import io.pantheist.handler.filekind.backend.FileKindHandler;
import io.pantheist.handler.kind.backend.KindValidation;
import io.pantheist.handler.kind.model.Kind;
import io.pantheist.handler.kind.model.KindHandler;
import io.pantheist.system.config.PantheistConfig;

final class EntityBackendImpl implements EntityBackend
{
	private static final Logger LOGGER = LogManager.getLogger(EntityBackendImpl.class);
	private final FileKindHandler fileKindHandler;
	private final CommonApiModelFactory commonFactory;
	private final UrlTranslation urlTranslation;
	private final KindValidation kindValidation;
	private final ApiEntityModelFactory modelFactory;
	private final PantheistConfig config;
	private final ObjectMapper objectMapper;

	@Inject
	private EntityBackendImpl(
			final FileKindHandler fileKindHandler,
			final CommonApiModelFactory commonFactory,
			final UrlTranslation urlTranslation,
			final KindValidation kindValidation,
			final ApiEntityModelFactory modelFactory,
			final PantheistConfig config,
			final ObjectMapper objectMapper)
	{
		this.fileKindHandler = checkNotNull(fileKindHandler);
		this.commonFactory = checkNotNull(commonFactory);
		this.urlTranslation = checkNotNull(urlTranslation);
		this.kindValidation = checkNotNull(kindValidation);
		this.modelFactory = checkNotNull(modelFactory);
		this.config = checkNotNull(config);
		this.objectMapper = checkNotNull(objectMapper);
	}

	@Override
	public Possible<Void> add(final String kindId, final String entityId, final AddRequest req)
	{
		final Optional<Kind> kind = fileKindHandler.getKind(kindId);
		if (kind.isPresent())
		{
			if (kind.get().hasParent("file"))
			{
				return fileKindHandler.add(kind.get(), entityId, req.addName());
			}
			else
			{
				return FailureReason.KIND_DOES_NOT_SUPPORT.happened();
			}
		}
		else
		{
			return FailureReason.PARENT_DOES_NOT_EXIST.happened();
		}
	}

	@Override
	public ListClassifierResponse listEntityClassifiers()
	{
		return fileKindHandler.listAllEntityTexts(fileKindHandler.metakind())
				.optMap(this::readKindText)
				.filter(k -> k.computed().isEntityKind())
				.map(this::toListClassifierItem)
				.wrap(commonFactory::listClassifierResponse);
	}

	private Optional<Kind> readKindText(final String text)
	{
		try
		{
			return Optional.of(objectMapper.readValue(text, Kind.class));
		}
		catch (final IOException e)
		{
			LOGGER.catching(e);
			return Optional.empty();
		}
	}

	private ListClassifierItem toListClassifierItem(final Kind kind)
	{
		final String classifierKindUrl = urlTranslation.kindToUrl("pantheist-classifier");
		return commonFactory.listClassifierItem(urlTranslation.entitiesUrl(kind.kindId()),
				kind.kindId(), false, classifierKindUrl);
	}

	private ListEntityItem toListEntityItem(final ObjectNode entity)
	{
		final String kindId = entity.get("kindId").textValue();
		final String entityId = entity.get("entityId").textValue();
		return modelFactory.listEntityItem(
				urlTranslation.entityToUrl(kindId, entityId),
				entityId,
				urlTranslation.kindToUrl(kindId));
	}

	@Override
	public Possible<ListEntityResponse> listEntitiesWithKind(final String kindId)
	{
		OtherPreconditions.checkNotNullOrEmpty(kindId);
		if (!fileKindHandler.getKind(kindId).isPresent())
		{
			return FailureReason.DOES_NOT_EXIST.happened();
		}
		return View.ok(kindValidation.objectsWithKind(kindId)
				.antiIt()
				.map(this::toListEntityItem)
				.wrap(modelFactory::listEntityResponse));
	}

	@Override
	public Possible<KindedMime> getEntity(final String kindId, final String entityId)
	{
		final Optional<Kind> kind = fileKindHandler.getKind(kindId);
		if (kind.isPresent())
		{
			if (kind.get().computed().handler() == KindHandler.file)
			{
				return fileKindHandler.getEntity(kind.get(), entityId);
			}
			else
			{
				return FailureReason.KIND_DOES_NOT_SUPPORT.happened();
			}
		}
		else
		{
			return FailureReason.PARENT_DOES_NOT_EXIST.happened();
		}
	}

	@Override
	public Possible<Void> putEntity(
			final String kindId,
			final String entityId,
			final String contentType,
			final String text,
			final boolean failIfExists)
	{
		OtherPreconditions.checkNotNullOrEmpty(contentType);
		final Optional<Kind> kind = fileKindHandler.getKind(kindId);
		if (kind.isPresent())
		{
			if (!contentType.equals(kind.get().computed().mimeType()))
			{
				return FailureReason.UNSUPPORTED_MEDIA_TYPE.happened();
			}
			if (kind.get().computed().handler() == KindHandler.file)
			{
				return fileKindHandler.putEntity(kind.get(), entityId, text, failIfExists);
			}
			else
			{
				return FailureReason.KIND_DOES_NOT_SUPPORT.happened();
			}
		}
		else
		{
			return FailureReason.PARENT_DOES_NOT_EXIST.happened();
		}
	}

	@Override
	public Possible<Void> deleteEntity(final String kindId, final String entityId)
	{
		final Optional<Kind> kind = fileKindHandler.getKind(kindId);
		if (kind.isPresent())
		{
			if (kind.get().computed().handler() == KindHandler.file)
			{
				return fileKindHandler.deleteEntity(kind.get(), entityId);
			}
			else
			{
				return FailureReason.KIND_DOES_NOT_SUPPORT.happened();
			}
		}
		else
		{
			return FailureReason.PARENT_DOES_NOT_EXIST.happened();
		}
	}
}
