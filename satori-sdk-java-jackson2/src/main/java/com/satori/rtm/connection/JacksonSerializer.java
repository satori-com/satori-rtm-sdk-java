package com.satori.rtm.connection;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
import com.satori.rtm.model.AnyJson;
import com.satori.rtm.model.InvalidJsonException;
import com.satori.rtm.model.PduRaw;
import java.io.IOException;

/**
 * JSON serialization based on Jackson2 library.
 * <p>
 * For more information about this library, see <a href="http://wiki.fasterxml.com/JacksonRelease20">Jackson2</a>.
 */
public class JacksonSerializer implements Serializer {
  private final ObjectMapper mMapper;

  public JacksonSerializer() {
    this(init(new ObjectMapper()));
  }

  public JacksonSerializer(ObjectMapper mapper) {
    this.mMapper = mapper;
  }

  public static ObjectMapper init(ObjectMapper mapper) {
    mapper
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    SimpleModule anyJsonModule = new SimpleModule()
        .addSerializer(AnyJson.class, new AnyJsonSerializer())
        .addDeserializer(AnyJson.class, new AnyJsonDeserializer(mapper));

    mapper.registerModule(anyJsonModule);
    return mapper;
  }

  public ObjectMapper getMapper() {
    return mMapper;
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

  public static class AnyJsonSerializer extends StdSerializer<AnyJson> {
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

  public static class AnyJsonDeserializer extends StdDeserializer<AnyJson> {
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
