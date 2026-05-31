package com.duanruixin.pulse.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.duanruixin.pulse.app.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
    // 继承 BaseMapper 后,自动拥有:
    //   - insert / deleteById / updateById / selectById
    //   - selectList / selectPage / selectCount 等 20+ 个方法
    // 暂时不需要自定义方法
}