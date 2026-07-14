package com.tails.notification;

import com.tails.travel.Travel;
import com.tails.travel.TravelRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 매일 09시, 내일 시작하는 여행 일정을 찾아 소유자에게 리마인더 알림 생성
@Component
@RequiredArgsConstructor
public class TravelReminderScheduler {

    private final TravelRepository travelRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendTravelStartReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Travel> travelsStartingTomorrow = travelRepository.findByStartDate(tomorrow);

        for (Travel travel : travelsStartingTomorrow) {
            notificationService.create(
                    travel.getMember().getId(),
                    NotificationType.TRAVEL,
                    "내일부터 '" + travel.getTitle() + "' 여행이 시작됩니다!",
                    travel.getTravelId());
        }
    }
}
