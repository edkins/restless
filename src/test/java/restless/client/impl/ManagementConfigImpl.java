package restless.client.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import restless.api.management.model.ListConfigResponse;
import restless.client.api.ManagementConfig;
import restless.client.api.ManagementConfigPoint;

final class ManagementConfigImpl implements ManagementConfig
{
	private final TargetWrapper target;

	ManagementConfigImpl(final TargetWrapper target)
	{
		this.target = checkNotNull(target);
	}

	@Override
	public ManagementConfigPoint create(final String path)
	{
		final Map<String, Object> map = new HashMap<>();
		map.put("pathSpec", path);
		final TargetWrapper resultPath = target.withSegment("config").createObjectAsJsonWithPostRequest(map);

		return new ManagementConfigPointImpl(resultPath);
	}

	@Override
	public ListConfigResponse list()
	{
		return target.withSegment("config").getJson(ListConfigResponse.class);
	}

}
