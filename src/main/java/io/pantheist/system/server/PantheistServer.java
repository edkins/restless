package io.pantheist.system.server;

public interface PantheistServer
{
	/**
	 * Start the server running on the configured port.
	 *
	 * @throws StartupException
	 */
	void start();

	/**
	 * Stop the server if it's running. Only really used for testing, normally
	 * you would just kill the process.
	 *
	 * @throws RuntimeException
	 *             if something went wrong
	 */
	void stop();
}
