package org.wymix.framework.utils.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

import java.io.IOException;
import java.math.BigDecimal;

public class NumberSerializer extends com.fasterxml.jackson.databind.ser.std.NumberSerializer {

    private static final long serialVersionUID = 1L;

    public NumberSerializer(Class<? extends Number> rawType) {
        super(rawType);
    }

    @Override
    public void serialize(Number value, JsonGenerator g, SerializerProvider provider)
            throws IOException {
        if (value != null
                && new BigDecimal(value.toString()).compareTo(new BigDecimal("100000000000000")) > 0) {
            g.writeString(value.toString());
        } else {
            super.serialize(value, g, provider);
        }
    }

    /**
     * Default implementation will write type prefix, call regular serialization method (since
     * assumption is that value itself does not need JSON Array or Object start/end markers), and
     * then write type suffix. This should work for most cases; some sub-classes may want to change
     * this behavior.
     */
    @Override
    public void serializeWithType(Number value, JsonGenerator g, SerializerProvider provider,
                                  TypeSerializer typeSer)
            throws IOException {
        if (value != null
                && new BigDecimal(value.toString()).compareTo(new BigDecimal("100000000000000")) > 0) {
            WritableTypeId typeIdDef = typeSer.writeTypePrefix(g,
                    typeSer.typeId(value, JsonToken.VALUE_STRING));
            serialize(value, g, provider);
            typeSer.writeTypeSuffix(g, typeIdDef);
        } else {
            super.serializeWithType(value, g, provider, typeSer);
        }
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
            throws JsonMappingException {
        visitStringFormat(visitor, typeHint);
    }
}
