package io.pantheist.handler.nginx.manage;

import java.util.List;
import java.util.Optional;

import io.pantheist.common.util.Possible;

public interface NginxService
{
	void generateConfIfMissing();

	void startOrRestart();

	void stop();

	boolean hasLocation(int port, String location);

	Possible<Void> deleteLocationAndRestart(int port, String location);

	Possible<List<String>> listLocations(int port);

	Possible<Void> putAndRestart(int port, String location, Optional<String> alias);
}
