package restless.api.java.model;

import java.util.List;

import com.google.inject.assistedinject.Assisted;

public interface ApiJavaModelFactory
{
	ListJavaPkgItem listJavaPkgItem(@Assisted("url") String url);

	ListJavaPkgResponse listJavaPkgResponse(List<ListJavaPkgItem> childResources);

	ListFileItem listFileItem(@Assisted("url") String url);

	ListFileResponse listFileResponse(List<ListFileItem> childResources);
}
