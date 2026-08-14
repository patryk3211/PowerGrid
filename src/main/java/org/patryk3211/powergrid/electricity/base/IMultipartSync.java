package org.patryk3211.powergrid.electricity.base;

import java.util.function.Consumer;

public interface IMultipartSync {
    void forSync(Consumer<ISynchronizedElement> consumer);
}
