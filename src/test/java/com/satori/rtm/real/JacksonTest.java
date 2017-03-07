package com.satori.rtm.real;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.satori.rtm.Ack;
import com.satori.rtm.RtmClient;
import com.satori.rtm.SubscriptionAdapter;
import com.satori.rtm.SubscriptionMode;
import com.satori.rtm.connection.Serializer;
import com.satori.rtm.model.AnyJson;
import com.satori.rtm.model.InvalidJsonException;
import com.satori.rtm.model.PduRaw;
import com.satori.rtm.model.SubscribeReply;
import com.satori.rtm.model.SubscribeRequest;
import com.satori.rtm.model.SubscriptionData;
import org.junit.Test;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class JacksonTest extends AbstractRealTest {
  @Test
  public void publishComplexObjectWithJackson() throws InterruptedException, ExecutionException {
    RtmClient client = clientBuilder()
        .setJsonSerializer(new JacksonSerializer())
        .build();
    client.start();

    client.createSubscription(channel, SubscriptionMode.SIMPLE,
        new SubscriptionAdapter() {
          @Override
          public void onEnterSubscribed(SubscribeRequest request, SubscribeReply reply) {
            dispatcher.add("on-enter-subscribed");
          }

          @Override
          public void onSubscriptionData(SubscriptionData data) {
            for (MyCustomBody message : data.getMessagesAsType(MyCustomBody.class)) {
              dispatcher.add(String.format("%s-%s", message.fieldA, message.fieldB));
            }
          }
        });
    assertThat(getEvent(), equalTo("on-enter-subscribed"));
    awaitFuture(client.publish(channel, new MyCustomBody("valueA", "valueB"), Ack.YES));

    assertThat(getEvent(), equalTo("valueA-valueB"));

    client.stop();
  }

  private static class MyCustomBody {
    String fieldA;
    String fieldB;

    public MyCustomBody() { }

    MyCustomBody(String fieldA, String fieldB) {
      this.fieldA = fieldA;
      this.fieldB = fieldB;
    }
  }

  static class JacksonSerializer implements Serializer {
    ObjectMapper mMapper;

    {
      mMapper = new ObjectMapper()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
          .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
          .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);

      SimpleModule anyJsonModule = new SimpleModule()
          .addSerializer(AnyJson.class, new AnyJsonSerializer())
          .addDeserializer(AnyJson.class, new AnyJsonDeserializer(mMapper));

      mMapper.registerModule(anyJsonModule);
    }

    @Override
    public PduRaw parsePdu(String json) throws InvalidJsonException {
      try {
        return mMapper.readValue(json, PduRaw.class);
      } catch (IOException e) {
        throw new InvalidJsonException(json, e);
      }
    }

    @Override
    public String toJson(Object obj) {
      try {
        return mMapper.writeValueAsString(obj);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }

    static class AnyJsonSerializer extends StdSerializer<AnyJson> {
      AnyJsonSerializer() {
        this(null);
      }

      AnyJsonSerializer(Class<AnyJson> vc) {
        super(vc);
      }

      @Override
      public void serialize(AnyJson value, JsonGenerator gen, SerializerProvider provider)
          throws IOException {
        gen.writeObject(value.convertToType(JsonNode.class));
      }
    }

    static class JsonNodeWrapper implements AnyJson {
      private final JsonNode node;
      private final ObjectMapper mapper;

      JsonNodeWrapper(ObjectMapper mapper, JsonNode node) {
        this.node = node;
        this.mapper = mapper;
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T> T convertToType(Class<T> clazz) {
        T typedBody;
        if (clazz.isAssignableFrom(node.getClass())) {
          typedBody = (T) node;
        } else {
          try {
            JavaType javaType = mapper.constructType(clazz);
            return mapper.readValue(mapper.treeAsTokens(node), javaType);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        return typedBody;
      }

      @Override
      public String toString() {
        return this.node.toString();
      }
    }

    static class AnyJsonDeserializer extends StdDeserializer<AnyJson> {
      private final ObjectMapper mapper;

      AnyJsonDeserializer(ObjectMapper mapper) {
        this(mapper, null);
      }

      AnyJsonDeserializer(ObjectMapper mapper, Class<?> vc) {
        super(vc);
        this.mapper = mapper;
      }

      @Override
      public AnyJson deserialize(JsonParser jp, DeserializationContext ctxt)
          throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        return new JsonNodeWrapper(mapper, node);
      }
    }
  }
}
