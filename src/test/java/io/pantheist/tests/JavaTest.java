package io.pantheist.tests;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import io.pantheist.api.java.model.ListJavaFileItem;
import io.pantheist.testclient.api.ManagementPathJavaFile;
import io.pantheist.testclient.api.ManagementPathRoot;
import io.pantheist.testclient.api.ResponseType;
import io.pantheist.testhelpers.classrule.TestSessionImpl;
import io.pantheist.testhelpers.rule.MainRule;

public class JavaTest
{
	@ClassRule
	public static final TestSessionImpl outerRule = TestSessionImpl.forApi();

	@Rule
	public final MainRule mainRule = MainRule.forNewTest(outerRule);

	private ManagementPathRoot manage;

	private static final String JAVA_SYNTAXERROR_RES = "/java-example/java-syntax-error";
	private static final String TEXT_PLAIN = "text/plain";
	private static final String JAVA_JERSEY_NAME = "ExampleJerseyResource";
	private static final String JAVA_JERSEY_RES = "/java-example/ExampleJerseyResource";
	private static final String JAVA_PKG = "io.pantheist.examples";
	private static final String JAVA_EMPTY_CLASS_RES = "/java-example/EmptyClass";
	private static final String JAVA_EMPTY_CLASS_NAME = "EmptyClass";

	@Before
	public void setup()
	{
		manage = mainRule.actions().manage();
	}

	@Test
	public void java_canPutSomewhere_andReadItBack() throws Exception
	{
		manage.javaPackage(JAVA_PKG)
				.file(JAVA_JERSEY_NAME)
				.data()
				.putResource(JAVA_JERSEY_RES, TEXT_PLAIN);

		final String data = manage
				.javaPackage(JAVA_PKG)
				.file(JAVA_JERSEY_NAME)
				.data()
				.getString(TEXT_PLAIN);

		assertThat(data, is(mainRule.resource(JAVA_JERSEY_RES)));
	}

	@Test
	public void java_canPutTwice() throws Exception
	{
		manage.javaPackage(JAVA_PKG)
				.file(JAVA_JERSEY_NAME)
				.data()
				.putResource(JAVA_JERSEY_RES, TEXT_PLAIN);

		manage.javaPackage(JAVA_PKG)
				.file(JAVA_EMPTY_CLASS_NAME)
				.data()
				.putResource(JAVA_EMPTY_CLASS_RES, TEXT_PLAIN);
	}

	@Test
	public void invalidJava_cannotStore() throws Exception
	{
		final ResponseType responseType = manage.javaPackage(JAVA_PKG)
				.file(JAVA_JERSEY_NAME)
				.data()
				.putResourceResponseType(JAVA_SYNTAXERROR_RES, TEXT_PLAIN);

		assertEquals(ResponseType.BAD_REQUEST, responseType);
	}

	@Test
	public void java_canList() throws Exception
	{
		final ManagementPathJavaFile file = manage.javaPackage(JAVA_PKG).file(JAVA_EMPTY_CLASS_NAME);
		file.data().putResource(JAVA_EMPTY_CLASS_RES, TEXT_PLAIN);

		final List<ListJavaFileItem> list = manage.javaPackage(JAVA_PKG).listJavaFiles().childResources();

		assertThat(list.size(), is(1));
		assertThat(list.get(0).url(), is(file.url()));
	}

	@Test
	public void java_canDelete() throws Exception
	{
		final ManagementPathJavaFile file = manage.javaPackage(JAVA_PKG)
				.file(JAVA_EMPTY_CLASS_NAME);
		file.data().putResource(JAVA_EMPTY_CLASS_RES, TEXT_PLAIN);

		assertThat(file.getJavaFileResponseType(), is(ResponseType.OK));

		file.delete();

		assertThat(file.getJavaFileResponseType(), is(ResponseType.NOT_FOUND));
	}

	@Test
	public void java_notThere_deleteReturnsNotFound() throws Exception
	{
		final ManagementPathJavaFile file = manage.javaPackage(JAVA_PKG)
				.file(JAVA_EMPTY_CLASS_NAME);

		assertThat(file.deleteResponseType(), is(ResponseType.NOT_FOUND));
	}

	@Test
	public void java_rebind_itDisappears_rebindBackAgain_itReappears() throws Exception
	{
		final ManagementPathJavaFile file = manage.javaPackage(JAVA_PKG)
				.file(JAVA_EMPTY_CLASS_NAME);

		assertThat(manage.javaBinding().getJavaBinding().location(), is("system/java"));

		file.data().putResource(JAVA_EMPTY_CLASS_RES, TEXT_PLAIN);

		manage.javaBinding().setJavaBinding("some/other/thing");
		assertThat(manage.javaBinding().getJavaBinding().location(), is("some/other/thing"));

		assertThat(file.data().getResponseTypeForContentType(TEXT_PLAIN), is(ResponseType.NOT_FOUND));
		manage.javaBinding().setJavaBinding("system/java");
		assertThat(file.data().getResponseTypeForContentType(TEXT_PLAIN), is(ResponseType.OK));
	}

	@Test
	public void java_post_discoversPackage_andClass() throws Exception
	{
		final String code = mainRule.resource(JAVA_EMPTY_CLASS_RES);
		final String url = manage.kind("java-file").postCreate(code, TEXT_PLAIN);

		final ManagementPathJavaFile file = manage.javaPackage(JAVA_PKG).file(JAVA_EMPTY_CLASS_NAME);
		assertThat(url, is(file.url()));
		assertThat(file.data().getString(TEXT_PLAIN), is(code));
	}

	@Test
	public void java_post_failsIfExists() throws Exception
	{
		final ResponseType response1 = manage.kind("java-file")
				.postCreateResponseType(mainRule.resource(JAVA_EMPTY_CLASS_RES), TEXT_PLAIN);
		final ResponseType response2 = manage.kind("java-file")
				.postCreateResponseType(mainRule.resource(JAVA_EMPTY_CLASS_RES), TEXT_PLAIN);

		assertThat(response1, is(ResponseType.CREATED));
		assertThat(response2, is(ResponseType.CONFLICT));
	}
}
