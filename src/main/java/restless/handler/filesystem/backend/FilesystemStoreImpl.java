package restless.handler.filesystem.backend;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import restless.common.util.FailureReason;
import restless.common.util.Possible;
import restless.common.util.View;

final class FilesystemStoreImpl implements FilesystemStore
{
	final ObjectMapper objectMapper;
	private final FilesystemFactory factory;

	@Inject
	FilesystemStoreImpl(final ObjectMapper objectMapper,
			final FilesystemFactory factory)
	{
		this.objectMapper = checkNotNull(objectMapper);
		this.factory = checkNotNull(factory);
	}

	@Override
	public void initialize()
	{
		final FilesystemSnapshot snapshot = snapshot();
		final FsPath path = systemBucket();
		if (!snapshot.isDir(path))
		{
			snapshot.writeSingle(path, File::mkdir);
		}
	}

	private FsPath fromBucketName(final String bucketName)
	{
		return FsPathImpl.empty().segment(bucketName);
	}

	@Override
	public FsPath systemBucket()
	{
		return fromBucketName("system");
	}

	@Override
	public FsPath srvBucket()
	{
		return fromBucketName("srv");
	}

	@Override
	public FsPath rootPath()
	{
		return FsPathImpl.empty();
	}

	@Override
	public FilesystemSnapshot snapshot()
	{
		return factory.snapshot();
	}

	@Override
	public <T> JsonSnapshot<T> jsonSnapshot(final FsPath path, final Class<T> clazz)
	{
		return new JsonSnapshotImpl<>(snapshot(), objectMapper, path, clazz);
	}

	@Override
	public Possible<String> getSrvData(final String relativePath)
	{
		final FilesystemSnapshot snapshot = snapshot();
		final FsPath path = srvBucket().slashSeparatedSegments(relativePath);
		if (snapshot.isFile(path))
		{
			final String data = snapshot.readText(path);
			return View.ok(data);
		}
		else
		{
			return FailureReason.DOES_NOT_EXIST.happened();
		}
	}

	@Override
	public Possible<Void> putSrvData(final String relativePath, final String data)
	{
		final FilesystemSnapshot snapshot = snapshot();
		final FsPath path = srvBucket().slashSeparatedSegments(relativePath);
		snapshot.willNeedDirectory(path.parent());
		snapshot.isFile(path); // throws an exception if it exists but is not a regular file
		snapshot.writeSingleText(path, data);
		return View.noContent();
	}
}
