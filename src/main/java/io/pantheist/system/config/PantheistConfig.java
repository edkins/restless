package io.pantheist.system.config;

import java.io.File;

public interface PantheistConfig
{
	/**
	 * Default 3142. Point your browser at this one.
	 */
	int nginxPort();

	/**
	 * Default 3301
	 */
	int internalPort();

	/**
	 * Default 3302
	 */
	int postgresPort();

	/**
	 * @return the directory where all the files get put.
	 */
	File dataDir();

	/**
	 * Default is "system".
	 *
	 * @return relative path (e.g. "my-system/stuff") to the system config files.
	 */
	String relativeSystemPath();

	/**
	 * Default is "srv".
	 *
	 * @return relative path (e.g. "my-www/somewhere") to the static files which will get served up.
	 */
	String relativeSrvPath();

	/**
	 * Default is "project".
	 *
	 * @return relative path to store project-specific information such as kinds.
	 */
	String relativeProjectPath();

	String nginxExecutable();
}
