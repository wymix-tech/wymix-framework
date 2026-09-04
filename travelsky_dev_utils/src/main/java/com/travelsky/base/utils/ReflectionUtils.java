package com.travelsky.base.utils;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ReflectionUtils {

    private ReflectionUtils() {

    }

    /**
     * return the clazz's super class generic parameter type.
     *
     * @param clazz class whose super class has generic parameter
     * @return generic class
     */
    public static Type getGenericParameter(Class<?> clazz) {

        while (clazz != null && (!(clazz.getGenericSuperclass() instanceof ParameterizedType))) {
            clazz = clazz.getSuperclass();
        }
        if (clazz != null) {
            return ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];
        } else {
            throw new IllegalArgumentException(
                    "Invalid argument, " + "argument's super class must specify type parameters");
        }
    }

    /**
     * <p>
     * 反射对象获取泛型, 如果泛型是List<****>类型，返回List，使用下面的getSuperClassGenericTypeCollectionGenType方法继续获取集合的泛型。
     * </p>
     *
     * @param clazz 对象
     * @param index 泛型所在位置
     * @return Class
     */
    public static Class getSuperClassGenericType(final Class clazz, final int index) {
        Type genType = clazz.getGenericSuperclass();
        Class superClass = clazz;
        while (!(genType instanceof ParameterizedType)) {
            if (superClass == Object.class) {
                return Object.class;
            }
            superClass = superClass.getSuperclass();
            genType = superClass.getGenericSuperclass();

        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        if (index >= params.length || index < 0) {
            return Object.class;
        }
        if (!(params[index] instanceof Class) && params[index] instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) params[index];
            return (Class) pType.getRawType();
        }
        return (Class) params[index];
    }

    /**
     * <p>
     * 反射对象获取泛型
     * </p>
     *
     * @param clazz              对象
     * @param index              泛型所在位置
     * @param collectionGenindex 泛型中的集合的泛型的index位置
     * @return Class
     */
    public static Class getSuperClassGenericTypeCollectionGenType(final Class clazz,
                                                                  final int index,
                                                                  final int collectionGenindex) {
        Type genType = clazz.getGenericSuperclass();
        if (!(genType instanceof ParameterizedType)) {
            return Object.class;
        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        if (index >= params.length || index < 0) {
            return Object.class;
        }
        if (!(params[index] instanceof Class) && params[index] instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) params[index];
            return (Class) pType.getActualTypeArguments()[collectionGenindex];
        }
        return (Class) params[index];
    }

    /**
     * <p>
     * 反射对象获取泛型,通过泛型获取对象实体.
     * </p>
     *
     * @param clazz 对象
     * @param index 泛型所在位置
     * @return Class
     */
    public static Object newInstance(final Class clazz, final int index) {
        try {
            return getSuperClassGenericType(clazz, index).newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("newInstance error", e);
        }
    }

    /**
     * 获取emun的值
     *
     * @param enumClass Class
     * @param value     value
     * @return E
     */
    public static <E> E enumValueOf(Class<E> enumClass, Object value) {
        E[] es = enumClass.getEnumConstants();
        return Arrays.stream(es).filter((e) -> value.equals(getEnumValue(e))).findAny()
                .orElse(null);
    }

    /**
     * 获取enum中的value的值
     *
     * @param enumObj Object
     * @return Object
     */
    public static Object getEnumValue(Object enumObj) {
        try {
            Method[] methods = enumObj.getClass().getMethods();

            if (methods != null) {
                Method method = Arrays.stream(methods).filter(m -> "getValue".equals(m.getName()))
                        .findAny().orElse(null);
                if (method != null) {
                    return method.invoke(enumObj);
                }
            }

            Field field = enumObj.getClass().getDeclaredField("value");
            if (field != null) {
                field.setAccessible(true);
                return field.get(enumObj);
            }

            return null;

        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 获取当前对象的所有字段，包括父类字段.
     *
     * @param obj Object
     * @return List<Field>
     */
    public static List<Field> getAllField(Object obj) {
        List<Field> fieldList = new ArrayList<>();
        Class tempClass = obj.getClass();
        while (tempClass != null) {// 当父类为null的时候说明到达了最上层的父类(Object类).
            fieldList.addAll(Arrays.asList(tempClass.getDeclaredFields()));
            tempClass = tempClass.getSuperclass(); // 得到父类,然后赋给自己
        }
        return fieldList;
    }

    /**
     * 获取当前对象的所有字段，包括父类字段.
     *
     * @param clazz clazz
     * @return List<Field>
     */
    public static List<Field> getAllFieldByClazz(Class clazz) {
        List<Field> fieldList = new ArrayList<>();
        Class tempClass = clazz;
        while (tempClass != null) {// 当父类为null的时候说明到达了最上层的父类(Object类).
            fieldList.addAll(Arrays.asList(tempClass.getDeclaredFields()));
            tempClass = tempClass.getSuperclass(); // 得到父类,然后赋给自己
        }
        return fieldList;
    }

    /**
     * 获取当前对象的所有字段，包括父类字段.
     *
     * @param obj  Object
     * @param name String
     * @return Field
     */
    public static Field findFieldForAll(Object obj, String name) {
        List<Field> fieldList = getAllField(obj);
        for (Field f : fieldList) {
            if (f.getName().equals(name)) {
                return f;
            }
        }
        return null;
    }

    /**
     * 获取字段名对应的值
     *
     * @param obj  Object
     * @param name name
     * @return value
     */
    public static Object getFieldValueForAll(Object obj, String name) {
        Field f = findFieldForAll(obj, name);
        f.setAccessible(true);
        try {
            return f.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> getClassName(String packageName, boolean childPackage) {
        List<String> fileNames = null;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String packagePath = packageName.replace(".", "/");
        URL url = loader.getResource(packagePath);
        if (url != null) {
            String type = url.getProtocol();
            if (type.equals("file")) {
                fileNames = getClassNameByFile(url.getPath(), null, childPackage);
            } else if (type.equals("jar")) {
                fileNames = getClassNameByJar(url.getPath(), childPackage);
            }
        } else {
            fileNames = getClassNameByJars(((URLClassLoader) loader).getURLs(), packagePath, childPackage);
        }
        return fileNames;
    }

    private static List<String> getClassNameByFile(String filePath, List<String> className, boolean childPackage) {
        List<String> myClassName = new ArrayList<String>();
        File file = new File(filePath);
        File[] childFiles = file.listFiles();
        for (File childFile : childFiles) {
            if (childFile.isDirectory()) {
                if (childPackage) {
                    myClassName.addAll(getClassNameByFile(childFile.getPath(), myClassName, childPackage));
                }
            } else {
                String childFilePath = childFile.getPath();
                if (childFilePath.endsWith(".class")) {
                    childFilePath = childFilePath.substring(childFilePath.indexOf("\\classes") + 9, childFilePath.lastIndexOf("."));
                    childFilePath = childFilePath.replace("\\", ".");
                    myClassName.add(childFilePath);
                }
            }
        }

        return myClassName;
    }

    private static List<String> getClassNameByJar(String jarPath, boolean childPackage) {
        List<String> myClassName = new ArrayList<String>();
        String[] jarInfo = jarPath.split("!");
        String jarFilePath = jarInfo[0].substring(jarInfo[0].indexOf("/"));
        String packagePath = jarInfo[1].substring(1);
        try {
            JarFile jarFile = new JarFile(jarFilePath);
            Enumeration<JarEntry> entrys = jarFile.entries();
            while (entrys.hasMoreElements()) {
                JarEntry jarEntry = entrys.nextElement();
                String entryName = jarEntry.getName();
                if (entryName.endsWith(".class")) {
                    if (childPackage) {
                        if (entryName.startsWith(packagePath)) {
                            entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
                            myClassName.add(entryName);
                        }
                    } else {
                        int index = entryName.lastIndexOf("/");
                        String myPackagePath;
                        if (index != -1) {
                            myPackagePath = entryName.substring(0, index);
                        } else {
                            myPackagePath = entryName;
                        }
                        if (myPackagePath.equals(packagePath)) {
                            entryName = entryName.replace("/", ".").substring(0, entryName.lastIndexOf("."));
                            myClassName.add(entryName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return myClassName;
    }

    private static List<String> getClassNameByJars(URL[] urls, String packagePath, boolean childPackage) {
        List<String> myClassName = new ArrayList<String>();
        if (urls != null) {
            for (int i = 0; i < urls.length; i++) {
                URL url = urls[i];
                String urlPath = url.getPath();
                // 不必搜索classes文件夹
                if (urlPath.endsWith("classes/")) {
                    continue;
                }
                String jarPath = urlPath + "!/" + packagePath;
                myClassName.addAll(getClassNameByJar(jarPath, childPackage));
            }
        }
        return myClassName;
    }


    public static Field[] findFieldWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return getAllFieldByClazz(clazz).stream().filter(field -> field.isAnnotationPresent(annotationClass)).toArray(Field[]::new);
    }

}
