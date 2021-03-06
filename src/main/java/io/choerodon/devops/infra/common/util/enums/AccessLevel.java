package io.choerodon.devops.infra.common.util.enums;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by Zenger on 2017/11/14.
 */
public enum AccessLevel {
    NONE(0),
    GUEST(10),
    REPORTER(20),
    DEVELOPER(30),
    MASTER(40),
    OWNER(50);

    private static HashMap<Integer, AccessLevel> valuesMap = new HashMap<>(6);

    static {
        AccessLevel[] var0 = values();

        for (AccessLevel accessLevel : var0) {
            valuesMap.put(accessLevel.value, accessLevel);
        }

    }

    public final Integer value;

    AccessLevel(int value) {
        this.value = value;
    }

    @JsonCreator
    public static AccessLevel forValue(Integer value) {
        return valuesMap.get(value);
    }

    /**
     * 根据string类型返回枚举类型
     *
     * @param value String
     */
    public static AccessLevel forString(String value) {
        switch (value) {
            case "GITLAB.OWNER":
                return AccessLevel.OWNER;
            case "GITLAB.MASTER":
                return AccessLevel.MASTER;
            case "GITLAB.DEVELOPER":
                return AccessLevel.DEVELOPER;
            case "GITLAB.REPORTER":
                return AccessLevel.REPORTER;
            case "GITLAB.GUEST":
                return AccessLevel.GUEST;
            case "GITLAB.NONE":
                return AccessLevel.NONE;
            default:
                return AccessLevel.NONE;
        }
    }

    @JsonValue
    public Integer toValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }
}
