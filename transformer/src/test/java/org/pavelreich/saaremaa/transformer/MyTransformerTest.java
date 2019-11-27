package org.pavelreich.saaremaa.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.IterableCodec;
import org.bson.codecs.IterableCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonWriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.pavelreich.saaremaa.transformer.MyTransformer.User;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class MyTransformerTest {

	@Mock
	PrintWriter writer1;

	PrintWriter writer = Mockito.mock(PrintWriter.class);

	MyTransformer myTransformer = new MyTransformer();

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testTData() {
		org.junit.Assert.assertEquals("ban", myTransformer.getName(new User("ban")));
	}

	@Test
	public void testHamcrestAssert() {
		String result = myTransformer.transform("ban");
		assertThat(result, anyOf(is("ban"), containsString("ban")));
	}

	@Test
	public void testAssert() {
		String result = myTransformer.transform("ban");
		org.junit.Assert.assertEquals("banban", result);

	}

	@Test
	public void testThrowingMock() {
		doThrow(new RuntimeException("a")).when(writer1).println(eq("banana"));
		try {
			myTransformer.print(writer1);
			fail("x");
		} catch (RuntimeException e) {

		}
	}

	@Test
	public void serialise() {
		List<Document> docs = Arrays.asList("a", "b", "c").stream().map(x -> new Document().append(x, x))
				.collect(Collectors.toList());
		System.out.println(docs);
		BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap();
		CodecRegistry registry = CodecRegistries.fromProviders(new ValueCodecProvider(), new DocumentCodecProvider(),
				new BsonValueCodecProvider(), new IterableCodecProvider());
		IterableCodec codec = new IterableCodec(registry, bsonTypeClassMap);
		StringWriter sw = new StringWriter();
		BsonWriter bsonWriter = new JsonWriter(sw);
		bsonWriter.writeStartDocument();
		bsonWriter.writeStartArray("results");
		codec.encode(bsonWriter, docs, EncoderContext.builder().build());
		bsonWriter.writeEndArray();
		bsonWriter.writeEndDocument();
		System.out.println(sw);
	}

	@Test
	public void testMock() {
		myTransformer.print(writer);
		Mockito.verify(writer).println("banana");
		myTransformer.print(writer);
		Mockito.verify(writer, Mockito.times(2)).println("banana");
	}
	
	@Test
	public void testThreading() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().submit(
				() -> {
					myTransformer.print(writer);
					latch.countDown();
				}
		);
		Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
		Mockito.verify(writer).println("banana");

	}

}
