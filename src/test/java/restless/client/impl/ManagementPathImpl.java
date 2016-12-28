package restless.client.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import restless.api.management.model.ListConfigItem;
import restless.api.management.model.ListConfigResponse;
import restless.client.api.ManagementData;
import restless.client.api.ManagementDataSchema;
import restless.client.api.ManagementPathJavaPackage;
import restless.client.api.ManagementPathLocation;
import restless.client.api.ManagementPathRoot;
import restless.client.api.ManagementPathServer;

final class ManagementPathImpl
		implements ManagementPathServer, ManagementPathLocation, ManagementPathRoot, ManagementPathJavaPackage
{
	private final TargetWrapper target;

	ManagementPathImpl(final TargetWrapper target)
	{
		this.target = checkNotNull(target);
	}

	@Override
	public ManagementPathServer server(final int port)
	{
		return new ManagementPathImpl(target.withSegment("server").withSegment(String.valueOf(port)));
	}

	@Override
	public ManagementPathLocation location(final String path)
	{
		return new ManagementPathImpl(target.withSegment("location").withEscapedSegment(path));
	}

	@Override
	public void bindToFilesystem()
	{
		final Map<String, Object> map = new HashMap<>();
		target.putObjectAsJson(map);
	}

	@Override
	public void bindToExternalFiles(final String absolutePath)
	{
		final Map<String, Object> map = new HashMap<>();
		map.put("alias", absolutePath);
		target.putObjectAsJson(map);
	}

	@Override
	public void delete()
	{
		target.delete();
	}

	@Override
	public boolean exists()
	{
		return target.exists("application/json");
	}

	@Override
	public List<ListConfigItem> listLocations()
	{
		return target.withSegment("location").getJson(ListConfigResponse.class).childResources();
	}

	@Override
	public String url()
	{
		return target.url();
	}

	@Override
	public ManagementData data(final String path)
	{
		return new ManagementDataImpl(target.withSegment("data").withSlashSeparatedSegments(path));
	}

	@Override
	public ManagementData file(final String file)
	{
		return new ManagementDataImpl(target.withSegment("file").withSegment(file));
	}

	@Override
	public ManagementPathJavaPackage javaPackage(final String pkg)
	{
		return new ManagementPathImpl(target.withSegment("java-pkg").withSegment(pkg));
	}

	@Override
	public ManagementDataSchema jsonSchema(final String schemaId)
	{
		return new ManagementDataImpl(target.withSegment("json-schema").withSegment(schemaId));
	}
}
