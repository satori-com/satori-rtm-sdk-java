package com.satori.rtm.connection;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.satori.rtm.model.AnyJson;
import com.satori.rtm.model.InvalidJsonException;
import com.satori.rtm.model.PduRaw;
import com.satori.rtm.model.PublishRequest;
import com.satori.rtm.model.WriteRequest;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * JSON serialization based on google-gson library.
 * <p>
 * For more information about this library, see <a href="https://github.com/google/gson">google-gson</a>.
 */
public class GsonSerializer implements Serializer {
  private final Gson mGson;

  public GsonSerializer() {
    this(new GsonBuilder());
  }

  public GsonSerializer(GsonBuilder builder) {
    mGson = builder
        .registerTypeAdapter(AnyJson.class, new AnyJsonAdapter())
        .registerTypeAdapterFactory(SerializeNullsAdapter.FACTORY)
        .create();
  }

  @Override
  public String toJson(Object obj) {
    return mGson.toJson(obj);
  }

  @Override
  public PduRaw parsePdu(String json) throws InvalidJsonException {
    try {
      return mGson.fromJson(json, PduRaw.class);
    } catch (Exception ex) {
      throw new InvalidJsonException(json, ex);
    }
  }

  public PduRaw parsePdu(JsonElement json) throws InvalidJsonException {
    return mGson.fromJson(json, PduRaw.class);
  }

  private static class JsonElementWrapper implements AnyJson {
    private final JsonElement json;
    private final JsonDeserializationContext context;

    JsonElementWrapper(JsonElement json, JsonDeserializationContext context) {
      this.json = json;
      this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertToType(Class<T> clazz) {
      T typedBody;
      if (clazz.isAssignableFrom(json.getClass())) {
        typedBody = (T) json;
      } else {
        typedBody = context.deserialize(json, clazz);
      }
      return typedBody;
    }

    @Override
    public String toString() {
      return json.toString();
    }
  }

  private static class AnyJsonAdapter
      implements JsonDeserializer<AnyJson>, JsonSerializer<AnyJson> {
    @Override
    public AnyJson deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
        throws JsonParseException {
      return new JsonElementWrapper(json, context);
    }

    @Override
    public JsonElement serialize(AnyJson src, Type typeOfSrc, JsonSerializationContext context) {
      return src.convertToType(JsonElement.class);
    }
  }

  private static class SerializeNullsAdapter<T> extends TypeAdapter<T> {
    static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
      @Override
      public <K> TypeAdapter<K> create(Gson gson, TypeToken<K> typeToken) {
        Class<? super K> rawType = typeToken.getRawType();
        if ((rawType == PublishRequest.class) || (rawType == WriteRequest.class)) {
          TypeAdapter<K> delegate = gson.getDelegateAdapter(this, typeToken);
          return new SerializeNullsAdapter<K>(gson, delegate);
        }
        return null;
      }
    };
    private final Gson mGson;
    private final TypeAdapter<T> mDelegate;

    private SerializeNullsAdapter(Gson gson, TypeAdapter<T> delegate) {
      this.mGson = gson;
      this.mDelegate = delegate;
    }

    private void writeFieldValue(Field field, T obj, JsonWriter out) throws IOException {
      boolean serializeNulls = out.getSerializeNulls();
      try {
        Object val = field.get(obj);
        if (null == val) {
          out.setSerializeNulls(true);
          out.nullValue();
        } else {
          mGson.toJson(val, val.getClass(), out);
        }
      } catch (IllegalAccessException e) {
        throw new AssertionError(e);
      } finally {
        out.setSerializeNulls(serializeNulls);
      }
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
      FieldNamingStrategy fieldNamingStrategy = mGson.fieldNamingStrategy();
      out.beginObject();
      for (Field field : value.getClass().getDeclaredFields()) {
        field.setAccessible(true);
        String name = fieldNamingStrategy.translateName(field);
        out.name(name);
        writeFieldValue(field, value, out);
      }
      out.endObject();
    }

    @Override
    public T read(JsonReader in) throws IOException {
      return mDelegate.read(in);
    }
  }
}
