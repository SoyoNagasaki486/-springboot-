package edu.lab.service;

import edu.lab.config.LabGuardProperties;
import edu.lab.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    private static final long USER_ID = 1L;
    private static final long LAB_ID = 2L;
    private static final String TIME_SLOT = "09:00-12:00";
    private static final String PURPOSE = "课程实验";
    @Mock
    private ReservationRepository reservationRepository;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, new LabGuardProperties());
    }

    @Test
    void shouldCreateReservationWhenRulesPass() {
        LocalDate date = LocalDate.now().plusDays(1);
        allowOpenQualifiedLab();

        reservationService.create(USER_ID, LAB_ID, date, TIME_SLOT, PURPOSE);

        verify(reservationRepository).save(USER_ID, LAB_ID, date, TIME_SLOT, PURPOSE);
    }

    @Test
    void shouldRejectUserWithoutQualificationForTargetLab() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(reservationRepository.findLabStatus(LAB_ID)).thenReturn(Optional.of("开放"));
        when(reservationRepository.hasQualifiedAccess(USER_ID, LAB_ID)).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> reservationService.create(USER_ID, LAB_ID, date, TIME_SLOT, PURPOSE));
        verify(reservationRepository, never()).save(USER_ID, LAB_ID, date, TIME_SLOT, PURPOSE);
    }

    @Test
    void shouldRejectClosedLab() {
        when(reservationRepository.findLabStatus(LAB_ID)).thenReturn(Optional.of("维护中"));

        assertThrows(ResponseStatusException.class,
                () -> reservationService.create(
                        USER_ID, LAB_ID, LocalDate.now().plusDays(1), TIME_SLOT, PURPOSE));
    }

    @Test
    void shouldRejectConflictingLabTimeSlot() {
        LocalDate date = LocalDate.now().plusDays(1);
        allowOpenQualifiedLab();
        when(reservationRepository.hasConflict(LAB_ID, date, TIME_SLOT)).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> reservationService.create(USER_ID, LAB_ID, date, TIME_SLOT, PURPOSE));
    }

    @Test
    void shouldRejectUserBookingTwoLabsAtSameTime() {
        LocalDate date = LocalDate.now().plusDays(1);
        allowOpenQualifiedLab();
        when(reservationRepository.hasUserConflict(USER_ID, date, TIME_SLOT)).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> reservationService.create(USER_ID, LAB_ID, date, TIME_SLOT, PURPOSE));
        verify(reservationRepository, never()).save(USER_ID, LAB_ID, date, TIME_SLOT, PURPOSE);
    }

    @Test
    void shouldRejectReservationBeyondAdvanceWindow() {
        when(reservationRepository.findLabStatus(LAB_ID)).thenReturn(Optional.of("开放"));

        assertThrows(ResponseStatusException.class,
                () -> reservationService.create(
                        USER_ID, LAB_ID, LocalDate.now().plusDays(61), TIME_SLOT, PURPOSE));
    }

    @Test
    void shouldRejectNonStandardTimeSlot() {
        when(reservationRepository.findLabStatus(LAB_ID)).thenReturn(Optional.of("开放"));

        assertThrows(ResponseStatusException.class,
                () -> reservationService.create(
                        USER_ID, LAB_ID, LocalDate.now().plusDays(1), "10:00-13:00", PURPOSE));
    }

    @Test
    void shouldAuditPendingFutureReservation() {
        when(reservationRepository.findForAudit(9L)).thenReturn(Optional.of(Map.of(
                "reserve_date", LocalDate.now().plusDays(1).toString(),
                "status", "待审核")));
        when(reservationRepository.audit(9L, "已通过", "李老师")).thenReturn(1);

        reservationService.audit(9L, "已通过", "李老师");

        verify(reservationRepository).audit(9L, "已通过", "李老师");
    }

    @Test
    void shouldRejectAuditingPastReservation() {
        when(reservationRepository.findForAudit(9L)).thenReturn(Optional.of(Map.of(
                "reserve_date", LocalDate.now().minusDays(1).toString(),
                "status", "待审核")));

        assertThrows(ResponseStatusException.class,
                () -> reservationService.audit(9L, "已通过", "李老师"));
        verify(reservationRepository, never()).audit(9L, "已通过", "李老师");
    }

    @Test
    void shouldCancelOwnFutureReservationByChangingStatus() {
        when(reservationRepository.findForCancellation(9L)).thenReturn(Optional.of(Map.of(
                "user_id", USER_ID,
                "reserve_date", LocalDate.now().plusDays(1).toString(),
                "status", "已通过")));
        when(reservationRepository.cancel(9L)).thenReturn(1);

        reservationService.cancel(9L, USER_ID);

        verify(reservationRepository).cancel(9L);
    }

    @Test
    void shouldRejectCancellingAnotherUsersReservation() {
        when(reservationRepository.findForCancellation(9L)).thenReturn(Optional.of(Map.of(
                "user_id", 99L,
                "reserve_date", LocalDate.now().plusDays(1).toString(),
                "status", "待审核")));

        assertThrows(ResponseStatusException.class, () -> reservationService.cancel(9L, USER_ID));
        verify(reservationRepository, never()).cancel(9L);
    }

    @Test
    void shouldRejectCancellingPastReservation() {
        when(reservationRepository.findForCancellation(9L)).thenReturn(Optional.of(Map.of(
                "user_id", USER_ID,
                "reserve_date", LocalDate.now().minusDays(1).toString(),
                "status", "已通过")));

        assertThrows(ResponseStatusException.class, () -> reservationService.cancel(9L, USER_ID));
        verify(reservationRepository, never()).cancel(9L);
    }

    private void allowOpenQualifiedLab() {
        when(reservationRepository.findLabStatus(LAB_ID)).thenReturn(Optional.of("开放"));
        when(reservationRepository.hasQualifiedAccess(USER_ID, LAB_ID)).thenReturn(true);
    }
}
