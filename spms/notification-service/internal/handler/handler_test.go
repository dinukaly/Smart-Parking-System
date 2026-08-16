package handler

import (
	"encoding/json"
	"testing"
	"time"
)

func TestDispatch_ParkingReserved(t *testing.T) {
	stats := &Stats{}
	evt := ParkingReservedEvent{
		ReservationID:    "res-test-1",
		UserID:           "user-uuid-1",
		UserEmail:        "driver@example.com",
		ParkingSpaceName: "Zone A - Spot 01",
		StartTime:        time.Now(),
		EndTime:          time.Now().Add(2 * time.Hour),
		TotalAmount:      15.00,
	}
	body, err := json.Marshal(evt)
	if err != nil {
		t.Fatalf("Failed to marshal event: %v", err)
	}

	Dispatch("parking.reserved", body, stats)

	if stats.ParkingReserved.Load() != 1 {
		t.Errorf("Expected ParkingReserved = 1, got %d", stats.ParkingReserved.Load())
	}
	if stats.TotalProcessed.Load() != 1 {
		t.Errorf("Expected TotalProcessed = 1, got %d", stats.TotalProcessed.Load())
	}
}

func TestDispatch_PaymentSuccess(t *testing.T) {
	stats := &Stats{}
	evt := PaymentStatusEvent{
		TransactionID:  "tx-uuid-1",
		ReservationID:  "res-test-1",
		UserID:         "user-uuid-1",
		Status:         "SUCCESS",
		Amount:         15.00,
		Currency:       "USD",
		TransactionRef: "TXN-20260817-001",
		OccurredAt:     time.Now(),
	}
	body, _ := json.Marshal(evt)

	Dispatch("payment.success", body, stats)

	if stats.PaymentSuccess.Load() != 1 {
		t.Errorf("Expected PaymentSuccess = 1, got %d", stats.PaymentSuccess.Load())
	}
}

func TestDispatch_PaymentFailed(t *testing.T) {
	stats := &Stats{}
	evt := PaymentStatusEvent{
		TransactionID:  "tx-uuid-2",
		ReservationID:  "res-test-2",
		UserID:         "user-uuid-1",
		Status:         "FAILED",
		Amount:         20.00,
		Currency:       "USD",
		TransactionRef: "TXN-20260817-002",
		OccurredAt:     time.Now(),
	}
	body, _ := json.Marshal(evt)

	Dispatch("payment.failed", body, stats)

	if stats.PaymentFailed.Load() != 1 {
		t.Errorf("Expected PaymentFailed = 1, got %d", stats.PaymentFailed.Load())
	}
}

func TestDispatch_PaymentRefunded(t *testing.T) {
	stats := &Stats{}
	evt := PaymentStatusEvent{
		TransactionID:  "tx-uuid-3",
		ReservationID:  "res-test-3",
		UserID:         "user-uuid-1",
		Status:         "REFUNDED",
		Amount:         25.00,
		Currency:       "USD",
		TransactionRef: "TXN-20260817-003",
		OccurredAt:     time.Now(),
	}
	body, _ := json.Marshal(evt)

	Dispatch("payment.refunded", body, stats)

	if stats.PaymentRefunded.Load() != 1 {
		t.Errorf("Expected PaymentRefunded = 1, got %d", stats.PaymentRefunded.Load())
	}
}

func TestDispatch_ReservationCancelled(t *testing.T) {
	stats := &Stats{}
	evt := ReservationCancelledEvent{
		ReservationID: "res-test-4",
		UserID:        "user-uuid-1",
		UserEmail:     "driver@example.com",
		Reason:        "User requested cancellation",
	}
	body, _ := json.Marshal(evt)

	Dispatch("reservation.cancelled", body, stats)

	if stats.ReservationCancelled.Load() != 1 {
		t.Errorf("Expected ReservationCancelled = 1, got %d", stats.ReservationCancelled.Load())
	}
}

func TestDispatch_VehicleEntryAndExit(t *testing.T) {
	stats := &Stats{}
	entryEvt := VehicleEntryEvent{
		VehicleID:      "veh-1",
		ParkingSpaceID: "space-1",
		Timestamp:      time.Now(),
	}
	entryBody, _ := json.Marshal(entryEvt)
	Dispatch("vehicle.entered", entryBody, stats)

	exitEvt := VehicleExitEvent{
		VehicleID:      "veh-1",
		ParkingSpaceID: "space-1",
		Timestamp:      time.Now().Add(1 * time.Hour),
		DurationMins:   60,
	}
	exitBody, _ := json.Marshal(exitEvt)
	Dispatch("vehicle.exited", exitBody, stats)

	if stats.VehicleEntered.Load() != 1 {
		t.Errorf("Expected VehicleEntered = 1, got %d", stats.VehicleEntered.Load())
	}
	if stats.VehicleExited.Load() != 1 {
		t.Errorf("Expected VehicleExited = 1, got %d", stats.VehicleExited.Load())
	}
	if stats.TotalProcessed.Load() != 2 {
		t.Errorf("Expected TotalProcessed = 2, got %d", stats.TotalProcessed.Load())
	}
}

func TestDispatch_UnknownRoutingKey(t *testing.T) {
	stats := &Stats{}
	Dispatch("unsupported.event.type", []byte(`{}`), stats)

	if stats.UnknownEvents.Load() != 1 {
		t.Errorf("Expected UnknownEvents = 1, got %d", stats.UnknownEvents.Load())
	}
}
