package com.baily.rpc.registry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * baily
 */
@Data
@AllArgsConstructor
@ToString
public class RegistryInfo {
    private String hostName;
    private String ip;
    private int port;
}
