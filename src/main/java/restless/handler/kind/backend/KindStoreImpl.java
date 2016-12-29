package restless.handler.kind.backend;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;

import restless.common.util.FailureReason;
import restless.common.util.Possible;
import restless.common.util.View;
import restless.handler.filesystem.backend.FilesystemStore;
import restless.handler.filesystem.backend.FsPath;
import restless.handler.filesystem.backend.JsonSnapshot;
import restless.handler.kind.model.Kind;

final class KindStoreImpl implements KindStore
{
	private final FilesystemStore filesystem;

	@Inject
	private KindStoreImpl(final FilesystemStore filesystem)
	{
		this.filesystem = checkNotNull(filesystem);
	}

	@Override
	public Possible<Void> putKind(final String kindId, final Kind kind)
	{
		final JsonSnapshot<Kind> snapshot = filesystem.jsonSnapshot(path(kindId), Kind.class);

		snapshot.exists(); // don't care
		snapshot.write(kind);
		return View.noContent();
	}

	private FsPath path(final String kindId)
	{
		return filesystem.systemBucket().segment("kind").segment(kindId);
	}

	@Override
	public Possible<Kind> getKind(final String kindId)
	{
		final JsonSnapshot<Kind> snapshot = filesystem.jsonSnapshot(path(kindId), Kind.class);

		if (snapshot.exists())
		{
			return View.ok(snapshot.read());
		}
		else
		{
			return FailureReason.DOES_NOT_EXIST.happened();
		}
	}

}