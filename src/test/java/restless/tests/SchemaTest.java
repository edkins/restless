package restless.tests;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import restless.api.schema.model.ListSchemaItem;
import restless.client.api.ManagementPathSchema;
import restless.client.api.ResponseType;

public class SchemaTest extends BaseTest
{
	private static final String JSON_SCHEMA_MIME = "application/schema+json";
	private static final String JSON_SCHEMA_COFFEE_RES = "/json-schema/coffee";
	private static final String JSON_SCHEMA_INTLIST_RES = "/json-schema/nonempty_nonnegative_int_list";
	private ManagementPathSchema schema;

	@Before
	public void setup2()
	{
		schema = mainRule.actions().manage().jsonSchema("coffee");
	}

	@Test
	public void schema_canReadItBack() throws Exception
	{
		schema.data().putResource(JSON_SCHEMA_COFFEE_RES, JSON_SCHEMA_MIME);

		final String data = schema.data().getString(JSON_SCHEMA_MIME);

		JSONAssert.assertEquals(data, resource(JSON_SCHEMA_COFFEE_RES), true);
	}

	@Test
	public void schema_canList() throws Exception
	{
		schema.data().putResource(JSON_SCHEMA_COFFEE_RES, JSON_SCHEMA_MIME);

		final List<ListSchemaItem> list = manage.listJsonSchemas().childResources();

		assertThat(list.size(), is(1));
		assertThat(list.get(0).url(), is(schema.url()));
	}

	@Test
	public void schema_canPutTwice() throws Exception
	{
		schema.data().putResource(JSON_SCHEMA_COFFEE_RES, JSON_SCHEMA_MIME);
		schema.data().putResource(JSON_SCHEMA_INTLIST_RES, JSON_SCHEMA_MIME);
	}

	@Test
	public void schema_canDelete() throws Exception
	{
		schema.data().putResource(JSON_SCHEMA_COFFEE_RES, JSON_SCHEMA_MIME);

		assertThat(schema.data().getResponseTypeForContentType(JSON_SCHEMA_MIME), is(ResponseType.OK));

		schema.delete();

		assertThat(schema.data().getResponseTypeForContentType(JSON_SCHEMA_MIME), is(ResponseType.NOT_FOUND));
	}

	@Test
	public void schema_ifMissing_deleteReturnsNotFound() throws Exception
	{
		schema.data().putResource(JSON_SCHEMA_COFFEE_RES, JSON_SCHEMA_MIME);

		assertThat(schema.deleteResponseType(), is(ResponseType.NO_CONTENT));
	}

	@Test
	public void invalidSchema_rejected() throws Exception
	{
		final ResponseType responseType = schema.data()
				.putResourceResponseType("/json-schema/invalid", JSON_SCHEMA_MIME);

		assertEquals(ResponseType.BAD_REQUEST, responseType);
	}

	@Test
	public void schema_validData_allowed() throws Exception
	{
		schema.data().putResource(JSON_SCHEMA_COFFEE_RES, JSON_SCHEMA_MIME);
		final ResponseType responseType = schema.validate(resource("/json-example/coffee-valid"), "application/json");
		assertEquals(ResponseType.NO_CONTENT, responseType);
	}

	@Test
	public void schema_invalidData_notAllowed() throws Exception
	{
		schema.data().putResource(JSON_SCHEMA_COFFEE_RES, JSON_SCHEMA_MIME);
		final ResponseType responseType = schema.validate(resource("/json-example/coffee-invalid"), "application/json");
		assertEquals(ResponseType.BAD_REQUEST, responseType);
	}
}
