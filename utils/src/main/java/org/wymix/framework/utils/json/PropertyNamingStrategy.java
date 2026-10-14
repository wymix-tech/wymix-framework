package org.wymix.framework.utils.json;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import org.wymix.framework.utils.StringUtils;

public class PropertyNamingStrategy extends PropertyNamingStrategies.NamingBase {

    private static final long serialVersionUID = 1L;

    @Override
    public String translate(String propertyName) {
        return propertyName;
    }

    @Override
    public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method,
                                      String defaultName) {
        String methodName = method.getName();
        if (methodName.startsWith("get")) {
            methodName = methodName.substring(3);
        }
        if (methodName.startsWith("is")) {
            methodName = methodName.substring(2);
        }
        return StringUtils.firstToLowerCase(methodName);
    }

    @Override
    public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method,
                                      String defaultName) {
        String methodName = method.getName();
        if (methodName.startsWith("set")) {
            methodName = methodName.substring(3);
        }
        return StringUtils.firstToLowerCase(methodName);
    }

}
