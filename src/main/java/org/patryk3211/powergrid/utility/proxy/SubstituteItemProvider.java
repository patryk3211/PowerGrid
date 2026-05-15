package org.patryk3211.powergrid.utility.proxy;

import net.minecraft.world.item.Item;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * This is extra spicy
 */
public class SubstituteItemProvider extends SubstituteProvider<Constructor<? extends Item>> {
    public static final SubstituteItemProvider INSTANCE = new SubstituteItemProvider();

    private SubstituteItemProvider() { }

    public <T extends Item> void register(Class<T> clazz, Constructor<T> constructor) {
        super.register(clazz, constructor);
    }

    public <T extends Item> void shadow(Class<T> clazz, Class<? extends T> overridingClazz) {
        register(clazz, (Constructor<? extends Item>) overridingClazz.getConstructors()[0]);
    }

    public <T extends Item> T invoke(Class<T> clazz, Object... params) {
        Constructor<? extends Item> constructor;
        if(isRegistered(clazz)) {
            constructor = getObject(clazz);
        } else {
            constructor = (Constructor<? extends Item>) clazz.getConstructors()[0];
        }
        try {
            return (T) constructor.newInstance(params);
        } catch(InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
