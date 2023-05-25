package cn.lanthing.svr.service.impl;

import cn.lanthing.svr.dao.IUnusedIDDao;
import cn.lanthing.svr.dao.IUsedIDDao;
import cn.lanthing.svr.entity.UnusedIDEntity;
import cn.lanthing.svr.service.DeviceIDService;
import cn.lanthing.utils.AutoLock;
import cn.lanthing.utils.AutoReentrantLock;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class DeviceIDServiceImpl implements DeviceIDService {

    @Resource
    private IUnusedIDDao unusedIDDao;

    @Resource
    private IUsedIDDao usedIDDao;

    private final AutoReentrantLock lock = new AutoReentrantLock();

    @Override
    public Long allocateDeviceID() {
        long deviceID;
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            UnusedIDEntity entity = unusedIDDao.getNextDeviceID();
            //log.info("UnusedIDEntity {}", entity);
            deviceID = entity.getDeviceID();
            unusedIDDao.deleteDeviceID(deviceID);
            usedIDDao.addDeviceID(deviceID);
        }
        log.info("Allocate device id '{}'", deviceID);
        return deviceID;
    }

    @Override
    public boolean isValidDeviceID(long deviceID) {
        try (AutoLock lockGuard = this.lock.lockAsResource()) {
            return usedIDDao.isValidID(deviceID);
        }
    }

}
