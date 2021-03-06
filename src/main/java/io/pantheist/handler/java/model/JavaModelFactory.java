package io.pantheist.handler.java.model;

import com.google.inject.assistedinject.Assisted;

public interface JavaModelFactory
{
	JavaComponent component(@Assisted("isRoot") boolean isRoot);

	JavaFileId fileId(@Assisted("pkg") String pkg, @Assisted("file") String file);

	JavaBinding javaBinding(@Assisted("location") String location);
}
