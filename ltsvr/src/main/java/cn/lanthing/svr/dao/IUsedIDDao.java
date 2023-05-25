package cn.lanthing.svr.dao;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUsedIDDao {
    boolean isValidID(long deviceID);

    void addDeviceID(long deviceID);
}
