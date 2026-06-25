package com.drivingschool.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.drivingschool.student.entity.StudentAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学员档案附件Mapper接口
 * 对应实体类：{@link StudentAttachment}（student_attachment表）
 * 提供学员附件的数据库CRUD操作，继承MyBatis-Plus的BaseMapper获得通用增删改查能力
 * 基础的增删改查方法（如按学员ID查询附件列表、删除附件等）已由BaseMapper提供
 */
@Mapper
public interface StudentAttachmentMapper extends BaseMapper<StudentAttachment> {
}
