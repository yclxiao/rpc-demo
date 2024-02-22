package com.baily.rpc.config;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * baily
 */
@Data
@AllArgsConstructor
public class ServiceConfig {
    private Class type;
    private Object instance;
}
