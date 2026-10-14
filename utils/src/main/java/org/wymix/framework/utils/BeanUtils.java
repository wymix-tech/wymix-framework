package org.wymix.framework.utils;

import org.springframework.beans.BeansException;

import java.util.List;


public class BeanUtils {

    public static <T> T copyProperties(Object source, T target) throws BeansException {
        org.springframework.beans.BeanUtils.copyProperties(source, target);
        return target;
    }

    public static <T> T copyProperties(Object source, T target, String... ignoreProperties) throws BeansException {
        org.springframework.beans.BeanUtils.copyProperties(source, target, ignoreProperties);
        return target;
    }

    public static <T> List<T> copyListProperties(List<Object> source, List<T> target, Class<T> targetClass) {
        if (org.springframework.beans.BeanUtils.isSimpleProperty(targetClass)) {
            copySimpleProperties(source, target);
            return target;
        }
        try {
            if (source.size() == target.size()) {
                for (int i = 0; i < source.size(); i++) {
                    copyProperties(source.get(i), target.get(i));
                }

            }

            if (source.size() > target.size()) {
                int targetSize = target.size();
                for (int i = 0; i < targetSize; i++) {
                    copyProperties(source.get(i), target.get(i));
                }
                for (int j = targetSize; j < source.size(); j++) {
                    T targetObject = targetClass.newInstance();
                    copyProperties(source.get(j), targetObject);
                    target.add(targetObject);
                }
            }

            return target;
        } catch (Exception e) {
            throw new RuntimeException("Could not copy list from source to target", e);
        }
    }

    private static <T> void copySimpleProperties(List<Object> source, List<T> target) {
        target.clear();
        for (Object o : source) {
            target.add((T) o);
        }


    }

}
