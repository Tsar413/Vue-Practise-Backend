package com.study.vuePractiseBackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

// 文件清理任务随业务事务提交，后台失败后重试。
@Entity
@Table(name = "repair_file_delete_task")
@TableName("repair_file_delete_task")
@Data
public class RepairFileDeleteTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "image_url", nullable = false, length = 500)
    @TableField("image_url")
    private String imageUrl;

    @Column(name = "create_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("create_time")
    private LocalDateTime createTime;
}
