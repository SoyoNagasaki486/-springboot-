package edu.lab.service;

import edu.lab.config.LabGuardProperties;
import edu.lab.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/** 准入校验、开放状态检查、冲突检测和预约审核业务。 */
@Service
public class ReservationService {
    private static final Set<String> AUDIT_STATUS = Set.of("已通过", "驳回");
    private static final Set<String> TIME_SLOTS = Set.of("09:00-12:00", "14:00-17:00", "18:00-21:00");
    private final ReservationRepository reservationRepository;
    private final LabGuardProperties properties;

    public ReservationService(ReservationRepository reservationRepository, LabGuardProperties properties) {
        this.reservationRepository = reservationRepository;
        this.properties = properties;
    }

    @Transactional
    public void create(long userId, long labId, LocalDate date, String slot, String purpose) {
        reservationRepository.lockUser(userId);
        String labStatus = reservationRepository.findLabStatus(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实验室不存在"));
        if (!"开放".equals(labStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该实验室当前未开放预约");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预约日期不能早于今天");
        }
        if (date.isAfter(LocalDate.now().plusDays(properties.getReservation().getMaxAdvanceDays()))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "最多只能提前 " + properties.getReservation().getMaxAdvanceDays() + " 天预约");
        }
        if (!TIME_SLOTS.contains(slot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预约时段不在开放的标准时段内");
        }
        if (purpose == null || purpose.trim().length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "使用目的至少填写 4 个字符");
        }
        if (!reservationRepository.hasQualifiedAccess(userId, labId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "尚未取得该实验室的有效准入资格");
        }
        if (reservationRepository.hasUserConflict(userId, date, slot)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "同一时间只能预约一个实验室");
        }
        if (reservationRepository.hasConflict(labId, date, slot)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该时段已被预约");
        }
        reservationRepository.save(userId, labId, date, slot, purpose.trim());
    }

    @Transactional
    public void audit(long id, String status, String reviewer) {
        if (!AUDIT_STATUS.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "审核状态只能为已通过或驳回");
        }
        Map<String, Object> reservation = reservationRepository.findForAudit(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "预约记录不存在"));
        if (!"待审核".equals(String.valueOf(reservation.get("status")))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该预约已完成审核，请刷新后重试");
        }
        if (LocalDate.parse(String.valueOf(reservation.get("reserve_date"))).isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已过期的预约不能再审核");
        }
        if (reservationRepository.audit(id, status, reviewer.trim()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "预约状态已变化，请刷新后重试");
        }
    }

    @Transactional
    public void cancel(long id, long userId) {
        Map<String, Object> reservation = reservationRepository.findForCancellation(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "预约记录不存在"));
        if (((Number) reservation.get("user_id")).longValue() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能取消本人提交的预约");
        }
        String status = String.valueOf(reservation.get("status"));
        if (!Set.of("待审核", "已通过").contains(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前预约状态不能取消");
        }
        LocalDate reserveDate = LocalDate.parse(String.valueOf(reservation.get("reserve_date")));
        if (reserveDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已过期的预约不能取消");
        }
        if (reservationRepository.cancel(id) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "预约状态已变化，请刷新后重试");
        }
    }
}
