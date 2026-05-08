package com.example.railway.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import com.example.railway.common.BusinessException;

@Entity
@Table(name = "seat_inventories", uniqueConstraints = @UniqueConstraint(
        name = "uk_inventory_train_date_seat",
        columnNames = {"train_id", "travel_date", "seat_type"}))
public class SeatInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(name = "seat_type", nullable = false, length = 32)
    private String seatType;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "remaining_seats", nullable = false)
    private Integer remainingSeats;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Version
    @Column(nullable = false)
    private Long version;

    public SeatInventory() {
    }

    public SeatInventory(Train train, LocalDate travelDate, String seatType, Integer totalSeats, BigDecimal price) {
        this.train = train;
        this.travelDate = travelDate;
        this.seatType = seatType;
        this.totalSeats = totalSeats;
        this.remainingSeats = totalSeats;
        this.price = price;
    }

    public void deductOne() {
        if (remainingSeats == null || remainingSeats <= 0) {
            throw new BusinessException("当前车次余票不足");
        }
        remainingSeats = remainingSeats - 1;
    }

    public void releaseOne() {
        if (remainingSeats == null) {
            remainingSeats = 0;
        }
        if (totalSeats != null && remainingSeats < totalSeats) {
            remainingSeats = remainingSeats + 1;
        }
    }

    public Long getId() {
        return id;
    }

    public Train getTrain() {
        return train;
    }

    public void setTrain(Train train) {
        this.train = train;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public Integer getRemainingSeats() {
        return remainingSeats;
    }

    public void setRemainingSeats(Integer remainingSeats) {
        this.remainingSeats = remainingSeats;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getVersion() {
        return version;
    }
}
