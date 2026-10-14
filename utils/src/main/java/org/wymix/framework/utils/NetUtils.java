package org.wymix.framework.utils;

import org.springframework.util.CollectionUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.function.Predicate;

public class NetUtils {

    public static String getLocalhostStr() {
        InetAddress localhost = getLocalhost();
        if (null != localhost) {
            return localhost.getHostAddress();
        }
        return null;
    }

    private static InetAddress getLocalhost() {
        final LinkedHashSet<InetAddress> localAddressList = localAddressList(address -> (!address.isLoopbackAddress()) && address instanceof Inet4Address);

        if (!CollectionUtils.isEmpty(localAddressList)) {
            InetAddress ret = null;
            for (InetAddress inetAddress : localAddressList) {
                if (!inetAddress.isSiteLocalAddress()) {
                    return inetAddress;
                } else if (null == ret) {
                    ret = inetAddress;
                }
            }

            return ret;
        }

        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            // ignore
        }
        return null;
    }


    public static LinkedHashSet<InetAddress> localAddressList(Predicate<InetAddress> predicate) {
        return localAddressList(null, predicate);
    }

    public static LinkedHashSet<InetAddress> localAddressList(Predicate<NetworkInterface> networkInterfacePredicate, Predicate<InetAddress> inetAddressPredicate) {
        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        if (null == networkInterfaces) {
            throw new RuntimeException("Get net work interface error!");
        }

        final LinkedHashSet<InetAddress> ipSet = new LinkedHashSet<>();

        while (networkInterfaces.hasMoreElements()) {
            final NetworkInterface networkInterface = networkInterfaces.nextElement();
            if (networkInterfacePredicate != null && !networkInterfacePredicate.test(networkInterface)) {
                continue;
            }
            final Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                final InetAddress inetAddress = inetAddresses.nextElement();
                if (inetAddress != null && (null == inetAddressPredicate || inetAddressPredicate.test(inetAddress))) {
                    ipSet.add(inetAddress);
                }
            }
        }

        return ipSet;
    }

}
