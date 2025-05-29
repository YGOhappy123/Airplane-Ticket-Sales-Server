package com.ygohappy123.server.models;

import com.ygohappy123.server.enums.SeatStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PlaneSeat {
    private int seatId;
    private String seatNumber;
    private SeatStatus status;
    private String holdExpiresAt;
    private String heldBy;

    public PlaneSeat() {
    }

    public PlaneSeat(int seatId, String seatNumber, SeatStatus status, String holdExpiresAt, String heldBy) {
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.status = status;
        this.holdExpiresAt = holdExpiresAt;
        this.heldBy = heldBy;
    }

    public PlaneSeat(int seatId, String seatNumber, String status, String holdExpiresAt, String heldBy) {
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.status = SeatStatus.valueOf(status.trim().toUpperCase());
        this.holdExpiresAt = holdExpiresAt;
        this.heldBy = heldBy;
    }

    public int getSeatId() {
        return this.seatId;
    }

    public String getSeatNumber() {
        return this.seatNumber;
    }

    public SeatStatus getStatus() {
        return this.status;
    }

    public String getMappedStatus() {
        return switch (this.status) {
            case AVAILABLE -> "Còn trống";
            case HELD -> "Đang được chọn";
            case BOOKED -> "Đã được mua";
        };
    }

    public String getHeldBy() {
        if (this.heldBy == null) return "Không có";

        return this.heldBy;
    }

    public String getHoldExpiresAt() {
        if (this.holdExpiresAt == null) return "Không có";

        return this.holdExpiresAt;
    }

    public String getFormattedHoldExpiresAt() {
        if (this.holdExpiresAt == null) return "Không có";

        DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(this.holdExpiresAt.substring(0, 19), inputFormat);

        DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss");
        return dateTime.format(outputFormat);
    }

    @Override
    public String toString() {
        return "PlaneSeat{" +
                "seatId=" + seatId +
                ", seatNumber='" + seatNumber + '\'' +
                ", status=" + status +
                ", heldBy='" + heldBy + '\'' +
                ", holdExpiresAt='" + holdExpiresAt + '\'' +
                '}';
    }
}
