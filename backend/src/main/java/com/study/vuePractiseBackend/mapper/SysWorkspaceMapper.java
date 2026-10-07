package com.study.vuePractiseBackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.vuePractiseBackend.entity.SysWorkspace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysWorkspaceMapper extends BaseMapper<SysWorkspace> {
    @Select("SELECT * FROM sys_workspace WHERE student_id = #{studentId} FOR UPDATE ")
    SysWorkspace selectByStudentIdForUpdate(@Param("studentId") String studentId);
}
