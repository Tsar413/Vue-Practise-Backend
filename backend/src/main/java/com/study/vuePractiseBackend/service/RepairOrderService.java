package com.study.vuePractiseBackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.vuePractiseBackend.dto.*;
import com.study.vuePractiseBackend.entity.*;
import com.study.vuePractiseBackend.vo.RepairOrderDetailVO;
import java.util.List;

public interface RepairOrderService extends IService<RepairOrder> {
    List<RepairOrder> getOrders(Long workspaceId, Long operatorId, Integer status,
                              String campus, String deviceType, String keyword, String scope);
    RepairOrderDetailVO getOrderDetail(Long workspaceId, Long orderId, Long operatorId);
    RepairOrder createOrder(Long workspaceId, RepairOrderCreateDTO dto);
    RepairOrder cancelOrder(Long workspaceId, Long orderId, Long operatorId);
    RepairOrder assignOrder(Long workspaceId, Long orderId, RepairAssignDTO dto);
    RepairOrder acceptOrder(Long workspaceId, Long orderId, Long operatorId);
    RepairProcessRecord addProcessRecord(Long workspaceId, Long orderId, RepairProcessDTO dto);
    RepairOrder submitOrder(Long workspaceId, Long orderId, RepairSubmitDTO dto);
    RepairOrder confirmOrder(Long workspaceId, Long orderId, Long operatorId);
    RepairOrder returnOrder(Long workspaceId, Long orderId, RepairReturnDTO dto);
    RepairEvaluation evaluateOrder(Long workspaceId, Long orderId, RepairEvaluationDTO dto);
    RepairEvaluation getEvaluation(Long workspaceId, Long orderId, Long operatorId);
    List<RepairProcessRecord> getProcessRecords(Long workspaceId, Long orderId, Long operatorId);
}
