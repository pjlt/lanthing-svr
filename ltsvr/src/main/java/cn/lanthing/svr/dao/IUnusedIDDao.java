package cn.lanthing.svr.dao;

import cn.lanthing.svr.entity.UnusedIDEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUnusedIDDao {
    UnusedIDEntity getNextDeviceID();

    void deleteDeviceID(long deviceID);
}
