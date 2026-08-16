package handler

import "time"

// Shared envelope (matches the Spring Boot pattern)

// BaseEvent is the outer wrapper sent by Spring Boot services.
// The "payload" field is type-specific; we unmarshal it per routing-key.
type BaseEvent struct {
	EventType string    `json:"eventType"`
	Version   string    `json:"version"`
	Timestamp time.Time `json:"timestamp"`
}

// parking.reserved
// Routing key: parking.reserved

type ParkingReservedEvent struct {
	ReservationID    string    `json:"reservationId"`
	UserID           string    `json:"userId"`
	UserEmail        string    `json:"userEmail"`
	ParkingSpaceName string    `json:"parkingSpaceName"`
	StartTime        time.Time `json:"startTime"`
	EndTime          time.Time `json:"endTime"`
	TotalAmount      float64   `json:"totalAmount"`
}


// payment.* (success / failed / refunded)
// Routing key: payment.success | payment.failed | payment.refunded
//
// Note: the Payment Service (PaymentStatusEvent) publishes these fields
// directly at the root level (no nested payload envelope).


type PaymentStatusEvent struct {
	TransactionID  string    `json:"transactionId"`
	ReservationID  string    `json:"reservationId"`
	UserID         string    `json:"userId"`
	Status         string    `json:"status"`
	Amount         float64   `json:"amount"`
	Currency       string    `json:"currency"`
	TransactionRef string    `json:"transactionRef"`
	OccurredAt     time.Time `json:"occurredAt"`
}


// reservation.cancelled
// Routing key: reservation.cancelled


type ReservationCancelledEvent struct {
	ReservationID string `json:"reservationId"`
	UserID        string `json:"userId"`
	UserEmail     string `json:"userEmail"`
	Reason        string `json:"reason"`
}

// vehicle.entered / vehicle.exited
// Routing key: vehicle.entered | vehicle.exited

type VehicleEntryEvent struct {
	VehicleID      string    `json:"vehicleId"`
	ParkingSpaceID string    `json:"parkingSpaceId"`
	Timestamp      time.Time `json:"timestamp"`
}

type VehicleExitEvent struct {
	VehicleID      string    `json:"vehicleId"`
	ParkingSpaceID string    `json:"parkingSpaceId"`
	Timestamp      time.Time `json:"timestamp"`
	DurationMins   int       `json:"durationMins"`
}
