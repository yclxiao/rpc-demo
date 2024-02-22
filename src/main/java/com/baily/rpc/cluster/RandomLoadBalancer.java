package com.baily.rpc.cluster;

import com.baily.rpc.registry.RegistryInfo;

import java.util.List;
import java.util.Random;

/**
 * baily
 */
public class RandomLoadBalancer implements LoadBalancer {
    @Override
    public RegistryInfo choose(List<RegistryInfo> registryInfoList) throws Exception {
        Random random = new Random();
        int index = random.nextInt(registryInfoList.size());
        return registryInfoList.get(index);
    }
}
