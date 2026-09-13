package org.joupen.utils.config;

import java.util.Map;

public interface ConfigConverter {
    /**
     * @return true, если конфиг был изменён (нуждается в сохранении)
     */
    boolean convert(Map<String, Object> config);
}
