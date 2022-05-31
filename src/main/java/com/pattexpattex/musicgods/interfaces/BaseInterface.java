package com.pattexpattex.musicgods.interfaces;

import java.util.HashMap;
import java.util.Map;

public interface BaseInterface {
    Map<Class<? extends BaseInterface>, BaseInterface> subInterfaces = new HashMap<>();

    default void subInterfaceLoaded(BaseInterface baseInterface) {
        subInterfaces.put(baseInterface.getClass(), baseInterface);
    }

    @SuppressWarnings("unchecked")
    default <T extends BaseInterface> T getSubInterface(Class<T> klass) {
        return (T) subInterfaces.get(klass);
    }

    /**
     * Called when kicked from a guild.
     */
    default void destroy() {
    }

    /**
     * Called when shutting down.
     */
    default void shutdown() {
    }
}
